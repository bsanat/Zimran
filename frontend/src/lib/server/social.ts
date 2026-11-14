import { createHash, randomBytes, randomUUID } from "node:crypto";
import { mkdir, readFile, rename, writeFile } from "node:fs/promises";
import { join, resolve } from "node:path";
import { CATALOG_EVENTS, catalogMatchesDate, catalogRegions } from "../catalog.ts";
import { addDays, getCityToday, INTERESTS, isDate, TIME_OPTIONS } from "../planner.ts";
import { ServiceError } from "./serpapi.ts";
import type { EventData } from "../../types/event.ts";
import type { Interest, Recommendation } from "../../types/planner.ts";
import type { AlertPreferences, BuddyPost, BuddyRequest, DemoNotification, DemoSubscription, PairPreferences, PairSession, PairVote } from "../../types/social.ts";

type Scope = { id: string; tokenHash: string; expiresAt: number; premium: boolean; pairTrial: boolean; assistantAt: number[]; assistantChats?: { id: string; expiresAt: number; turns: number }[]; preferences: AlertPreferences | null; alertsEnabledAt: number; seenIds: string[] };
type StoredRequest = Omit<BuddyRequest, "isOwner"> & { ownerId: string };
type StoredPost = Omit<BuddyPost, "isOwner" | "requests"> & { ownerId: string; requests: StoredRequest[] };
type StoredPair = Omit<PairSession, "role"> & { ownerA: string; ownerB: string | null; inviteHash: string };
type StoredNotification = DemoNotification & { ownerId: string; deliverAt: number };
type Store = { version: 1; scopes: Scope[]; posts: StoredPost[]; pairs: StoredPair[]; notifications: StoredNotification[]; safety: { reporter: string; target: string; postId: string; kind: "BLOCK" | "REPORT"; reason: string; createdAt: string }[] };
const DAY = 86_400_000;
const hash = (value: string) => createHash("sha256").update(value).digest("hex");
const secret = () => randomBytes(32).toString("hex");
const iso = (now: number) => new Date(now).toISOString();
const globals = globalThis as typeof globalThis & { outlyDemoQueues?: Map<string, Promise<unknown>>; outlyDemoLimits?: Map<string, number[]> };
const queues = globals.outlyDemoQueues ??= new Map<string, Promise<unknown>>();
const limits = globals.outlyDemoLimits ??= new Map<string, number[]>();
const emptyStore = (): Store => ({ version: 1, scopes: [], posts: [], pairs: [], notifications: [], safety: [] });
function fail(status: number, code: string, message: string): never { throw new ServiceError(status, code, message); }
function invalid(message = "Please check your preferences and try again."): never { return fail(400, "VALIDATION_ERROR", message); }
const record = (value: unknown): value is Record<string, unknown> => typeof value === "object" && value !== null && !Array.isArray(value);

// shortcut: Atomic files and a process-wide queue support one local demo server; use a transactional database before multi-instance deployment.
async function transaction<T>(mutate: boolean, action: (store: Store, now: number) => T | Promise<T>): Promise<T> {
  const directory = resolve(/* turbopackIgnore: true */ process.env.OUTLY_DEMO_DIR || ".outly-demo");
  const operation = (queues.get(directory) ?? Promise.resolve()).catch(() => {}).then(async () => {
    let store: Store;
    try { store = JSON.parse(await readFile(join(directory, "state.json"), "utf8")) as Store; }
    catch (error) { if ((error as NodeJS.ErrnoException).code === "ENOENT") store = emptyStore(); else throw error; }
    if (store.version !== 1 || !Array.isArray(store.scopes) || !Array.isArray(store.posts) || !Array.isArray(store.pairs) || !Array.isArray(store.notifications) || !Array.isArray(store.safety)) throw new Error("Invalid demo store");
    const value = await action(store, Date.now());
    if (mutate) {
      await mkdir(directory, { recursive: true, mode: 0o700 });
      const temporary = join(directory, `state-${randomUUID()}.tmp`);
      await writeFile(temporary, JSON.stringify(store), { mode: 0o600 });
      await rename(temporary, join(directory, "state.json"));
    }
    return value;
  });
  queues.set(directory, operation);
  return operation;
}

