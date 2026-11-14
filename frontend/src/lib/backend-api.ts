import { ApiError, apiRequest, isEvent } from "./api.ts";
import { CATALOG_EVENTS } from "./catalog.ts";
import { INTERESTS } from "./planner.ts";
import type { AssistantChatResponse, AssistantPendingField } from "../types/assistant.ts";
import type { EventData } from "../types/event.ts";
import type { Interest, PlannerRequest, PlannerResponse } from "../types/planner.ts";
import type { AlertPreferences, BuddyPost, BuddyRequest, DemoNotification, DemoSubscription, NotificationFeed, PairPreferences, PairSession, PairSuggestion, PairVote } from "../types/social.ts";

// Guest identity issued by the Spring backend (POST /api/guest/session), sent as X-Guest-Token.
// Kept per tab, like the previous demo session; P2 auth will replace it.
const storageKey = "outly-guest-session-v1";
const tokenPattern = /^[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+$/;
// Renew a little before the backend expiry so a request never races the deadline.
const expirySkewMs = 60_000;

interface GuestSession { token: string; expiresAt: number }

let session: GuestSession | null = null;
let sessionPromise: Promise<GuestSession> | null = null;

function usable(value: GuestSession | null, now = Date.now()): value is GuestSession {
  return !!value && value.expiresAt - expirySkewMs > now;
}

function readStored(): GuestSession | null {
  try {
    const raw = sessionStorage.getItem(storageKey);
    if (!raw) return null;
    const value = JSON.parse(raw) as Partial<GuestSession>;
    return typeof value.token === "string" && tokenPattern.test(value.token) && typeof value.expiresAt === "number"
      ? { token: value.token, expiresAt: value.expiresAt } : null;
  } catch { return null; }
}

function store(value: GuestSession | null) {
  try {
    if (value) sessionStorage.setItem(storageKey, JSON.stringify(value));
    else sessionStorage.removeItem(storageKey);
  } catch { /* The in-memory session still works until reload. */ }
}

async function createSession(): Promise<GuestSession> {
  const body = await apiRequest("/guest/session", { method: "POST" }) as Record<string, unknown>;
  const expiresAt = typeof body.expiresAt === "string" ? Date.parse(body.expiresAt) : NaN;
  if (typeof body.guestToken !== "string" || !tokenPattern.test(body.guestToken) || !Number.isFinite(expiresAt)) {
    throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
  }
  return { token: body.guestToken, expiresAt };
}

export async function getGuestToken(): Promise<string> {
  if (usable(session)) return session.token;
  const stored = readStored();
  if (usable(stored)) return (session = stored).token;
  sessionPromise ??= createSession()
    .then(created => { session = created; store(created); return created; })
    .finally(() => { sessionPromise = null; });
  return (await sessionPromise).token;
}

export function clearGuestSession() {
  session = null;
  store(null);
}

function withToken(init: RequestInit, token: string): RequestInit {
  const headers = new Headers(init.headers);
  if (init.body) headers.set("Content-Type", "application/json");
  headers.set("X-Guest-Token", token);
  return { ...init, headers, cache: "no-store" };
}

/**
 * Calls a backend endpoint that needs a guest. A 401 means the token was rejected before the
 * request ran (expired, or the backend's signing key changed), so it is safe to retry once.
 * Resolves to null for 204 No Content.
 */
export async function guestRequest<T>(path: string, init: RequestInit = {}, signal?: AbortSignal): Promise<T> {
  try {
    return await apiRequest(path, withToken(init, await getGuestToken()), signal, true) as T;
  } catch (cause) {
    if (!(cause instanceof ApiError) || cause.status !== 401) throw cause;
    clearGuestSession();
    return await apiRequest(path, withToken(init, await getGuestToken()), signal, true) as T;
  }
}

// ---------------------------------------------------------------------------
// Adapters between the Spring contract and the frontend's types.
// The backend uses upper-case frequencies and a richer subscription; the screens keep using the
// shapes in types/social.ts.

function invalid(): never {
  throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
}

function object(value: unknown): Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value) ? value as Record<string, unknown> : invalid();
}

const interestIds = new Set<string>(INTERESTS.map(interest => interest.id));
const frequencies = ["instant", "daily", "weekly"] as const;

