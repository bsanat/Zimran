import assert from "node:assert/strict";
import { mkdtemp, rm } from "node:fs/promises";
import { tmpdir } from "node:os";
import { join } from "node:path";
import test from "node:test";
import { CATALOG_EVENTS } from "../src/lib/catalog.ts";
import { chatAssistant, parseAssistant, recommendCatalog, validateAssistantContext, validateAssistantPreferences } from "../src/lib/server/assistant.ts";
import type { EventData } from "../src/types/event.ts";
import type { PlannerRequest } from "../src/types/planner.ts";
import { POST as chatRoute } from "../src/app/api/ai/chat/route.ts";
import { POST as demoRoute } from "../src/app/api/demo/[...path]/route.ts";

const now = new Date("2026-11-14T18:00:00Z");
const input: PlannerRequest = { city: "New York", interests: ["music"], startDate: "2026-11-21", endDate: "2026-11-21", timePreference: "any", budgetMax: 30 };
function event(overrides: Partial<EventData> = {}): EventData {
  return { id: "fixture:music", source: "catalog", sourceEventId: "fixture", title: "Test music event", category: "music", city: "New York", startAt: "2026-11-21T23:00:00Z", timezone: "America/New_York", venue: "Test venue", address: null, description: null, imageUrl: null, priceMin: 25, priceMax: 25, priceKnown: true, currency: "USD", sourceUrl: "https://example.com", status: "scheduled", ...overrides };
}

test("assistant parses a natural-language message into editable strict filters", () => {
  const parsed = parseAssistant({ message: "I have $60 this Saturday evening in NYC. I like jazz and cozy places." }, now);
  assert.deepEqual(parsed.clarifications, []);
  assert.equal(parsed.engine, "catalog");
  assert.equal(parsed.preferences?.city, "New York");
  assert.deepEqual(parsed.preferences?.interests, ["music"]);
  assert.equal(parsed.preferences?.startDate, "2026-11-14");
  assert.equal(parsed.preferences?.endDate, "2026-11-14");
  assert.equal(parsed.preferences?.budgetMax, 60);
  assert.equal(parsed.preferences?.timePreference, "evening");
});

test("assistant asks for missing or ambiguous fields and retains detected preferences", () => {
  const absent = parseAssistant({ message: "Jazz under $30" }, now);
  assert.equal(absent.preferences, null);
  assert.equal(absent.detected.budgetMax, 30);
  assert.equal(absent.clarifications.length, 2);
  assert.equal(parseAssistant({ message: "New York or Boston, music tomorrow" }, now).preferences, null);
  assert.equal(parseAssistant({ message: "NYC music today or tomorrow" }, now).preferences, null);
  assert.equal(parseAssistant({ message: "NYC music tomorrow $30 or $50" }, now).preferences, null);
  assert.equal(parseAssistant({ message: "NYC music tomorrow -$5" }, now).preferences, null);
  assert.equal(parseAssistant({ message: "NYC music tomorrow morning or evening" }, now).preferences, null);
  assert.equal(parseAssistant({ message: "London music tomorrow" }, now).preferences, null);
});

test("assistant preserves New York dates at midnight and validates explicit date ranges", () => {
  const midnight = new Date("2026-11-15T02:00:00Z");
  assert.equal(parseAssistant({ message: "NYC music today" }, midnight).preferences?.startDate, "2026-11-14");
  assert.equal(parseAssistant({ message: "NYC music tonight" }, midnight).preferences?.startDate, "2026-11-14");
  const range = parseAssistant({ message: "Boston music 2026-11-21 to 2026-11-22" }, now);
  assert.equal(range.preferences?.startDate, "2026-11-21");
  assert.equal(range.preferences?.endDate, "2026-11-22");
  for (const message of ["NYC music 2026-11-13", "NYC music 2026-02-30", "NYC music 2026-11-22 to 2026-11-21", "NYC music 2026-11-21 today"]) assert.equal(parseAssistant({ message }, now).preferences, null, message);
  assert.equal(parseAssistant({ message: "NYC music next Saturday" }, now).preferences?.startDate, "2026-11-21");
});

test("assistant input validation rejects malformed payloads and excessive ranges", () => {
  for (const bad of [null, [], {}, { message: 12 }, { message: " " }, { message: "x".repeat(501) }]) assert.throws(() => parseAssistant(bad, now));
  for (const bad of [null, { ...input, city: "London" }, { ...input, interests: [] }, { ...input, interests: ["invented"] }, { ...input, startDate: "2026-11-13" }, { ...input, endDate: "2028-01-01" }, { ...input, budgetMax: -1 }, { ...input, budgetMax: Infinity }, { ...input, timePreference: "night" }, { ...input, notes: "x".repeat(501) }]) assert.throws(() => validateAssistantPreferences(bad, now));
});