function rate(key: string, maximum: number, window = 60_000, now = Date.now()) {
  const recent = (limits.get(key) || []).filter(time => now - time < window);
  if (recent.length >= maximum) fail(429, "RATE_LIMITED", "Too many requests. Please wait a moment before trying again.");
  recent.push(now);
  if (limits.size > 2000) limits.delete(limits.keys().next().value!);
  limits.set(key, recent);
}

function scopeFor(store: Store, request: Request, now: number): Scope {
  const token = request.headers.get("authorization")?.match(/^Bearer ([a-f0-9]{64})$/)?.[1];
  const scope = token && store.scopes.find(item => item.tokenHash === hash(token) && item.expiresAt > now);
  if (!scope) fail(401, "SESSION_REQUIRED", "Your demo session has expired. Start a new demo session.");
  return scope;
}
function premium(scope: Scope) { if (!scope.premium) fail(403, "PREMIUM_REQUIRED", "Try Premium Demo to use this feature. No payment is required."); }
function subscription(scope: Scope): DemoSubscription { return { status: scope.premium ? "DEMO_PREMIUM" : "FREE", price: 9.99, currency: "USD" }; }
function text(value: unknown, label: string, maximum = 500, minimum = 1): string {
  if (typeof value !== "string" || value.trim().length < minimum || value.trim().length > maximum) invalid(`${label} must contain ${minimum}–${maximum} characters.`);
  return (value as string).trim();
}
function publicText(value: unknown, label: string, maximum = 500, minimum = 1) {
  const result = text(value, label, maximum, minimum);
  if (/[\w.+-]+@[\w.-]+\.[a-z]{2,}|https?:\/\/|www\.|@[\w.]{2,}|(?:\+?\d[\s().-]*){8,}/i.test(result)) invalid("Keep phone numbers, email addresses and social handles out of public messages.");
  return result;
}
function city(value: unknown) {
  if (typeof value !== "string" || !catalogRegions.some(region => region.cities.includes(value))) invalid("Choose a city from the event catalog.");
  return value as string;
}
function interests(value: unknown): Interest[] {
  if (!Array.isArray(value) || !value.length || value.length > INTERESTS.length || value.some(id => !INTERESTS.some(item => item.id === id))) invalid("Choose at least one valid interest.");
  return [...new Set(value as Interest[])];
}
function budget(value: unknown): number | undefined {
  if (value === undefined) return undefined;
  if (typeof value !== "number" || !Number.isFinite(value) || value < 0 || value > 100_000) invalid("Choose a valid maximum budget.");
  return value as number;
}
function dates(start: unknown, end: unknown, now: number): { startDate: string; endDate: string } {
  if (typeof start !== "string" || typeof end !== "string" || !isDate(start) || !isDate(end) || start < getCityToday(new Date(now)) || end < start || end > addDays(start, 366)) invalid("Choose a valid date range within one year, starting today or later.");
  return { startDate: start as string, endDate: end as string };
}
function preferences(value: unknown, now: number): PairPreferences {
  if (!record(value)) invalid();
  const input = value as Record<string, unknown>;
  if (!TIME_OPTIONS.some(item => item.value === input.timePreference)) invalid("Choose a valid time of day.");
  const amount = budget(input.budgetMax);
  const notes = input.notes === undefined ? "" : publicText(input.notes, "Notes", 500, 0);
  return { displayName: publicText(input.displayName, "Display name", 50), interests: interests(input.interests), ...dates(input.startDate, input.endDate, now), ...(amount === undefined ? {} : { budgetMax: amount }), timePreference: input.timePreference as PairPreferences["timePreference"], ...(notes ? { notes } : {}) };
}
function validEvent(value: unknown): EventData {
  const found = typeof value === "string" && CATALOG_EVENTS.find(item => item.id === value);
  if (!found || found.status !== "scheduled" || (found.endDate || found.startDate || "9999") < getCityToday()) fail(404, "NOT_FOUND", "This event could not be found.");
  return found;
}
function blocked(store: Store, a: string, b: string) { return store.safety.some(item => item.kind === "BLOCK" && (item.reporter === a && item.target === b || item.reporter === b && item.target === a)); }
function postStatus(post: StoredPost, now: number): BuddyPost["status"] { return Date.parse(post.expiresAt) <= now ? "EXPIRED" : post.status; }
function postView(store: Store, post: StoredPost, scope: Scope, now: number): BuddyPost {
  return { id: post.id, eventId: post.eventId, displayName: post.displayName, bio: post.bio, language: post.language, status: postStatus(post, now), isOwner: post.ownerId === scope.id, requests: post.requests.filter(item => (post.ownerId === scope.id || item.ownerId === scope.id) && !blocked(store, post.ownerId, item.ownerId)).map(({ ownerId, ...item }) => ({ ...item, isOwner: ownerId === scope.id })), createdAt: post.createdAt, expiresAt: post.expiresAt, demo: true };
}
function findPost(store: Store, id: string): StoredPost { return store.posts.find(item => item.id === id) ?? fail(404, "NOT_FOUND", "This Buddy post could not be found."); }
function pairView(pair: StoredPair, scope: Scope, now: number): PairSession {
  if (scope.id !== pair.ownerA && scope.id !== pair.ownerB) fail(403, "FORBIDDEN", "Only the invited participants can open this plan.");
  const { ownerA, ownerB: _ownerB, inviteHash: _inviteHash, ...session } = pair;
  return { ...session, status: Date.parse(pair.expiresAt) <= now ? "EXPIRED" : pair.status, role: scope.id === ownerA ? "A" : "B" };
}
function findPair(store: Store, id: string, scope: Scope, now: number): StoredPair {
  const pair = store.pairs.find(item => item.id === id) ?? fail(404, "NOT_FOUND", "This shared plan could not be found.");
  pairView(pair, scope, now);
  return pair;
}
function pairActive(pair: StoredPair, now: number) { if (["CLOSED", "EXPIRED"].includes(pair.status) || Date.parse(pair.expiresAt) <= now) fail(410, "PLAN_EXPIRED", "This invitation has expired or the plan has been closed."); }
function matchesBudget(event: EventData, maximum?: number) { return maximum === undefined || event.priceKnown && event.currency === "USD" && event.priceMin !== null && event.priceMin <= maximum; }
function matchesTime(event: EventData, preference: string) {
  if (preference === "any") return true;
  if (!event.startAt) return false;
  const date = new Date(event.startAt);
  if (!Number.isFinite(date.getTime())) return false;
  const hour = Number(new Intl.DateTimeFormat("en-US", { timeZone: event.timezone, hour: "numeric", hourCycle: "h23" }).format(date));
  return preference === "morning" ? hour < 12 : preference === "afternoon" ? hour >= 12 && hour < 17 : hour >= 17;
}
function inDateRange(event: EventData, start: string, end: string) {
  for (let date = start; date <= end; date = addDays(date, 1)) if (catalogMatchesDate(event, date)) return true;
  return false;
}
export function matchPairEvents(cityName: string, a: PairPreferences, b: PairPreferences, events: EventData[] = CATALOG_EVENTS): Recommendation[] {
  const start = a.startDate > b.startDate ? a.startDate : b.startDate;
  const end = a.endDate < b.endDate ? a.endDate : b.endDate;
  if (end < start || a.timePreference !== "any" && b.timePreference !== "any" && a.timePreference !== b.timePreference) return [];
  const amounts = [a.budgetMax, b.budgetMax].filter((value): value is number => value !== undefined);
  const maximum = amounts.length ? Math.min(...amounts) : undefined;
  const common = a.interests.filter(id => b.interests.includes(id));
  const union = [...new Set([...a.interests, ...b.interests])];
  return events.filter(event => event.status === "scheduled" && event.city === cityName && union.includes(event.category as Interest) && matchesBudget(event, maximum) && matchesTime(event, a.timePreference) && matchesTime(event, b.timePreference) && inDateRange(event, start, end)).map(event => ({ event, matchScore: common.includes(event.category as Interest) ? 95 : 75, reason: common.includes(event.category as Interest) ? "A shared interest, within both date ranges and budgets." : "Within both date ranges and budgets. This interest is a compromise for one of you." })).sort((a, b) => b.matchScore - a.matchScore || a.event.title.localeCompare(b.event.title)).slice(0, 5);
}
export function matchesAlert(event: EventData, input: AlertPreferences, now = Date.now()): boolean {
  return !input.paused && event.status === "scheduled" && event.city === input.city && input.categorySlugs.includes(event.category as Interest) && matchesBudget(event, input.budgetMax) && inDateRange(event, input.startDate || getCityToday(new Date(now)), input.endDate || addDays(getCityToday(new Date(now)), 366));
}
function notificationFeed(store: Store, scope: Scope, now: number) {
  const items = store.notifications.filter(item => item.ownerId === scope.id && item.deliverAt <= now).sort((a, b) => b.createdAt.localeCompare(a.createdAt)).map(({ ownerId: _ownerId, deliverAt: _deliverAt, ...item }) => item);
  return { items, unreadCount: items.filter(item => !item.readAt).length };
}