function parsePreferences(value: unknown): AlertPreferences {
  const body = object(value);
  const frequency = typeof body.frequency === "string" ? body.frequency.toLowerCase() : "";
  if (typeof body.city !== "string" || !Array.isArray(body.categorySlugs) || !frequencies.includes(frequency as typeof frequencies[number]) ||
      typeof body.paused !== "boolean" || (body.budgetMax != null && typeof body.budgetMax !== "number")) invalid();
  return {
    city: body.city,
    categorySlugs: body.categorySlugs.map(String),
    ...(typeof body.budgetMax === "number" ? { budgetMax: body.budgetMax } : {}),
    frequency: frequency as AlertPreferences["frequency"],
    channels: ["in_app"],
    paused: body.paused,
  };
}

/**
 * A category the backend alerts on (GET /api/meta), with its label in the page language (outly-lang cookie)
 * and the planner interests it covers; e.g. "tech" covers technology, AI and crypto events.
 */
export interface AlertCategory { id: string; label: string; interests: Interest[] }

const categoryRequests = new Map<string, Promise<AlertCategory[]>>();

/** Cached per language for the page's lifetime; a failed request is retried on the next call. */
export function getAlertCategories(locale: string): Promise<AlertCategory[]> {
  let request = categoryRequests.get(locale);
  if (!request) {
    request = apiRequest("/meta", { cache: "no-store" }).then(value => {
      const body = object(value);
      if (!Array.isArray(body.categories)) invalid();
      return body.categories.map((item): AlertCategory => {
        const category = object(item);
        if (typeof category.id !== "string" || typeof category.label !== "string" || !Array.isArray(category.interests)) invalid();
        return { id: category.id, label: category.label, interests: category.interests.map(String).filter(id => interestIds.has(id)) as Interest[] };
      });
    });
    request.catch(() => categoryRequests.delete(locale));
    categoryRequests.set(locale, request);
  }
  return request;
}

/** The planner interest whose icon stands for a category (its first interest). */
export function categoryIcon(category: AlertCategory | undefined): string {
  return INTERESTS.find(interest => interest.id === category?.interests[0])?.icon ?? "bell";
}

/** The saved preferences, or null when the guest has not configured alerts yet. */
export async function getAlertPreferences(): Promise<AlertPreferences | null> {
  const body = object(await guestRequest<unknown>("/alerts/preferences"));
  return body.configured === true ? parsePreferences(body.preferences) : null;
}

export async function saveAlertPreferences(preferences: AlertPreferences): Promise<AlertPreferences> {
  const body = object(await guestRequest<unknown>("/alerts/preferences", {
    method: "PUT",
    body: JSON.stringify({
      city: preferences.city, categorySlugs: preferences.categorySlugs, frequency: preferences.frequency.toUpperCase(),
      channels: ["in_app"], paused: preferences.paused,
      ...(preferences.budgetMax !== undefined ? { budgetMax: preferences.budgetMax } : {}),
    }),
  }));
  return parsePreferences(body.preferences);
}

export async function getNotifications(): Promise<NotificationFeed> {
  const body = object(await guestRequest<unknown>("/notifications?size=50"));
  if (typeof body.unreadCount !== "number" || !Array.isArray(body.items)) invalid();
  return {
    unreadCount: body.unreadCount,
    items: body.items.map((value): DemoNotification => {
      const item = object(value);
      if (typeof item.id !== "string" || typeof item.eventId !== "string" || typeof item.title !== "string" ||
          typeof item.createdAt !== "string" || (item.readAt !== null && typeof item.readAt !== "string")) invalid();
      return {
        id: item.id, eventId: item.eventId, title: item.title, createdAt: item.createdAt, readAt: item.readAt as string | null,
        categorySlug: String(item.categorySlug), demo: true,
      };
    }),
  };
}

export async function markNotificationRead(id: string): Promise<void> {
  await guestRequest<null>(`/notifications/${encodeURIComponent(id)}/read`, { method: "PATCH" });
}

/** Demo only (backend DEMO_ENABLED): matches the catalog against the saved alerts right now. */
export async function checkAlertsNow(): Promise<{ checked: number; created: number }> {
  const body = object(await guestRequest<unknown>("/demo/check-alerts", { method: "POST" }));
  if (typeof body.checked !== "number" || typeof body.created !== "number") invalid();
  return { checked: body.checked, created: body.created };
}

