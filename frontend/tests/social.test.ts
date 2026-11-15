import assert from "node:assert/strict";
import { mkdtemp, readFile, rm, writeFile } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { CATALOG_EVENTS, catalogMatchesDate } from "../src/lib/catalog.ts";
import { addDays, getCityToday } from "../src/lib/planner.ts";
import { consumeAssistantChat, consumeAssistantSession, matchPairEvents, matchesAlert } from "../src/lib/server/social.ts";
import { GET, POST, PUT, PATCH } from "../src/app/api/demo/[...path]/route.ts";
import type { EventData } from "../src/types/event.ts";
import type { AlertPreferences, PairPreferences } from "../src/types/social.ts";

const today = getCityToday();
const future = addDays(today, 30);
const preferences: PairPreferences = { displayName: "Alex", interests: ["gaming", "fitness", "running", "art", "family"], startDate: today, endDate: future, timePreference: "any" };
const event = (overrides: Partial<EventData> = {}): EventData => ({ id: "test", source: "catalog", sourceEventId: "test", title: "Verified test", category: "music", city: "New York", startAt: `${today}T19:00:00-05:00`, timezone: "America/New_York", venue: null, address: null, description: null, imageUrl: null, priceMin: 25, priceMax: 25, priceKnown: true, currency: "USD", sourceUrl: "https://example.org/event", status: "scheduled", ...overrides });

async function call(path: string, method = "GET", token?: string, body?: unknown) {
  const url = new URL(`http://localhost/api/demo/${path}`);
  const request = new Request(url, { method, headers: { ...(token ? { authorization: `Bearer ${token}` } : {}), ...(body !== undefined ? { "Content-Type": "application/json" } : {}) }, ...(body !== undefined ? { body: JSON.stringify(body) } : {}) });
  const handler = ({ GET, POST, PUT, PATCH } as Record<string, typeof GET>)[method];
  const response = await handler(request, { params: Promise.resolve({ path: url.pathname.replace("/api/demo/", "").split("/") }) });
  return { status: response.status, body: await response.json() };
}

test("pair matching enforces both dates, strictest budget, time and source IDs", () => {
  const a = { ...preferences, interests: ["music"] as PairPreferences["interests"], budgetMax: 50 };
  const b = { ...a, displayName: "Blair", budgetMax: 30 };
  const events = [event(), event({ id: "expensive", priceMin: 31 }), event({ id: "unknown", priceMin: null, priceKnown: false }), event({ id: "other-city", city: "Boston" }), event({ id: "cancelled", status: "cancelled" })];
  assert.deepEqual(matchPairEvents("New York", a, b, events).map(item => item.event.id), ["test"]);
  assert.equal(matchPairEvents("New York", a, { ...b, startDate: addDays(today, 1) }, events).length, 0);
  assert.equal(matchPairEvents("New York", { ...a, timePreference: "morning" }, { ...b, timePreference: "evening" }, events).length, 0);
  assert.equal(matchPairEvents("New York", { ...a, timePreference: "evening" }, b, [event({ startAt: null, startDate: today })]).length, 0);
  assert.equal(matchPairEvents("New York", { ...a, budgetMax: 0 }, { ...b, budgetMax: 0 }, [event({ priceMin: null, priceKnown: false })]).length, 0);
});

test("alert matching excludes sports, wrong city, unknown price, paused and expired events", () => {
  const settings: AlertPreferences = { city: "New York", categorySlugs: ["music"], frequency: "instant", channels: ["in_app"], paused: false, budgetMax: 30 };
  assert.equal(matchesAlert(event(), settings), true);
  for (const item of [event({ category: "sports" }), event({ city: "Boston" }), event({ priceKnown: false, priceMin: null }), event({ status: "cancelled" }), event({ priceMin: 31 }), event({ startAt: "2001-01-01T19:00:00-05:00" })]) assert.equal(matchesAlert(item, settings), false);
  assert.equal(matchesAlert(event(), { ...settings, paused: true }), false);
});