function chargeAssistantSession(scope: Scope, now: number) {
  const window = scope.premium ? DAY : 7 * DAY;
  scope.assistantAt = scope.assistantAt.filter(time => now - time < window);
  if (scope.assistantAt.length >= (scope.premium ? 50 : 1)) fail(429, "ASSISTANT_LIMIT", scope.premium ? "Your demo fair-use limit is reached. Try again tomorrow." : "Your free weekly assistant session is used. Try Premium Demo for more plans.");
  scope.assistantAt.push(now);
}

export async function consumeAssistantSession(request: Request): Promise<void> {
  await transaction(true, (store, now) => {
    const scope = scopeFor(store, request, now);
    rate(`assistant:${scope.id}`, 8, 60_000, now);
    chargeAssistantSession(scope, now);
  });
}

export async function consumeAssistantChat(request: Request, conversationId?: string): Promise<string> {
  return transaction(true, (store, now) => {
    const scope = scopeFor(store, request, now);
    rate(`assistant:${scope.id}`, 8, 60_000, now);
    if (conversationId !== undefined) {
      if (typeof conversationId !== "string" || !/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/.test(conversationId)) invalid("Choose a valid assistant conversation.");
      const chat = scope.assistantChats?.find(item => item.id === conversationId) ?? fail(404, "ASSISTANT_CONVERSATION_NOT_FOUND", "This assistant conversation is unavailable. Start a new conversation.");
      if (chat.expiresAt <= now) fail(410, "ASSISTANT_CONVERSATION_EXPIRED", "This assistant conversation has expired. Start a new conversation.");
      if (chat.turns >= 30) fail(429, "ASSISTANT_TURN_LIMIT", "This conversation has reached its limit of 30 results. Start a new conversation.");
      chat.turns++;
      return chat.id;
    }
    chargeAssistantSession(scope, now);
    const chat = { id: randomUUID(), expiresAt: now + DAY, turns: 1 };
    scope.assistantChats = [...(scope.assistantChats || []).filter(item => item.expiresAt > now).slice(-4), chat];
    return chat.id;
  });
}