function parseSubscription(value: unknown): DemoSubscription {
  const body = object(value);
  const plan = body.plan == null ? {} : object(body.plan);
  if (typeof body.isPremium !== "boolean") invalid();
  return {
    // The screens only distinguish free from premium; PREMIUM_ACTIVE counts as premium too.
    status: body.isPremium ? "DEMO_PREMIUM" : "FREE",
    price: typeof plan.priceCents === "number" ? plan.priceCents / 100 : 9.99,
    currency: typeof plan.currency === "string" ? plan.currency : "USD",
  };
}

export async function getSubscription(): Promise<DemoSubscription> {
  return parseSubscription(await guestRequest<unknown>("/subscription"));
}

/** Demo only (backend DEMO_ENABLED). */
export async function activateDemoPremium(): Promise<DemoSubscription> {
  return parseSubscription(await guestRequest<unknown>("/demo/premium", { method: "POST" }));
}

/** Demo only (backend DEMO_ENABLED). Ends demo Premium; a paid subscription is never changed. */
export async function deactivateDemoPremium(): Promise<DemoSubscription> {
  return parseSubscription(await guestRequest<unknown>("/demo/premium", { method: "DELETE" }));
}

// ---------------------------------------------------------------------------
// Pair planning. Each participant gets a participantToken (sent as X-Pair-Token) next to the guest
// token. It is kept per tab, like the guest session.

const pairTokenKey = (sessionId: string) => `outly.pair.token:${sessionId}`;
const catalogBySourceId = new Map(CATALOG_EVENTS.map(event => [event.sourceEventId, event]));

function savePairToken(sessionId: string, token: string) {
  try { sessionStorage.setItem(pairTokenKey(sessionId), token); } catch { /* Lost on reload when storage is unavailable. */ }
}

function pairToken(sessionId: string): string {
  let token: string | null = null;
  try { token = sessionStorage.getItem(pairTokenKey(sessionId)); } catch { /* Treated as missing below. */ }
  if (!token) throw new ApiError("Open this plan in the tab where you created or joined it.", 404, "PAIR_TOKEN_MISSING");
  return token;
}

async function pairRequest<T>(sessionId: string, path: string, init: RequestInit = {}): Promise<T> {
  const headers = new Headers(init.headers);
  headers.set("X-Pair-Token", pairToken(sessionId));
  return guestRequest<T>(path, { ...init, headers });
}

/**
 * The backend serves the frontend catalog; show the frontend's own copy of each event so cards and links stay
 * the same. Other events (e.g. backend demo seeds) are used as sent when they are complete, otherwise null.
 */
function catalogEvent(value: unknown): EventData | null {
  const event = object(value);
  const local = typeof event.sourceEventId === "string" ? catalogBySourceId.get(event.sourceEventId) : undefined;
  return local ?? (isEvent(event) ? event : null);
}

function pairPreferences(value: unknown): PairPreferences {
  const body = object(value);
  if (!Array.isArray(body.interests) || typeof body.dateFrom !== "string" || typeof body.dateTo !== "string") invalid();
  return {
    interests: body.interests.map(String) as Interest[],
    startDate: body.dateFrom, endDate: body.dateTo,
    timePreference: (typeof body.timePreference === "string" ? body.timePreference.toLowerCase() : "any") as PairPreferences["timePreference"],
    ...(typeof body.budgetMax === "number" ? { budgetMax: body.budgetMax } : {}),
    ...(typeof body.message === "string" && body.message ? { notes: body.message } : {}),
  };
}

const pairStatuses: Record<string, PairSession["status"]> = {
  WAITING_FOR_B: "WAITING", READY: "READY", NO_MATCH: "READY", MATCHED: "MATCHED", EXPIRED: "EXPIRED", REVOKED: "CLOSED",
};
const voteTypes = ["LIKE", "MAYBE", "PASS"];