test("recommendations enforce both known prices and USD budgets, including free only", () => {
  const values = [event(), event({ id: "fixture:unknown", priceKnown: false, priceMin: null }), event({ id: "fixture:expensive", priceMin: 40 }), event({ id: "fixture:eur", currency: "EUR" }), event({ id: "fixture:free", priceMin: 0, priceMax: 0 })];
  assert.deepEqual(recommendCatalog(input, now, values).recommendations.map(item => item.event.id).sort(), ["fixture:free", "fixture:music"]);
  assert.deepEqual(recommendCatalog({ ...input, budgetMax: 0 }, now, values).recommendations.map(item => item.event.id), ["fixture:free"]);
});

test("recommendations reject wrong city, category, date, cancellation and unconfirmed specific time", () => {
  const values = [event(), event({ id: "fixture:city", city: "Boston" }), event({ id: "fixture:category", category: "sports" }), event({ id: "fixture:date", startAt: "2026-11-22T23:00:00Z" }), event({ id: "fixture:cancelled", status: "cancelled" }), event({ id: "fixture:date-only", startAt: null, startDate: "2026-11-21", dateText: "Nov 21, 2026" })];
  assert.equal(recommendCatalog(input, now, values).recommendations.length, 2);
  assert.deepEqual(recommendCatalog({ ...input, timePreference: "evening" }, now, values).recommendations.map(item => item.event.id), ["fixture:music"]);
  assert.deepEqual(recommendCatalog({ ...input, timePreference: "morning" }, now, values).recommendations, []);
});

test("recommendations honor recurring schedules and catalog date exceptions", () => {
  const recurring = event({ startAt: null, isRecurring: true, recurrenceDays: [6], excludedDates: ["2026-11-21"] });
  assert.deepEqual(recommendCatalog(input, now, [recurring]).recommendations, []);
  assert.equal(recommendCatalog({ ...input, startDate: "2026-11-28", endDate: "2026-11-28" }, now, [recurring]).recommendations.length, 1);
  const selective = event({ startAt: null, occurrenceDates: ["2026-11-22"] });
  assert.deepEqual(recommendCatalog(input, now, [selective]).recommendations, []);
});

test("real catalog recommendations remain whitelisted and samples produce real results", () => {
  const parsed = parseAssistant({ message: "NYC, free games this Thursday" }, now);
  assert.ok(parsed.preferences);
  const result = recommendCatalog(parsed.preferences, now);
  assert.ok(result.recommendations.length > 0);
  assert.ok(result.recommendations.length <= 5);
  assert.ok(result.recommendations.every(item => CATALOG_EVENTS.some(event => event.id === item.event.id) && item.event.category === "gaming" && item.event.priceKnown && item.event.priceMin === 0));
  const unavailable = recommendCatalog({ ...parsed.preferences, interests: ["crypto"] }, now);
  assert.deepEqual(unavailable.recommendations, []);
  assert.ok(unavailable.suggestions?.length);
});

test("chat returns direct real-event recommendations for a complete message", () => {
  const response = chatAssistant({ message: "NYC, free games this Thursday" }, now);
  assert.deepEqual(response.clarifications, []);
  assert.deepEqual(response.pendingFields, []);
  assert.ok(response.result?.recommendations.length);
  assert.ok(response.result.recommendations.every(item => CATALOG_EVENTS.some(event => event.id === item.event.id)));
  assert.equal(response.preferences.budgetMax, 0);
  assert.equal(response.preferences.city, "New York");
});

test("chat independently collects city, interests and date over multiple turns", () => {
  const first = chatAssistant({ message: "New York" }, now);
  assert.equal(first.result, null);
  assert.equal(first.preferences.city, "New York");
  assert.deepEqual(first.pendingFields, ["interests", "dates"]);
  const second = chatAssistant({ message: "games", preferences: first.preferences, pendingFields: first.pendingFields }, now);
  assert.equal(second.result, null);
  assert.deepEqual(second.pendingFields, ["dates"]);
  const third = chatAssistant({ message: "next Thursday", preferences: second.preferences, pendingFields: second.pendingFields }, now);
  assert.ok(third.result?.recommendations.length);
  assert.equal(third.preferences.city, "New York");
  assert.deepEqual(third.preferences.interests, ["gaming"]);
  assert.equal(third.preferences.startDate, "2026-11-19");
  assert.equal(third.preferences.endDate, "2026-11-19");
  assert.equal(third.preferences.notes, "next Thursday");
});

