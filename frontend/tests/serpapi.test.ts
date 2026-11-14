import assert from "node:assert/strict";
import test, { afterEach, beforeEach } from "node:test";
import { GET as eventsRoute } from "../src/app/api/events/route.ts";
import { GET as detailsRoute } from "../src/app/api/events/[id]/route.ts";
import { GET as healthRoute } from "../src/app/api/health/route.ts";
import { POST as planRoute } from "../src/app/api/plan/route.ts";
import { isPlannerResponse } from "../src/lib/api.ts";

const secret = "server-only-fixture-key";
const previous = { API_KEY: process.env.API_KEY, SERPAPI_KEY: process.env.SERPAPI_KEY };
beforeEach(() => {
  process.env.API_KEY = secret;
  delete process.env.SERPAPI_KEY;
});
afterEach(() => {
  for (const [key, value] of Object.entries(previous)) {
    if (value === undefined) delete process.env[key]; else process.env[key] = value;
  }
});

const raw = {
  title: "Source concert", date: "Nov 21", time: "7:00 PM", address: ["Source venue", "New York, NY"],
  thumbnail: "https://example.org/event.jpg", event_token: "fixture-event-token",
};
const request = (query: string) => new Request(`http://localhost/api/events?source=live&city=New+York&${query}`);
const plan = (value: unknown) => planRoute(new Request("http://localhost/api/plan", { method: "POST", body: JSON.stringify(value) }));

test("health and invalid filters never spend provider requests", async t => {
  t.mock.method(globalThis, "fetch", async () => { throw new Error("Unexpected provider request"); });
  assert.deepEqual(await (await healthRoute()).json(), { ok: true });
  for (const query of ["city=Chicago", "category=unknown", "date=2099-02-30", "date=2099-13-01", "page=0", "page=1.5", "keyword=" + "x".repeat(101), "major=maybe", "state=NJ"]) {
    const response = await eventsRoute(new Request(`http://localhost/api/events?source=live&${query}`));
    assert.equal(response.status, 400, query);
    assert.equal((await response.json()).error.code, "VALIDATION_ERROR");
  }
  assert.deepEqual((await (await eventsRoute(request("major=true"))).json()).events, []);
});

test("server key, raw source dates, nullable links/prices and stable IDs survive normalization", async t => {
  let calls = 0;
  t.mock.method(globalThis, "fetch", async (url: URL, init: RequestInit) => {
    calls++;
    const query = new URL(url).searchParams;
    assert.equal(query.get("api_key"), secret);
    assert.equal(query.get("engine"), "google");
    assert.match(query.get("q")!, /concerts in New York.*normalization/);
    assert.equal(init.cache, "no-store");
    return Response.json({ events_results: [raw, raw, { title: null }], search_parameters: { api_key: secret } });
  });
  const response = await eventsRoute(request("category=music&keyword=normalization"));
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.equal(body.events.length, 1);
  const event = body.events[0];
  assert.equal(event.dateText, "Nov 21 · 7:00 PM");
  assert.equal(event.startAt, null);
  assert.equal(event.priceKnown, false);
  assert.equal(event.priceMin, null);
  assert.equal(event.sourceUrl, null);
  assert.equal(event.venue, "Source venue");
  assert.equal(event.isMajor, false);
  assert.ok(!JSON.stringify(body).includes(secret));
  assert.ok(!JSON.stringify(body).includes(raw.event_token));
  assert.equal((await (await eventsRoute(request("category=music&keyword=normalization"))).json()).events[0].id, event.id);
  assert.equal(calls, 1);
});