function parsePairSession(value: unknown): PairSession {
  const body = object(value);
  const status = pairStatuses[String(body.status)];
  const role = body.myRole === "A" || body.myRole === "B" ? body.myRole : invalid();
  if (typeof body.sessionId !== "string" || !status || typeof body.expiresAt !== "string") invalid();
  const me = pairPreferences(body.me);
  const partner = body.partner == null ? null : pairPreferences(body.partner);
  const other = role === "A" ? "B" : "A";
  const votes: PairSession["votes"] = { A: {}, B: {} };
  const recommendations = (Array.isArray(body.suggestions) ? body.suggestions : []).flatMap((value): PairSuggestion[] => {
    const item = object(value);
    const backendEvent = object(item.event);
    const event = catalogEvent(backendEvent);
    if (!event) return [];
    if (voteTypes.includes(String(item.myVote))) votes[role][event.id] = item.myVote as PairVote;
    if (voteTypes.includes(String(item.partnerVote))) votes[other][event.id] = item.partnerVote as PairVote;
    const reason = role === "A" ? item.reasonA : item.reasonB;
    return [{ event, reason: typeof reason === "string" ? reason : "", voteId: String(backendEvent.id) }];
  });
  const shared = body.sharedPlan == null ? null : catalogEvent(object(body.sharedPlan).event);
  const conflict = body.conflict == null ? null : object(body.conflict);
  return {
    id: body.sessionId, city: typeof body.cityName === "string" ? body.cityName : String(body.city), role,
    participantA: role === "A" ? me : partner ?? invalid(), participantB: role === "B" ? me : partner,
    status, recommendations, votes, sharedEventId: shared?.id ?? null, expiresAt: body.expiresAt, demo: true,
    ...(conflict && typeof conflict.message === "string"
      ? { conflict: { message: conflict.message, relaxMessage: typeof conflict.relaxMessage === "string" ? conflict.relaxMessage : null } } : {}),
  };
}

function pairPreferencesBody(preferences: PairPreferences) {
  return {
    interests: preferences.interests, dateFrom: preferences.startDate, dateTo: preferences.endDate,
    timePreference: preferences.timePreference,
    ...(preferences.budgetMax !== undefined ? { budgetMax: preferences.budgetMax } : {}),
    ...(preferences.notes ? { message: preferences.notes } : {}),
  };
}

export function getPairSession(sessionId: string): Promise<PairSession> {
  return pairRequest<unknown>(sessionId, `/pair/${encodeURIComponent(sessionId)}`).then(parsePairSession);
}

/** Creates a session as side A. inviteToken goes into the partner's link. */
export async function createPairSession(city: string, preferences: PairPreferences): Promise<{ session: PairSession; inviteToken: string }> {
  const body = object(await guestRequest<unknown>("/pair/sessions", {
    method: "POST", body: JSON.stringify({ city, participantA: pairPreferencesBody(preferences) }),
  }));
  const inviteToken = typeof body.inviteUrl === "string" ? new URLSearchParams(body.inviteUrl.split("#")[1] ?? "").get("invite") : null;
  if (typeof body.sessionId !== "string" || typeof body.participantToken !== "string" || !inviteToken) invalid();
  savePairToken(body.sessionId, body.participantToken);
  return { session: await getPairSession(body.sessionId), inviteToken };
}

/** Joins as side B with the one-time invite. */
export async function joinPairSession(inviteToken: string, preferences: PairPreferences): Promise<PairSession> {
  const body = object(await guestRequest<unknown>("/pair/join", {
    method: "POST", body: JSON.stringify({ inviteToken, participantB: pairPreferencesBody(preferences) }),
  }));
  if (typeof body.sessionId !== "string" || typeof body.participantToken !== "string") invalid();
  savePairToken(body.sessionId, body.participantToken);
  return getPairSession(body.sessionId);
}

export function votePair(sessionId: string, eventId: string, vote: PairVote): Promise<PairSession> {
  return pairRequest<unknown>(sessionId, `/pair/${encodeURIComponent(sessionId)}/votes`, {
    method: "POST", body: JSON.stringify({ eventId, vote }),
  }).then(parsePairSession);
}

/** Creator only: ends the session and invalidates the invite. */
export async function revokePairSession(sessionId: string): Promise<PairSession> {
  await pairRequest<null>(sessionId, `/pair/${encodeURIComponent(sessionId)}/revoke`, { method: "POST" });
  return getPairSession(sessionId);
}

// ---------------------------------------------------------------------------
// AI assistant chat (Spring POST /api/ai/chat, answered with Google Gemini). The screen keeps the
// AssistantChatResponse shape; events are shown from the frontend catalog like everywhere else.

const pendingFieldIds: AssistantPendingField[] = ["city", "interests", "dates", "budgetMax", "timePreference"];
const timePreferences = ["any", "morning", "afternoon", "evening"];