test("chat updates constraints without discarding the rest of the conversation", () => {
  const original = chatAssistant({ message: "NYC games next Thursday, under $30" }, now);
  const free = chatAssistant({ message: "free only", preferences: original.preferences }, now);
  assert.ok(free.result?.recommendations.length);
  assert.equal(free.preferences.budgetMax, 0);
  assert.equal(free.preferences.city, "New York");
  assert.equal(free.preferences.startDate, "2026-11-19");
  const tomorrow = chatAssistant({ message: "tomorrow instead", preferences: free.preferences }, now);
  assert.equal(tomorrow.preferences.startDate, "2026-11-15");
  assert.equal(tomorrow.preferences.endDate, "2026-11-15");
  assert.equal(tomorrow.preferences.budgetMax, 0);
  assert.ok(tomorrow.result);
  assert.deepEqual(tomorrow.result.recommendations, []);
  const unlimited = chatAssistant({ message: "any budget", preferences: free.preferences }, now);
  assert.equal(Object.hasOwn(unlimited.preferences, "budgetMax"), false);
  assert.ok(unlimited.result?.recommendations.length);
  const morning = chatAssistant({ message: "morning", preferences: unlimited.preferences }, now);
  assert.equal(morning.preferences.timePreference, "morning");
  const anytime = chatAssistant({ message: "any time", preferences: morning.preferences }, now);
  assert.equal(anytime.preferences.timePreference, "any");
  assert.ok(anytime.result?.recommendations.length);
});

test("chat resumes saved context across New York midnight and accepts a fresh date", () => {
  const beforeMidnight = new Date("2026-11-19T04:55:00Z");
  const afterMidnight = new Date("2026-11-19T05:05:00Z");
  const saved = chatAssistant({ message: "NYC free games today" }, beforeMidnight);
  assert.equal(saved.preferences.startDate, "2026-11-18");
  const unchanged = chatAssistant({ message: "free only", preferences: saved.preferences }, afterMidnight);
  assert.equal(unchanged.result, null);
  assert.equal(unchanged.preferences.startDate, undefined);
  assert.equal(unchanged.preferences.endDate, undefined);
  assert.deepEqual(unchanged.pendingFields, ["dates"]);
  const tomorrow = chatAssistant({ message: "tomorrow instead", preferences: saved.preferences }, afterMidnight);
  assert.equal(tomorrow.preferences.startDate, "2026-11-20");
  assert.equal(tomorrow.preferences.endDate, "2026-11-20");
  assert.ok(tomorrow.result);
  assert.deepEqual(tomorrow.pendingFields, []);
  const today = chatAssistant({ message: "today instead", preferences: saved.preferences }, afterMidnight);
  assert.equal(today.preferences.startDate, "2026-11-19");
  assert.ok(today.result?.recommendations.length);
});

test("chat preserves unresolved ambiguity across turns until an explicit answer", () => {
  const preferences = chatAssistant({ message: "NYC games next Thursday, under $30" }, now).preferences;
  const ambiguous = chatAssistant({ message: "under $20 or $50", preferences }, now);
  assert.equal(ambiguous.result, null);
  assert.equal(Object.hasOwn(ambiguous.preferences, "budgetMax"), false);
  assert.deepEqual(ambiguous.pendingFields, ["budgetMax"]);
  const stillPending = chatAssistant({ message: "games", preferences: ambiguous.preferences, pendingFields: ambiguous.pendingFields }, now);
  assert.equal(stillPending.result, null);
  assert.deepEqual(stillPending.pendingFields, ["budgetMax"]);
  const fixed = chatAssistant({ message: "any budget", preferences: stillPending.preferences, pendingFields: stillPending.pendingFields }, now);
  assert.ok(fixed.result?.recommendations.length);
  assert.deepEqual(fixed.pendingFields, []);
});

test("chat clears stale city, date and time when a replacement is ambiguous", () => {
  const preferences = chatAssistant({ message: "NYC games next Thursday" }, now).preferences;
  for (const message of ["New York or Boston", "London instead", "Paris", "city: Atlantis"]) {
    const response = chatAssistant({ message, preferences }, now);
    assert.equal(response.result, null, message);
    assert.equal(response.preferences.city, undefined, message);
    assert.ok(response.pendingFields.includes("city"), message);
  }
  for (const message of ["today or tomorrow", "2026-11-13", "next month", "2026-02-30"]) {
    const response = chatAssistant({ message, preferences }, now);
    assert.equal(response.result, null, message);
    assert.equal(response.preferences.startDate, undefined, message);
    assert.equal(response.preferences.endDate, undefined, message);
    assert.ok(response.pendingFields.includes("dates"), message);
  }
  const time = chatAssistant({ message: "morning or evening", preferences }, now);
  assert.equal(time.preferences.timePreference, undefined);
  assert.equal(time.result, null);
  const answered = chatAssistant({ message: "any time", preferences: time.preferences, pendingFields: time.pendingFields }, now);
  assert.equal(answered.preferences.timePreference, "any");
  assert.ok(answered.result?.recommendations.length);
});