test("same concurrent query and subsequent pages share the 30 minute provider cache", async t => {
  let calls = 0;
  t.mock.method(globalThis, "fetch", async () => {
    calls++;
    return Response.json({ events_results: Array.from({ length: 12 }, (_, index) => ({ ...raw, title: `Event ${index}` })) });
  });
  const results = await Promise.all([eventsRoute(request("keyword=cache")), eventsRoute(request("keyword=cache"))]);
  const first = await results[0].json();
  assert.equal(first.events.length, 10);
  assert.equal(first.pagination.hasMore, true);
  assert.equal((await results[1].json()).events.length, 10);
  const second = await (await eventsRoute(request("keyword=cache&page=2"))).json();
  assert.equal(second.events.length, 2);
  assert.equal(second.pagination.hasMore, false);
  assert.equal(calls, 1);
  const originalNow = Date.now();
  t.mock.method(Date, "now", () => originalNow + 31 * 60_000);
  await eventsRoute(request("keyword=cache"));
  assert.equal(calls, 2);
});

test("empty results are valid; failed providers and network errors are sanitized and retriable", async t => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ organic_results: [] }));
  assert.deepEqual((await (await eventsRoute(request("keyword=empty"))).json()).events, []);
  for (const [label, providerResponse, status] of [
    ["invalid-key", () => Response.json({ error: secret }, { status: 401 }), 502],
    ["quota", () => Response.json({ error: secret }, { status: 429 }), 429],
    ["bad-events", () => Response.json({ events_results: {} }), 502],
    ["bad-json", () => new Response("not-json"), 502],
    ["network", () => { throw new Error(secret); }, 502],
  ] as const) {
    t.mock.method(globalThis, "fetch", async () => providerResponse());
    const response = await eventsRoute(request(`keyword=${label}`));
    assert.equal(response.status, status);
    assert.ok(!(await response.text()).includes(secret));
  }
  t.mock.method(globalThis, "fetch", async () => Response.json({ events_results: [raw] }));
  assert.equal((await eventsRoute(request("keyword=network"))).status, 200);
});

test("missing key produces a service error without contacting SerpApi", async t => {
  delete process.env.API_KEY;
  t.mock.method(globalThis, "fetch", async () => { throw new Error("Unexpected request"); });
  const response = await eventsRoute(request("keyword=missing-key"));
  assert.equal(response.status, 503);
  assert.equal((await response.json()).error.code, "SERVICE_NOT_CONFIGURED");
});

test("details use the server-held token and expose the original HTTPS event link", async t => {
  let detailCalls = 0;
  t.mock.method(globalThis, "fetch", async (url: URL) => {
    const query = new URL(url).searchParams;
    if (query.get("engine") === "google") return Response.json({ events_results: [raw] });
    detailCalls++;
    assert.equal(query.get("event_token"), raw.event_token);
    return Response.json({ event_details: {
      title: raw.title, date: { start_date: "Nov 21", when: "Sat, Nov 21, 2099, 7:00 PM" },
      description: "Original source description", price: "From $25", extracted_price: 25,
      ticket_info: [{ link: "javascript:alert(1)" }, { link: "https://example.org/tickets" }],
    } });
  });
  const event = (await (await eventsRoute(request("keyword=details"))).json()).events[0];
  const response = await detailsRoute(new Request("http://localhost"), { params: Promise.resolve({ id: event.id }) });
  const detailed = (await response.json()).event;
  assert.equal(response.status, 200);
  assert.equal(detailed.id, event.id);
  assert.equal(detailed.sourceUrl, "https://example.org/tickets");
  assert.equal(detailed.priceKnown, true);
  assert.equal(detailed.priceMin, 25);
  assert.equal(detailed.priceMax, null);
  assert.equal(detailed.startAt, null);
  assert.equal(detailed.dateText, "Sat, Nov 21, 2099, 7:00 PM");
  await detailsRoute(new Request("http://localhost"), { params: Promise.resolve({ id: event.id }) });
  assert.equal(detailCalls, 1);
});

test("malformed or unsupported event IDs return 404 without spending quota", async t => {
  t.mock.method(globalThis, "fetch", async () => { throw new Error("Unexpected request"); });
  for (const id of ["ticketmaster:old", "serpapi:garbage", "serpapi:" + Buffer.from(JSON.stringify(["Chicago", "IL", "", "", "", "a".repeat(24)])).toString("base64url")]) {
    assert.equal((await detailsRoute(new Request("http://localhost"), { params: Promise.resolve({ id }) })).status, 404);
  }
});