function chatPreferences(value: unknown): Partial<PlannerRequest> {
  const body = object(value);
  const preferences: Partial<PlannerRequest> = {};
  if (typeof body.city === "string") preferences.city = body.city;
  if (Array.isArray(body.interests)) preferences.interests = body.interests.map(String).filter(id => interestIds.has(id)) as Interest[];
  if (typeof body.startDate === "string") preferences.startDate = body.startDate;
  if (typeof body.endDate === "string") preferences.endDate = body.endDate;
  if (typeof body.budgetMax === "number") preferences.budgetMax = body.budgetMax;
  if (typeof body.timePreference === "string" && timePreferences.includes(body.timePreference)) preferences.timePreference = body.timePreference as PlannerRequest["timePreference"];
  if (typeof body.notes === "string") preferences.notes = body.notes;
  return preferences;
}

function chatResult(value: unknown): PlannerResponse | null {
  if (value == null) return null;
  const body = object(value);
  const recommendations = (Array.isArray(body.recommendations) ? body.recommendations : []).flatMap(item => {
    const recommendation = object(item);
    const event = catalogEvent(recommendation.event);
    return event && typeof recommendation.matchScore === "number" && typeof recommendation.reason === "string"
      ? [{ event, matchScore: recommendation.matchScore, reason: recommendation.reason }] : [];
  });
  const suggestions = (Array.isArray(body.relaxationSuggestions) ? body.relaxationSuggestions : [])
    .flatMap(item => { const message = object(item).message; return typeof message === "string" ? [message] : []; });
  return {
    // No match means no ranked plan (and no id) on the backend.
    planId: typeof body.planId === "string" && body.planId ? body.planId : "no-match",
    summary: typeof body.summary === "string" ? body.summary : "",
    recommendations,
    ...(suggestions.length ? { suggestions } : {}),
  };
}

/** One chat turn. previous is the last answer of this chat (its preferences and conversationId are sent back). */
export async function assistantChat(message: string, previous: AssistantChatResponse | undefined, signal?: AbortSignal): Promise<AssistantChatResponse> {
  const body = object(await guestRequest<unknown>("/ai/chat", {
    method: "POST",
    body: JSON.stringify({
      message,
      ...(previous ? { preferences: previous.preferences } : {}),
      ...(previous?.conversationId ? { conversationId: previous.conversationId } : {}),
    }),
  }, signal));
  if (!Array.isArray(body.pendingFields) || !Array.isArray(body.clarifications)) invalid();
  return {
    engine: "ai",
    preferences: chatPreferences(body.preferences),
    pendingFields: body.pendingFields.map(String).filter((id): id is AssistantPendingField => pendingFieldIds.includes(id as AssistantPendingField)),
    clarifications: body.clarifications.filter((item): item is string => typeof item === "string"),
    result: chatResult(body.result),
    ...(typeof body.conversationId === "string" ? { conversationId: body.conversationId } : {}),
  };
}

// ---------------------------------------------------------------------------
// Event Buddy and moderation (Spring /api/buddy, /api/reports, /api/blocks). The screen shows one event at a
// time: its open posts (public feed) merged with the caller's own posts and requests (GET /api/buddy/me).

const postStatuses: BuddyPost["status"][] = ["OPEN", "MATCHED", "CLOSED", "EXPIRED"];
const requestStatuses: BuddyRequest["status"][] = ["PENDING", "ACCEPTED", "DECLINED", "CANCELLED"];

/** The backend knows catalog events as json:<id>; any other id is sent as it is. */
export function backendEventId(eventId: string): string {
  const local = CATALOG_EVENTS.find(event => event.id === eventId);
  return local ? `json:${local.sourceEventId}` : eventId;
}

function buddyPost(value: unknown, frontendEventId: string): BuddyPost {
  const body = object(value);
  if (typeof body.id !== "string" || typeof body.displayName !== "string" || typeof body.shortBio !== "string" ||
      !postStatuses.includes(body.status as BuddyPost["status"]) || typeof body.expiresAt !== "string") invalid();
  return {
    id: body.id, eventId: frontendEventId, displayName: body.displayName, bio: body.shortBio,
    language: typeof body.language === "string" ? body.language : null,
    status: body.status as BuddyPost["status"], isOwner: body.own === true, requests: [],
    createdAt: typeof body.createdAt === "string" ? body.createdAt : body.expiresAt, expiresAt: body.expiresAt, demo: true,
  };
}