test("chat accepts follow-ups and keeps notes bounded to the last message", () => {
  const first = chatAssistant({ message: "NYC, games this Thursday" }, now);
  assert.ok(first.result?.recommendations.length);
  const second = chatAssistant({ message: "free", preferences: first.preferences }, now);
  assert.equal(second.preferences.budgetMax, 0);
  assert.ok(second.result?.recommendations.length);
  const third = chatAssistant({ message: "any budget", preferences: second.preferences }, now);
  assert.equal(Object.hasOwn(third.preferences, "budgetMax"), false);
  assert.equal(third.preferences.notes, "any budget");
  assert.ok(third.result?.recommendations.length);
});

test("chat validates untrusted partial context, pending fields and conversation IDs", () => {
  for (const preferences of [null, [], { city: "London" }, { city: null }, { interests: ["invented"] }, { interests: "music" }, { startDate: "2026-02-30" }, { startDate: "2026-11-20", endDate: "2026-11-19" }, { startDate: "2026-11-20", endDate: "2028-11-19" }, { budgetMax: -1 }, { budgetMax: NaN }, { budgetMax: null }, { timePreference: "night" }, { notes: "x".repeat(501) }, { injected: true }]) {
    assert.throws(() => chatAssistant({ message: "games", preferences }, now), JSON.stringify(preferences));
  }
  for (const pendingFields of [null, "city", ["invented"], ["city", "city", "city", "city", "city", "city"]]) assert.throws(() => chatAssistant({ message: "games", pendingFields }, now));
  for (const conversationId of [null, 123, "invented"]) assert.throws(() => chatAssistant({ message: "games", conversationId }, now));
  const conversationId = "970e7a9b-38de-44fb-944a-d597d95e43d1";
  const response = chatAssistant({ message: "games", conversationId }, now);
  assert.equal(response.conversationId, conversationId);
  assert.deepEqual(validateAssistantContext({ city: "New York", interests: [] }), { city: "New York", interests: [] });
});

test("unrecognized chat turns ask a clarification instead of replaying stale recommendations", () => {
  const preferences = chatAssistant({ message: "NYC games Thursday" }, now).preferences;
  const response = chatAssistant({ message: "hello again", preferences }, now);
  assert.equal(response.result, null);
  assert.ok(response.clarifications.length);
  const replay = chatAssistant({ message: "show events", preferences }, now);
  assert.ok(replay.result?.recommendations.length);
});

test("chat API completes a free conversation and refines it without consuming a second weekly trial", async t => {
  const directory = await mkdtemp(join(tmpdir(), "outly-assistant-chat-"));
  const previousDirectory = process.env.OUTLY_DEMO_DIR;
  process.env.OUTLY_DEMO_DIR = directory;
  t.after(async () => {
    if (previousDirectory === undefined) delete process.env.OUTLY_DEMO_DIR;
    else process.env.OUTLY_DEMO_DIR = previousDirectory;
    await rm(directory, { recursive: true, force: true });
  });
  async function token() {
    const response = await demoRoute(new Request("http://localhost/api/demo/session", { method: "POST", body: "{}" }), { params: Promise.resolve({ path: ["session"] }) });
    assert.equal(response.status, 200);
    return (await response.json()).token as string;
  }
  async function send(body: unknown, bearer?: string) {
    const response = await chatRoute(new Request("http://localhost/api/ai/chat", { method: "POST", headers: { "Content-Type": "application/json", "x-forwarded-for": "chat-api-test", ...(bearer ? { authorization: `Bearer ${bearer}` } : {}) }, body: JSON.stringify(body) }));
    return { status: response.status, body: await response.json() };
  }
  const guest = await token();
  const partial = await send({ message: "New York" }, guest);
  assert.equal(partial.status, 200);
  assert.equal(partial.body.result, null);
  assert.equal(partial.body.conversationId, undefined);
  const first = await send({ message: "free games Thursday", preferences: partial.body.preferences, pendingFields: partial.body.pendingFields }, guest);
  assert.equal(first.status, 200);
  assert.ok(first.body.result.recommendations.length);
  assert.ok(first.body.conversationId);
  const second = await send({ message: "any budget", preferences: first.body.preferences, pendingFields: first.body.pendingFields, conversationId: first.body.conversationId }, guest);
  assert.equal(second.status, 200);
  assert.equal(second.body.conversationId, first.body.conversationId);
  assert.ok(second.body.result.recommendations.length);
  const newConversation = await send({ message: "NYC games Thursday" }, guest);
  assert.equal(newConversation.status, 429);
  assert.equal(newConversation.body.error.code, "ASSISTANT_LIMIT");
  const stranger = await send({ message: "NYC games Thursday", conversationId: first.body.conversationId }, await token());
  assert.equal(stranger.status, 404);
  const malformed = await send({ message: "games", preferences: { city: "Atlantis" } }, guest);
  assert.equal(malformed.status, 400);
});