test("persisted demo API isolates guests and completes Buddy, pair and notification flows", async t => {
  const directory = await mkdtemp(join(tmpdir(), "outly-social-"));
  process.env.OUTLY_DEMO_DIR = directory;
  t.after(async () => { delete process.env.OUTLY_DEMO_DIR; await rm(directory, { recursive: true, force: true }); });
  const a = (await call("session", "POST", undefined, {})).body.token as string;
  const b = (await call("session", "POST", undefined, {})).body.token as string;
  const stranger = (await call("session", "POST", undefined, {})).body.token as string;
  assert.match(a, /^[a-f0-9]{64}$/); assert.notEqual(a, b);
  const available = CATALOG_EVENTS.find(item => item.city === "New York" && [...Array(31).keys()].some(day => catalogMatchesDate(item, addDays(today, day))))!;
  assert.ok(available);

  await t.test("entitlements are server-owned and token hashes persist without secrets", async () => {
    assert.equal((await call("subscription")).status, 401);
    assert.equal((await call("alerts/preferences", "GET", a)).status, 403);
    assert.equal((await call("buddy/posts", "POST", a, { eventId: available.id, displayName: "Alex", bio: "A relaxed evening", language: "English", ageConfirmed: true })).status, 403);
    assert.equal((await call("subscription", "POST", a, { action: "activate" })).body.status, "DEMO_PREMIUM");
    await call("subscription", "POST", b, { action: "activate" });
    assert.deepEqual((await call("subscription", "GET", a)).body, { status: "DEMO_PREMIUM", price: 4.99, currency: "USD" });
    const raw = await readFile(join(directory, "state.json"), "utf8");
    assert.ok(!raw.includes(a) && !raw.includes(b));
    assert.ok(raw.includes("tokenHash"));
  });

  let postId = "";
  await t.test("Buddy validates age, rejects contacts and unknown events", async () => {
    const payload = { eventId: available.id, displayName: "Alex", bio: "Looking for a relaxed outing", language: "English", ageConfirmed: true };
    assert.equal((await call("buddy/posts", "POST", a, { ...payload, ageConfirmed: false })).status, 400);
    assert.equal((await call("buddy/posts", "POST", a, { ...payload, bio: "Email me at alex@example.com" })).status, 400);
    assert.equal((await call("buddy/posts", "POST", a, { ...payload, eventId: "imaginary" })).status, 404);
    const posted = await call("buddy/posts", "POST", a, payload);
    assert.equal(posted.status, 200); assert.equal(posted.body.post.demo, true); assert.equal(posted.body.post.isOwner, true);
    postId = posted.body.post.id;
    assert.equal((await call(`buddy/posts?eventId=${encodeURIComponent(available.id)}`, "GET", b)).body.posts[0].id, postId);
    assert.ok(!JSON.stringify(posted.body).includes("ownerId"));
  });

  await t.test("Buddy author accepts and sender sees match, outsiders cannot decide or read messages", async () => {
    const joined = await call(`buddy/posts/${postId}/requests`, "POST", b, { displayName: "Blair", message: "Happy to meet at the venue", ageConfirmed: true });
    assert.equal(joined.status, 200);
    const requestId = joined.body.post.requests[0].id;
    assert.equal((await call(`buddy/requests/${requestId}/decision`, "POST", b, { decision: "ACCEPTED" })).status, 403);
    assert.equal((await call(`buddy/requests/${requestId}/decision`, "POST", stranger, { decision: "ACCEPTED" })).status, 403);
    assert.equal((await call("buddy/posts", "GET", stranger)).body.posts[0].requests.length, 0);
    const accepted = await call(`buddy/requests/${requestId}/decision`, "POST", a, { decision: "ACCEPTED" });
    assert.equal(accepted.body.post.status, "MATCHED");
    assert.equal((await call("buddy/posts", "GET", b)).body.posts[0].requests[0].status, "ACCEPTED");
    assert.equal((await call(`buddy/requests/${requestId}/cancel`, "POST", b, {})).body.post.status, "OPEN");
  });

  await t.test("report and block are free and block prevents interaction in both directions", async () => {
    assert.equal((await call("safety", "POST", stranger, { postId, kind: "REPORT", reason: "Please review this demo post" })).status, 200);
    const blocked = await call("safety", "POST", b, { postId, kind: "BLOCK", reason: "Do not show this person" });
    assert.equal(blocked.status, 200);
    assert.equal((await call("buddy/posts", "GET", b)).body.posts.length, 0);
    assert.equal((await call(`buddy/posts/${postId}/requests`, "POST", b, { displayName: "Blair", message: "Try again", ageConfirmed: true })).status, 403);
    assert.equal((await call(`buddy/posts/${postId}/close`, "POST", b, {})).status, 403);
    assert.equal((await call(`buddy/posts/${postId}/close`, "POST", a, {})).body.post.status, "CLOSED");
  });

  await t.test("independent pair roles share constraints and reach a mutual like", async () => {
    const created = await call("pair/sessions", "POST", a, { city: "New York", participantA: { ...preferences, budgetMax: 50 } });
    assert.equal(created.status, 200);
    const { session, inviteToken } = created.body;
    assert.match(inviteToken, /^[a-f0-9]{64}$/);
    assert.equal((await call(`pair/${session.id}`, "GET", stranger)).status, 403);
    assert.equal((await call("pair/join", "POST", a, { sessionId: session.id, inviteToken, participantB: preferences })).status, 409);
    assert.equal((await call("pair/join", "POST", b, { sessionId: session.id, inviteToken: "bad", participantB: preferences })).status, 403);
    const joined = await call("pair/join", "POST", b, { sessionId: session.id, inviteToken, participantB: { ...preferences, displayName: "Blair", budgetMax: 30 } });
    assert.equal(joined.status, 200); assert.equal(joined.body.session.role, "B");
    assert.ok(joined.body.session.recommendations.length);
    assert.ok(joined.body.session.recommendations.every((item: { event: EventData }) => item.event.priceKnown && item.event.priceMin! <= 30));
    assert.equal((await call("pair/join", "POST", stranger, { sessionId: session.id, inviteToken, participantB: preferences })).status, 403);
    const id = joined.body.session.recommendations[0].event.id;
    assert.equal((await call(`pair/${session.id}/votes`, "POST", stranger, { eventId: id, vote: "LIKE" })).status, 403);
    assert.equal((await call(`pair/${session.id}/votes`, "POST", a, { eventId: "invented", vote: "LIKE" })).status, 400);
    assert.equal((await call(`pair/${session.id}/votes`, "POST", a, { eventId: id, vote: "LIKE" })).body.session.status, "READY");
    const result = await call(`pair/${session.id}/votes`, "POST", b, { eventId: id, vote: "LIKE" });
    assert.equal(result.body.session.status, "MATCHED"); assert.equal(result.body.session.sharedEventId, id);
    assert.equal((await call(`pair/${session.id}`, "GET", a)).body.session.sharedEventId, id);
    assert.ok(!JSON.stringify(result.body).includes("inviteHash"));
    assert.equal((await call(`pair/${session.id}/close`, "POST", b, {})).status, 403);
    assert.equal((await call(`pair/${session.id}/close`, "POST", a, {})).body.session.status, "CLOSED");
    assert.equal((await call(`pair/${session.id}/votes`, "POST", b, { eventId: id, vote: "PASS" })).status, 410);
  });

  await t.test("Free allows one pair trial and weekly assistant trial; Premium quota differs", async () => {
    assert.equal((await call("pair/sessions", "POST", stranger, { city: "New York", participantA: preferences })).status, 200);
    assert.equal((await call("pair/sessions", "POST", stranger, { city: "New York", participantA: preferences })).status, 403);
    const request = new Request("http://localhost", { headers: { authorization: `Bearer ${stranger}` } });
    await consumeAssistantSession(request);
    await assert.rejects(consumeAssistantSession(request), { code: "ASSISTANT_LIMIT" });
    await consumeAssistantSession(new Request("http://localhost", { headers: { authorization: `Bearer ${a}` } }));
    await consumeAssistantSession(new Request("http://localhost", { headers: { authorization: `Bearer ${a}` } }));
  });

  await t.test("assistant conversations charge once, remain scope-owned and enforce persisted expiry and turns", async () => {
    const token = (await call("session", "POST", undefined, {})).body.token as string;
    const foreignToken = (await call("session", "POST", undefined, {})).body.token as string;
    const request = new Request("http://localhost", { headers: { authorization: `Bearer ${token}` } });
    const foreignRequest = new Request("http://localhost", { headers: { authorization: `Bearer ${foreignToken}` } });
    const conversationId = await consumeAssistantChat(request);
    assert.match(conversationId, /^[a-f0-9-]{36}$/);
    assert.equal(await consumeAssistantChat(request, conversationId), conversationId);
    await assert.rejects(consumeAssistantChat(request), { code: "ASSISTANT_LIMIT" });
    await assert.rejects(consumeAssistantChat(foreignRequest, conversationId), { status: 404, code: "ASSISTANT_CONVERSATION_NOT_FOUND" });
    await assert.rejects(consumeAssistantChat(foreignRequest, "client-created-context"), { status: 400, code: "VALIDATION_ERROR" });
    const file = join(directory, "state.json");
    const stored = JSON.parse(await readFile(file, "utf8"));
    const owner = stored.scopes.find((item: { assistantChats?: { id: string }[] }) => item.assistantChats?.some(chat => chat.id === conversationId));
    assert.equal(owner.assistantAt.length, 1);
    assert.equal(owner.assistantChats[0].turns, 2);
    assert.ok(!JSON.stringify(stored).includes(token));
    owner.assistantChats[0].expiresAt = Date.now() - 1;
    await writeFile(file, JSON.stringify(stored));
    await assert.rejects(consumeAssistantChat(request, conversationId), { status: 410, code: "ASSISTANT_CONVERSATION_EXPIRED" });
    owner.assistantChats[0].expiresAt = Date.now() + 60_000;
    owner.assistantChats[0].turns = 30;
    await writeFile(file, JSON.stringify(stored));
    await assert.rejects(consumeAssistantChat(request, conversationId), { status: 429, code: "ASSISTANT_TURN_LIMIT" });
    owner.assistantChats = [];
    await writeFile(file, JSON.stringify(stored));
    await assert.rejects(consumeAssistantChat(request, conversationId), { status: 404, code: "ASSISTANT_CONVERSATION_NOT_FOUND" });
    assert.equal(typeof await consumeAssistantChat(foreignRequest), "string");
  });

  await t.test("saved alerts filter categories, deduplicate, pause and read only own notifications", async () => {
    const settings: AlertPreferences = { city: available.city, categorySlugs: [available.category as AlertPreferences["categorySlugs"][number]], frequency: "instant", channels: ["in_app"], paused: false };
    assert.equal((await call("alerts/preferences", "PUT", a, { ...settings, categorySlugs: [] })).status, 400);
    assert.equal((await call("alerts/preferences", "PUT", a, settings)).status, 200);
    assert.deepEqual((await call("alerts/preferences", "GET", a)).body.preferences, settings);
    const checked = await call("alerts/check", "POST", a, {});
    assert.equal(checked.body.created, 1); assert.equal(checked.body.unreadCount, 1);
    assert.ok(checked.body.items.every((item: { categorySlug: string }) => settings.categorySlugs.includes(item.categorySlug as AlertPreferences["categorySlugs"][number])));
    const id = checked.body.items[0].id;
    assert.equal((await call(`notifications/${id}/read`, "PATCH", b, {})).status, 404);
    assert.equal((await call(`notifications/${id}/read`, "PATCH", a, {})).body.unreadCount, 0);
    const rechecked = await call("alerts/check", "POST", a, {});
    assert.equal(new Set(rechecked.body.items.map((item: { eventId: string }) => item.eventId)).size, rechecked.body.items.length);
    await call("alerts/preferences", "PUT", a, { ...settings, paused: true });
    assert.equal((await call("alerts/check", "POST", a, {})).body.created, 0);
    await call("subscription", "POST", a, { action: "deactivate" });
    assert.equal((await call("alerts/check", "POST", a, {})).status, 403);
  });

  await t.test("digest alerts wait for delivery time and persisted state survives reads", async () => {
    const settings: AlertPreferences = { city: available.city, categorySlugs: [available.category as AlertPreferences["categorySlugs"][number]], frequency: "daily", channels: ["in_app"], paused: false };
    await call("alerts/preferences", "PUT", b, settings);
    const checked = await call("alerts/check", "POST", b, {});
    assert.equal(checked.body.created, 0); assert.equal(checked.body.pending, 1); assert.equal(checked.body.unreadCount, 0);
    const stored = JSON.parse(await readFile(join(directory, "state.json"), "utf8"));
    for (const notification of stored.notifications) notification.deliverAt = Date.now() - 1;
    await writeFile(join(directory, "state.json"), JSON.stringify(stored));
    assert.equal((await call("notifications", "GET", b)).body.unreadCount, 1);
  });
});