function buddyRequest(value: unknown, isOwner: boolean): BuddyRequest {
  const body = object(value);
  if (typeof body.id !== "string" || typeof body.displayName !== "string" || typeof body.message !== "string" ||
      !requestStatuses.includes(body.status as BuddyRequest["status"])) invalid();
  return {
    id: body.id, displayName: body.displayName, message: body.message, status: body.status as BuddyRequest["status"],
    isOwner, createdAt: typeof body.createdAt === "string" ? body.createdAt : "",
    ...(typeof body.safetyNotice === "string" ? { safetyNotice: body.safetyNotice } : {}),
  };
}

/**
 * The event's open posts plus the caller's own posts for it (with incoming requests) and the posts the caller
 * requested to join (with that request, even once matched or closed). isOwner on a request = the caller sent it.
 */
export async function getBuddyPosts(eventId: string): Promise<BuddyPost[]> {
  const id = backendEventId(eventId);
  const [feed, me] = await Promise.all([
    guestRequest<unknown>(`/buddy/posts?eventId=${encodeURIComponent(id)}`).then(object),
    guestRequest<unknown>("/buddy/me").then(object),
  ]);
  if (!Array.isArray(feed.posts) || !Array.isArray(me.posts) || !Array.isArray(me.requests)) invalid();
  const posts = new Map<string, BuddyPost>();
  for (const value of feed.posts) { const post = buddyPost(value, eventId); posts.set(post.id, post); }
  for (const value of me.posts) {
    const mine = object(value);
    if (object(mine.post).eventId !== id) continue;
    const post = buddyPost(mine.post, eventId);
    post.requests = (Array.isArray(mine.requests) ? mine.requests : []).map(request => buddyRequest(request, false));
    posts.set(post.id, post);
  }
  for (const value of me.requests) {
    const sent = object(value);
    if (object(sent.post).eventId !== id) continue;
    const post = posts.get(String(object(sent.post).id)) ?? buddyPost(sent.post, eventId);
    post.requests = [...post.requests, buddyRequest(sent, true)];
    posts.set(post.id, post);
  }
  return [...posts.values()];
}

export async function createBuddyPost(eventId: string, post: { displayName: string; bio: string; language: string; ageConfirmed: boolean }): Promise<void> {
  await guestRequest("/buddy/posts", {
    method: "POST",
    body: JSON.stringify({ eventId: backendEventId(eventId), displayName: post.displayName, shortBio: post.bio, language: post.language, ageConfirmed: post.ageConfirmed }),
  });
}

export async function requestToJoinBuddy(postId: string, request: { displayName: string; message: string; ageConfirmed: boolean }): Promise<void> {
  await guestRequest(`/buddy/posts/${encodeURIComponent(postId)}/requests`, { method: "POST", body: JSON.stringify(request) });
}

export async function decideBuddyRequest(requestId: string, decision: "ACCEPT" | "DECLINE"): Promise<void> {
  await guestRequest(`/buddy/requests/${encodeURIComponent(requestId)}/decision`, { method: "POST", body: JSON.stringify({ decision }) });
}

export async function cancelBuddyRequest(requestId: string): Promise<void> {
  await guestRequest(`/buddy/requests/${encodeURIComponent(requestId)}/cancel`, { method: "POST" });
}

export async function closeBuddyPost(postId: string): Promise<void> {
  await guestRequest(`/buddy/posts/${encodeURIComponent(postId)}/close`, { method: "POST" });
}

export const REPORT_REASONS = ["SPAM", "HARASSMENT", "INAPPROPRIATE", "SCAM", "UNDERAGE", "CONTACT_DETAILS", "OTHER"] as const;
export type ReportReason = typeof REPORT_REASONS[number];

/** Reports a post or request (stored for moderation); the other person is not told. */
export async function reportBuddy(targetType: "POST" | "REQUEST", targetId: string, reason: ReportReason, comment?: string): Promise<void> {
  await guestRequest("/reports", { method: "POST", body: JSON.stringify({ targetType, targetId, reason, ...(comment ? { comment } : {}) }) });
}

/** Blocks the author of a post or request; returns the scopeRef that lifts the block. */
export async function blockBuddy(targetType: "POST" | "REQUEST", targetId: string): Promise<string> {
  const body = object(await guestRequest<unknown>("/blocks", { method: "POST", body: JSON.stringify({ targetType, targetId }) }));
  if (typeof body.scopeRef !== "string") invalid();
  return body.scopeRef;
}

export async function unblockBuddy(scopeRef: string): Promise<void> {
  await guestRequest(`/blocks/${encodeURIComponent(scopeRef)}`, { method: "DELETE" });
}
