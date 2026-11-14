import assert from "node:assert/strict";
import test from "node:test";
import { ApiError, generatePlan, getEvent, getEvents } from "../src/lib/api.ts";
import type { EventData } from "../src/types/event.ts";

// Test fixtures never appear in the application catalog.
const event: EventData = {
  id: "ticketmaster:fixture", source: "ticketmaster", sourceEventId: "fixture", title: "Test event",
  category: "music", city: "New York", startAt: "2026-11-21T23:00:00Z", timezone: "America/New_York",
  venue: null, address: null, description: null, imageUrl: null, priceMin: null, priceMax: null,
  priceKnown: false, currency: "USD", sourceUrl: "https://www.ticketmaster.com/event/fixture", status: "scheduled",
};

const payload = {
  city: "New York", interests: ["music"] as ["music"], startDate: "2026-11-21", endDate: "2026-11-22", timePreference: "any" as const,
};

test("API preserves query filters and encodes source IDs", async (t) => {
  let calledUrl = "";
  t.mock.method(globalThis, "fetch", async (url: string) => {
    calledUrl = url;
    return Response.json({ events: [event], pagination: { page: 2, pageSize: 20, hasMore: false } });
  });
  const result = await getEvents({ city: "New York", state: "NY", date: "2026-11-21", category: "music", page: 2, major: true, keyword: "live jazz" });
  assert.equal(result.events[0].priceKnown, false);
  const query = new URL(calledUrl, "https://local.test").searchParams;
  assert.equal(query.get("city"), "New York");
  assert.equal(query.get("date"), "2026-11-21");
  assert.equal(query.get("category"), "music");
  assert.equal(query.get("page"), "2");
  assert.equal(query.get("state"), "NY");
  assert.equal(query.get("major"), "true");
  assert.equal(query.get("keyword"), "live jazz");
  t.mock.method(globalThis, "fetch", async (url: string) => { calledUrl = url; return Response.json({ event }); });
  await getEvent("ticketmaster:fixture");
  assert.match(calledUrl, /\/api\/events\/ticketmaster%3Afixture$/);
});

test("POST sends the exact planner JSON and accepts a verified empty result", async (t) => {
  t.mock.method(globalThis, "fetch", async (_url: string, init: RequestInit) => {
    assert.equal(init.method, "POST");
    assert.deepEqual(JSON.parse(init.body as string), payload);
    return Response.json({ planId: "empty", summary: "No matches", recommendations: [] });
  });
  assert.deepEqual((await generatePlan(payload)).recommendations, []);
});

test("API exposes inline validation fields and rate limit/service errors", async (t) => {
  t.mock.method(globalThis, "fetch", async () => Response.json({
    error: { code: "VALIDATION_ERROR", message: "Choose at least one interest.", details: { field: "interests" } },
  }, { status: 400 }));
  await assert.rejects(() => generatePlan(payload), (error: unknown) => {
    assert.ok(error instanceof ApiError);
    assert.equal(error.status, 400);
    assert.equal(error.code, "VALIDATION_ERROR");
    assert.equal(error.field, "interests");
    return true;
  });
  for (const status of [404, 429, 502, 503]) {
    t.mock.method(globalThis, "fetch", async () => new Response("upstream error", { status }));
    await assert.rejects(() => getEvent(event.id), (error: unknown) => error instanceof ApiError && error.status === status && error.message.length > 0);
  }
});

test("incomplete event data and more than five recommendations are rejected", async (t) => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ event: { id: "missing-fields" } }));
  await assert.rejects(() => getEvent(event.id), (error: unknown) => error instanceof ApiError && error.code === "INVALID_RESPONSE");
  const recommendation = { event, matchScore: 92, reason: "Matches music" };
  for (const recommendations of [
    Array(6).fill(recommendation),
    [recommendation, recommendation],
    [{ ...recommendation, event: { ...event, status: "cancelled" } }],
    [{ ...recommendation, matchScore: 101 }],
  ]) {
    t.mock.method(globalThis, "fetch", async () => Response.json({ planId: "invalid", summary: "Invalid", recommendations }));
    await assert.rejects(() => generatePlan(payload), (error: unknown) => error instanceof ApiError && error.code === "INVALID_RESPONSE");
  }
});

test("caller cancellation remains an AbortError", async (t) => {
  t.mock.method(globalThis, "fetch", async (_url: string, init: RequestInit) => {
    init.signal?.throwIfAborted();
    throw new Error("Expected an aborted request");
  });
  const controller = new AbortController();
  controller.abort();
  await assert.rejects(() => getEvent(event.id, controller.signal), (error: unknown) => error instanceof DOMException && error.name === "AbortError");
});

test("major event metadata accepts booleans and rejects ambiguous values", async (t) => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ event: { ...event, isMajor: "true" } }));
  await assert.rejects(() => getEvent(event.id), (error: unknown) => error instanceof ApiError && error.code === "INVALID_RESPONSE");
  t.mock.method(globalThis, "fetch", async () => Response.json({ event: { ...event, isMajor: true } }));
  assert.equal((await getEvent(event.id)).event.isMajor, true);
});

test("SerpApi events preserve unconfirmed dates, prices and missing source links", async t => {
  const sourced = { ...event, source: "serpapi", startAt: null, dateText: "Nov 21 · 7:00 PM", priceText: "From $30", sourceUrl: null };
  t.mock.method(globalThis, "fetch", async () => Response.json({ event: sourced }));
  assert.deepEqual((await getEvent(event.id)).event, sourced);
  t.mock.method(globalThis, "fetch", async () => Response.json({ event: { ...sourced, dateText: { invalid: true } } }));
  await assert.rejects(() => getEvent(event.id), (error: unknown) => error instanceof ApiError && error.code === "INVALID_RESPONSE");
});