export async function demoAction(request: Request, path: string[]): Promise<unknown> {
  const route = path.join("/");
  const method = request.method;
  const query = new URL(request.url).searchParams;
  let input: Record<string, unknown> = {};
  if (!["GET", "HEAD"].includes(method)) {
    const raw = await request.text();
    if (raw.length > 10_000) fail(413, "PAYLOAD_TOO_LARGE", "Keep your request under 10,000 characters.");
    if (raw) { let parsed: unknown; try { parsed = JSON.parse(raw); } catch { invalid("Send a valid JSON request."); } if (!record(parsed)) invalid(); input = parsed as Record<string, unknown>; }
  }
  const mutation = method !== "GET";
  return transaction(mutation, (store, now) => {
    if (method === "POST" && route === "session") {
      rate(`session:${hash(request.headers.get("x-forwarded-for")?.split(",")[0]?.trim() || "local")}`, 60, 10 * 60_000, now);
      const token = secret();
      const scope: Scope = { id: randomUUID(), tokenHash: hash(token), expiresAt: now + 7 * DAY, premium: false, pairTrial: false, assistantAt: [], preferences: null, alertsEnabledAt: 0, seenIds: [] };
      store.scopes.push(scope);
      return { token, subscription: subscription(scope) };
    }
    const scope = scopeFor(store, request, now);
    if (mutation) rate(`mutation:${scope.id}`, 40, 60_000, now);
    if (route === "subscription") {
      if (method === "POST") { if (!["activate", "deactivate"].includes(input.action as string)) invalid(); scope.premium = input.action === "activate"; if (!scope.premium) store.notifications = store.notifications.filter(item => item.ownerId !== scope.id || item.deliverAt <= now); }
      else if (method !== "GET") fail(405, "METHOD_NOT_ALLOWED", "This method is not supported.");
      return subscription(scope);
    }
    if (route === "buddy/posts" && method === "GET") {
      const eventId = query.get("eventId");
      if (eventId) validEvent(eventId);
      return { posts: store.posts.filter(post => (!eventId || post.eventId === eventId) && !blocked(store, scope.id, post.ownerId) && (post.ownerId === scope.id || post.requests.some(item => item.ownerId === scope.id) || ["OPEN", "MATCHED"].includes(postStatus(post, now)))).slice(-100).reverse().map(post => postView(store, post, scope, now)) };
    }
    if (route === "buddy/posts" && method === "POST") {
      premium(scope); validEvent(input.eventId);
      if (input.ageConfirmed !== true) invalid("Confirm that you are at least 18 years old.");
      if (store.posts.filter(post => post.ownerId === scope.id && postStatus(post, now) === "OPEN").length >= 8) fail(429, "POST_LIMIT", "Close an existing Buddy post before creating another.");
      const post: StoredPost = { id: randomUUID(), ownerId: scope.id, eventId: input.eventId as string, displayName: publicText(input.displayName, "Display name", 50), bio: publicText(input.bio, "Introduction"), language: publicText(input.language, "Language", 40), requests: [], status: "OPEN", createdAt: iso(now), expiresAt: iso(now + 2 * DAY), demo: true };
      store.posts.push(post);
      return { post: postView(store, post, scope, now) };
    }
    if (/^buddy\/posts\/[^/]+\/requests$/.test(route) && method === "POST") {
      premium(scope);
      const post = findPost(store, path[2]);
      if (post.ownerId === scope.id || blocked(store, scope.id, post.ownerId)) fail(403, "FORBIDDEN", "You cannot send a request to this Buddy post.");
      if (postStatus(post, now) !== "OPEN") fail(409, "POST_CLOSED", "This Buddy post is no longer accepting requests.");
      if (input.ageConfirmed !== true) invalid("Confirm that you are at least 18 years old.");
      if (post.requests.some(item => item.ownerId === scope.id && ["PENDING", "ACCEPTED"].includes(item.status))) fail(409, "DUPLICATE_REQUEST", "You have already sent a request to this post.");
      post.requests.push({ id: randomUUID(), ownerId: scope.id, displayName: publicText(input.displayName, "Display name", 50), message: publicText(input.message, "Message"), status: "PENDING", createdAt: iso(now) });
      return { post: postView(store, post, scope, now) };
    }
    if (/^buddy\/requests\/[^/]+\/(decision|cancel)$/.test(route) && method === "POST") {
      const post = store.posts.find(item => item.requests.some(reply => reply.id === path[2])) ?? fail(404, "NOT_FOUND", "This request could not be found.");
      const reply = post.requests.find(item => item.id === path[2])!;
      if (path[3] === "cancel") {
        if (reply.ownerId !== scope.id) fail(403, "FORBIDDEN", "Only the sender can cancel this request.");
        if (!["PENDING", "ACCEPTED"].includes(reply.status)) fail(409, "REQUEST_RESOLVED", "This request has already been resolved.");
        const wasMatched = reply.status === "ACCEPTED";
        reply.status = "CANCELLED";
        if (wasMatched && postStatus(post, now) === "MATCHED") post.status = "OPEN";
      } else {
        if (post.ownerId !== scope.id || blocked(store, scope.id, reply.ownerId)) fail(403, "FORBIDDEN", "Only the post author can decide on this request.");
        if (!["ACCEPTED", "DECLINED"].includes(input.decision as string)) invalid();
        if (postStatus(post, now) !== "OPEN" || reply.status !== "PENDING") fail(409, "REQUEST_RESOLVED", "This request has already been resolved.");
        reply.status = input.decision as "ACCEPTED" | "DECLINED";
        if (reply.status === "ACCEPTED") { post.status = "MATCHED"; for (const other of post.requests) if (other.status === "PENDING") other.status = "DECLINED"; }
      }
      return { post: postView(store, post, scope, now) };
    }
    if (/^buddy\/posts\/[^/]+\/close$/.test(route) && method === "POST") {
      const post = findPost(store, path[2]);
      if (post.ownerId !== scope.id) fail(403, "FORBIDDEN", "Only the author can close this post.");
      post.status = "CLOSED";
      for (const reply of post.requests) if (["PENDING", "ACCEPTED"].includes(reply.status)) reply.status = "CANCELLED";
      return { post: postView(store, post, scope, now) };
    }
    if (route === "safety" && method === "POST") {
      const post = findPost(store, text(input.postId, "Post ID", 100));
      let target = post.ownerId;
      if (input.requestId !== undefined) {
        if (post.ownerId !== scope.id) fail(403, "FORBIDDEN", "Only the post author can moderate requests.");
        target = post.requests.find(item => item.id === input.requestId)?.ownerId ?? fail(404, "NOT_FOUND", "This request could not be found.");
      }
      if (target === scope.id) invalid("You can close your own post instead.");
      if (!["REPORT", "BLOCK"].includes(input.kind as string)) invalid();
      const reason = publicText(input.reason, "Reason", 500);
      if (!store.safety.some(item => item.reporter === scope.id && item.target === target && item.kind === input.kind)) store.safety.push({ reporter: scope.id, target, postId: post.id, kind: input.kind as "REPORT" | "BLOCK", reason, createdAt: iso(now) });
      if (input.kind === "BLOCK") for (const item of store.posts) for (const reply of item.requests) if (blocked(store, item.ownerId, reply.ownerId) && ["ACCEPTED", "PENDING"].includes(reply.status)) { const accepted = reply.status === "ACCEPTED"; reply.status = "CANCELLED"; if (accepted && item.status === "MATCHED") item.status = "OPEN"; }
      return { saved: true };
    }
    if (route === "pair/sessions" && method === "POST") {
      if (!scope.premium && scope.pairTrial) fail(403, "PREMIUM_REQUIRED", "Your trial shared plan is used. Try Premium Demo to plan again.");
      const participantA = preferences(input.participantA, now);
      const token = secret();
      const pair: StoredPair = { id: randomUUID(), city: city(input.city), ownerA: scope.id, ownerB: null, inviteHash: hash(token), participantA, participantB: null, status: "WAITING", recommendations: [], votes: { A: {}, B: {} }, sharedEventId: null, expiresAt: iso(now + 2 * DAY), demo: true };
      scope.pairTrial = true;
      store.pairs.push(pair);
      return { session: pairView(pair, scope, now), inviteToken: token };
    }
    if (route === "pair/join" && method === "POST") {
      const pair = store.pairs.find(item => item.id === input.sessionId) ?? fail(404, "NOT_FOUND", "This invitation could not be found.");
      pairActive(pair, now);
      if (typeof input.inviteToken !== "string" || !/^[a-f0-9]{64}$/.test(input.inviteToken) || hash(input.inviteToken) !== pair.inviteHash) fail(403, "INVALID_INVITATION", "This invitation is invalid or has already been used.");
      if (pair.ownerA === scope.id) fail(409, "SEPARATE_SESSION_REQUIRED", "Open the invitation in another browser or an independent guest session.");
      if (pair.ownerB) fail(409, "INVITATION_USED", "This invitation has already been used.");
      pair.participantB = preferences(input.participantB, now); pair.ownerB = scope.id; pair.inviteHash = "";
      pair.recommendations = matchPairEvents(pair.city, pair.participantA, pair.participantB);
      pair.status = "READY";
      return { session: pairView(pair, scope, now) };
    }
    if (/^pair\/[^/]+$/.test(route) && method === "GET") return { session: pairView(findPair(store, path[1], scope, now), scope, now) };
    if (/^pair\/[^/]+\/votes$/.test(route) && method === "POST") {
      const pair = findPair(store, path[1], scope, now); pairActive(pair, now);
      if (!pair.participantB) fail(409, "WAITING_FOR_PARTNER", "Wait for your partner to join before voting.");
      if (!pair.recommendations.some(item => item.event.id === input.eventId) || !["LIKE", "MAYBE", "PASS"].includes(input.vote as string)) invalid("Choose one of the suggested events and a valid vote.");
      pair.votes[scope.id === pair.ownerA ? "A" : "B"][input.eventId as string] = input.vote as PairVote;
      pair.sharedEventId = pair.recommendations.find(item => pair.votes.A[item.event.id] === "LIKE" && pair.votes.B[item.event.id] === "LIKE")?.event.id || null;
      pair.status = pair.sharedEventId ? "MATCHED" : "READY";
      return { session: pairView(pair, scope, now) };
    }
    if (/^pair\/[^/]+\/close$/.test(route) && method === "POST") {
      const pair = findPair(store, path[1], scope, now);
      if (pair.ownerA !== scope.id) fail(403, "FORBIDDEN", "Only the creator can close this plan.");
      pair.status = "CLOSED"; pair.inviteHash = "";
      return { session: pairView(pair, scope, now) };
    }
    if (route === "alerts/preferences") {
      premium(scope);
      if (method === "PUT") {
        if (!["instant", "daily", "weekly"].includes(input.frequency as string) || !Array.isArray(input.channels) || input.channels.length !== 1 || input.channels[0] !== "in_app" || typeof input.paused !== "boolean") invalid();
        const amount = budget(input.budgetMax);
        const range = input.startDate === undefined && input.endDate === undefined ? {} : dates(input.startDate, input.endDate, now);
        scope.preferences = { city: city(input.city), categorySlugs: interests(input.categorySlugs), ...(amount === undefined ? {} : { budgetMax: amount }), ...range, frequency: input.frequency as AlertPreferences["frequency"], channels: ["in_app"], paused: input.paused as boolean };
        if (!scope.alertsEnabledAt) scope.alertsEnabledAt = now;
        if (scope.preferences.paused) store.notifications = store.notifications.filter(item => item.ownerId !== scope.id || item.deliverAt <= now);
      } else if (method !== "GET") fail(405, "METHOD_NOT_ALLOWED", "This method is not supported.");
      return { preferences: scope.preferences };
    }
    if (route === "notifications" && method === "GET") { premium(scope); return notificationFeed(store, scope, now); }
    if (/^notifications\/[^/]+\/read$/.test(route) && method === "PATCH") {
      premium(scope);
      const item = store.notifications.find(item => item.id === path[1] && item.ownerId === scope.id && item.deliverAt <= now) ?? fail(404, "NOT_FOUND", "This notification could not be found.");
      item.readAt ||= iso(now);
      return notificationFeed(store, scope, now);
    }
    if (route === "alerts/check" && method === "POST") {
      premium(scope); rate(`alerts:${scope.id}`, 10, 60_000, now);
      const input = scope.preferences;
      if (!input) invalid("Save your notification preferences first.");
      let created = 0;
      if (!input.paused) {
        const unseen = CATALOG_EVENTS.filter(event => event.city === input.city && !scope.seenIds.includes(event.id));
        const selected = unseen.find(event => matchesAlert(event, input, now));
        const other = unseen.find(event => event.id !== selected?.id && !input.categorySlugs.includes(event.category as Interest));
        for (const event of [selected, other]) if (event) {
          scope.seenIds.push(event.id);
          if (matchesAlert(event, input, now) && !store.notifications.some(item => item.ownerId === scope.id && item.eventId === event.id)) {
            const deliverAt = now + (input.frequency === "instant" ? 0 : input.frequency === "daily" ? DAY : 7 * DAY);
            store.notifications.push({ id: randomUUID(), ownerId: scope.id, eventId: event.id, title: event.title, categorySlug: event.category as Interest, readAt: null, createdAt: iso(now), deliverAt, demo: true });
            if (deliverAt <= now) created++;
          }
        }
      }
      return { created, pending: store.notifications.filter(item => item.ownerId === scope.id && item.deliverAt > now).length, ...notificationFeed(store, scope, now) };
    }
    return fail(404, "NOT_FOUND", "This demo endpoint could not be found.");
  });
}