const payload = { city: "New York", interests: ["food"], startDate: "2099-11-21", endDate: "2099-11-22", timePreference: "evening", budgetMax: 0 };
test("planner verifies inclusive source dates, zero budget and local time; unknown data is excluded", async t => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ events_results: [
    { title: "Start date", date: "Nov 21, 2099", time: "7:00 PM", price: "Free" },
    { title: "End date", date: "Nov 22, 2099", time: "5:00 PM", price: "Free" },
    { title: "Year missing", date: "Nov 21", time: "7:00 PM", price: "Free" },
    { title: "Price missing", date: "Nov 21, 2099", time: "7:00 PM" },
    { title: "Outside range", date: "Nov 20, 2099", time: "7:00 PM", price: "Free" },
  ] }));
  const response = await plan({ ...payload, notes: "Quiet please" });
  assert.equal(response.status, 200);
  const body = await response.json();
  assert.ok(isPlannerResponse(body));
  assert.deepEqual(body.recommendations.map((item: { event: { title: string } }) => item.event.title), ["Start date", "End date"]);
  assert.equal(body.suggestions?.length, 2);
});

test("planner validates direct callers, malformed JSON and oversized bodies before searching", async t => {
  t.mock.method(globalThis, "fetch", async () => { throw new Error("Unexpected request"); });
  for (const value of [null, { ...payload, city: "Chicago" }, { ...payload, interests: [] }, { ...payload, interests: ["unknown"] },
    { ...payload, startDate: "2099-02-30" }, { ...payload, endDate: "2099-11-20" }, { ...payload, budgetMax: -1 },
    { ...payload, budgetMax: "0" }, { ...payload, timePreference: "midnight" }, { ...payload, notes: "x".repeat(501) }]) {
    assert.equal((await plan(value)).status, 400);
  }
  assert.equal((await planRoute(new Request("http://localhost/api/plan", { method: "POST", body: "{" }))).status, 400);
  assert.equal((await planRoute(new Request("http://localhost/api/plan", { method: "POST", body: "x".repeat(10_001) }))).status, 413);
});

test("planner does not infer a start time from an ambiguous time range", async t => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ events_results: [
    { title: "Explicit evening", date: "Nov 21, 2099", time: "7:00 PM", price: "Free" },
    { title: "Ambiguous evening", date: "Nov 21, 2099", time: "7 – 10 PM", price: "Free" },
    { title: "Explicit morning", date: "Nov 21, 2099", time: "10:00 AM", price: "Free" },
  ] }));
  const body = await (await plan({ ...payload, interests: ["sports"] })).json();
  assert.deepEqual(body.recommendations.map((item: { event: { title: string } }) => item.event.title), ["Explicit evening"]);
});

test("catalog date filters use Eastern dates for ISO timestamps and never invent a missing year", async t => {
  t.mock.method(globalThis, "fetch", async () => Response.json({ events_results: [
    { title: "Eastern evening", startAt: "2099-11-22T02:00:00Z" },
    { title: "Year missing", date: "Nov 21" },
    { title: "Other date", date: "Nov 22, 2099" },
  ] }));
  const body = await (await eventsRoute(request("category=wellness&date=2099-11-21"))).json();
  assert.deepEqual(body.events.map((event: { title: string }) => event.title), ["Eastern evening"]);
});

test("provider quota guard rejects the 31st uncached search in one minute", async t => {
  t.mock.method(Date, "now", () => new Date("2099-01-01T00:00:00Z").getTime());
  let calls = 0;
  t.mock.method(globalThis, "fetch", async () => { calls++; return Response.json({ events_results: [] }); });
  for (let index = 0; index < 30; index++) assert.equal((await eventsRoute(request(`keyword=guard${index}`))).status, 200);
  assert.equal((await eventsRoute(request("keyword=guard-over-limit"))).status, 429);
  assert.equal(calls, 30);
});
