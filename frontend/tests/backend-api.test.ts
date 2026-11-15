import assert from "node:assert/strict";
import test from "node:test";

test("backend client issues one guest token, retries a 401 once and maps the Spring contract", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });

  const tokens = ["aaa.bbb.ccc", "ddd.eee.fff"];
  let issued = 0;
  const calls: { url: string; init?: RequestInit }[] = [];
  let rejectNext = false;
  let omitPlan = false;
  t.mock.method(globalThis, "fetch", async (url: string, init?: RequestInit) => {
    calls.push({ url, init });
    if (url.endsWith("/api/guest/session")) return Response.json({ guestToken: tokens[issued++], expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
    if (rejectNext) { rejectNext = false; return Response.json({ error: { code: "UNAUTHORIZED", message: "Expired" } }, { status: 401 }); }
    if (url.endsWith("/api/alerts/preferences") && init?.method === "PUT") {
      const body = JSON.parse(String(init.body));
      return Response.json({ saved: true, preferences: { ...body, paused: body.paused, enabledAt: null } });
    }
    if (url.endsWith("/api/alerts/preferences")) return Response.json({ configured: false, premiumRequired: true, preferences: { city: "New York", categorySlugs: ["music"], frequency: "INSTANT", channels: ["in_app"], paused: false } });
    if (url.includes("/api/notifications?")) return Response.json({ unreadCount: 1, items: [{ id: "n1", eventId: "json:1", title: "Jazz", categorySlug: "food_drinks", createdAt: "2026-11-14T10:00:00Z", readAt: null }], pagination: { page: 1, pageSize: 50, hasMore: false } });
    if (url.endsWith("/read")) return new Response(null, { status: 204 });
    if (url.endsWith("/api/subscription")) return Response.json({ status: "PREMIUM_ACTIVE", isPremium: true, demoOnly: false, plan: omitPlan ? undefined : { priceCents: 499, currency: "USD" } });
    return Response.json({ error: { message: "Unexpected" } }, { status: 500 });
  });
  const client = await import(`../src/lib/backend-api.ts?test=${Date.now()}`);

  assert.equal(await client.getAlertPreferences(), null, "unconfigured defaults are not treated as saved alerts");
  assert.equal(calls.filter(call => call.url.endsWith("/guest/session")).length, 1);
  assert.equal(new Headers(calls[1].init?.headers).get("X-Guest-Token"), tokens[0]);

  const savedPreferences = await client.saveAlertPreferences({ city: "New York", categorySlugs: ["food_drinks", "tech"], frequency: "daily", channels: ["in_app"], paused: false, budgetMax: 25 });
  const sent = JSON.parse(String(calls.at(-1)!.init?.body));
  assert.deepEqual(sent.categorySlugs, ["food_drinks", "tech"], "backend category ids are sent as they are");
  assert.equal(sent.frequency, "DAILY");
  assert.deepEqual(savedPreferences.categorySlugs, ["food_drinks", "tech"]);
  assert.equal(savedPreferences.frequency, "daily");

  const feed = await client.getNotifications();
  assert.equal(feed.items[0].categorySlug, "food_drinks");
  await client.markNotificationRead("n1");

  const subscription = await client.getSubscription();
  assert.deepEqual(subscription, { status: "DEMO_PREMIUM", price: 4.99, currency: "USD" });
  omitPlan = true;
  assert.deepEqual(await client.getSubscription(), { status: "DEMO_PREMIUM", price: 4.99, currency: "USD" });

  rejectNext = true;
  await client.getSubscription();
  assert.equal(issued, 2, "a rejected token is replaced once");
  assert.equal(new Headers(calls.at(-1)!.init?.headers).get("X-Guest-Token"), tokens[1]);
});

test("pair client keeps the participant token, shows catalog events and votes with backend ids", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });

  const { CATALOG_EVENTS } = await import("../src/lib/catalog.ts");
  const local = CATALOG_EVENTS[0];
  // What Spring returns for the same catalog entry (served from frontend/src/data/catalog.json).
  const backendEvent = { ...local, id: `json:${local.sourceEventId}`, source: "json" };
  const me = { role: "A", interests: ["music"], dateFrom: "2026-11-20", dateTo: "2026-11-30", timePreference: "evening", budgetMax: 40, message: "Something calm" };
  const partner = { role: "B", interests: ["art"], dateFrom: "2026-11-21", dateTo: "2026-11-29", timePreference: "any", budgetMax: null, message: null };
  let state: "waiting" | "ready" | "revoked" = "waiting";
  const calls: { url: string; init?: RequestInit }[] = [];
  t.mock.method(globalThis, "fetch", async (url: string, init?: RequestInit) => {
    calls.push({ url, init });
    if (url.endsWith("/api/guest/session")) return Response.json({ guestToken: "aaa.bbb.ccc", expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
    if (url.endsWith("/api/pair/sessions")) return Response.json({ sessionId: "s1", inviteUrl: "http://localhost:3000/pair/join#invite=INV123", participantToken: "PT-A", status: "WAITING_FOR_B" });
    if (url.endsWith("/api/pair/s1/revoke")) { state = "revoked"; return new Response(null, { status: 204 }); }
    const vote = url.endsWith("/votes") ? JSON.parse(String(init?.body)).vote : null;
    if (url.endsWith("/votes")) state = "ready";
    return Response.json({
      sessionId: "s1", status: state === "waiting" ? "WAITING_FOR_B" : state === "revoked" ? "REVOKED" : "MATCHED",
      city: "nyc", cityName: "New York", expiresAt: "2026-11-16T10:00:00Z", myRole: "A", me,
      partner: state === "waiting" ? null : partner,
      suggestions: state === "waiting" ? [] : [{ rank: 1, event: backendEvent, reasonA: "Your music pick", reasonB: "Theirs", myVote: vote, partnerVote: "LIKE" }],
      sharedPlan: state === "ready" ? { event: backendEvent, reasonA: "Your music pick", reasonB: "Theirs" } : null,
      conflict: null,
    });
  });
  const client = await import(`../src/lib/backend-api.ts?pair=${Date.now()}`);

  const created = await client.createPairSession("New York", { interests: ["music"], startDate: "2026-11-20", endDate: "2026-11-30", timePreference: "evening", budgetMax: 40, notes: "Something calm" });
  assert.equal(created.inviteToken, "INV123");
  const createBody = JSON.parse(String(calls.find(call => call.url.endsWith("/pair/sessions"))!.init?.body));
  assert.deepEqual(createBody.participantA, { interests: ["music"], dateFrom: "2026-11-20", dateTo: "2026-11-30", timePreference: "evening", budgetMax: 40, message: "Something calm" });
  assert.equal(new Headers(calls.at(-1)!.init?.headers).get("X-Pair-Token"), "PT-A");
  assert.equal(created.session.status, "WAITING");
  assert.equal(created.session.participantB, null);
  assert.equal(created.session.participantA.notes, "Something calm");

  const voted = await client.votePair("s1", created.session.recommendations[0]?.voteId ?? `json:${local.sourceEventId}`, "LIKE");
  assert.equal(JSON.parse(String(calls.at(-1)!.init?.body)).eventId, `json:${local.sourceEventId}`, "votes use the backend id");
  assert.equal(voted.recommendations[0].event, local, "the frontend's own catalog event is shown");
  assert.equal(voted.recommendations[0].reason, "Your music pick");
  assert.equal(voted.votes.A[local.id], "LIKE");
  assert.equal(voted.votes.B[local.id], "LIKE");
  assert.equal(voted.sharedEventId, local.id);
  assert.equal(voted.status, "MATCHED");
  assert.equal(voted.participantB?.budgetMax, undefined);

  assert.equal((await client.revokePairSession("s1")).status, "CLOSED");
  saved.clear();
  await assert.rejects(client.getPairSession("s1"), (error: { code?: string }) => error.code === "PAIR_TOKEN_MISSING");
});

test("demo Premium is activated with POST and ended with DELETE on the same backend path", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });
  const calls: { url: string; method?: string }[] = [];
  t.mock.method(globalThis, "fetch", async (url: string, init?: RequestInit) => {
    calls.push({ url, method: init?.method });
    if (url.endsWith("/api/guest/session")) return Response.json({ guestToken: "aaa.bbb.ccc", expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
    const premium = init?.method === "POST";
    return Response.json({ status: premium ? "DEMO_PREMIUM" : "FREE", isPremium: premium, demoOnly: premium, plan: { priceCents: 499, currency: "USD" } });
  });
  const client = await import(`../src/lib/backend-api.ts?premium=${Date.now()}`);
  assert.equal((await client.activateDemoPremium()).status, "DEMO_PREMIUM");
  assert.equal((await client.deactivateDemoPremium()).status, "FREE");
  assert.deepEqual(calls.slice(1).map(call => `${call.method} ${call.url.replace(/^.*\/api/, "")}`), ["POST /demo/premium", "DELETE /demo/premium"]);
});

test("assistant chat sends the previous preferences, asks while fields are pending and shows catalog events", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });
  const { CATALOG_EVENTS } = await import("../src/lib/catalog.ts");
  const { isAssistantChatResponse } = await import("../src/lib/assistant-chat.ts");
  const local = CATALOG_EVENTS[0];
  const bodies: Record<string, unknown>[] = [];
  t.mock.method(globalThis, "fetch", async (url: string, init?: RequestInit) => {
    if (url.endsWith("/api/guest/session")) return Response.json({ guestToken: "aaa.bbb.ccc", expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
    const body = JSON.parse(String(init?.body));
    bodies.push(body);
    if (!body.preferences) {
      return Response.json({ engine: "ai", preferences: { city: "New York", interests: ["music"], startDate: null, endDate: null, budgetMax: null, timePreference: null, notes: null }, pendingFields: ["dates"], clarifications: ["Which day or dates do you have in mind?"], result: null, conversationId: null });
    }
    return Response.json({
      engine: "ai", preferences: { ...body.preferences, startDate: "2026-11-21", endDate: "2026-11-21", budgetMax: null, timePreference: "evening", notes: null },
      pendingFields: [], clarifications: [], conversationId: "3f2b8c1e-1d2a-4c3b-9a8e-123456789abc",
      result: { planId: "plan-1", summary: "Picked for you", relaxationSuggestions: [], recommendations: [{ eventId: `json:${local.sourceEventId}`, matchScore: 92, reason: "Live music on Saturday", event: { ...local, id: `json:${local.sourceEventId}`, source: "json" } }] },
    });
  });
  const client = await import(`../src/lib/backend-api.ts?chat=${Date.now()}`);

  const first = await client.assistantChat("Live music in New York", undefined);
  assert.equal(isAssistantChatResponse(first), true, "the screen's own check accepts the answer");
  assert.deepEqual(first.preferences, { city: "New York", interests: ["music"] }, "nulls are dropped");
  assert.deepEqual(first.pendingFields, ["dates"]);
  assert.equal(first.result, null);
  assert.equal(first.conversationId, undefined);
  assert.deepEqual(bodies[0], { message: "Live music in New York" });

  const second = await client.assistantChat("This Saturday evening", first);
  assert.deepEqual(bodies[1], { message: "This Saturday evening", preferences: { city: "New York", interests: ["music"] } });
  assert.equal(isAssistantChatResponse(second), true);
  assert.equal(second.result?.recommendations[0].event, local, "the frontend's catalog copy is shown");
  assert.equal(second.result?.recommendations[0].reason, "Live music on Saturday");
  assert.equal(second.preferences.timePreference, "evening");

  await client.assistantChat("Cheaper please", second);
  assert.equal(bodies[2].conversationId, "3f2b8c1e-1d2a-4c3b-9a8e-123456789abc", "later turns continue the conversation");
});

test("alert categories come from GET /api/meta, cached and retried after a failure", async t => {
  let calls = 0;
  let fail = true;
  t.mock.method(globalThis, "fetch", async (url: string) => {
    calls++;
    assert.ok(url.endsWith("/api/meta"));
    if (fail) { fail = false; return Response.json({ error: { code: "UPSTREAM_UNAVAILABLE", message: "Down" } }, { status: 503 }); }
    return Response.json({ categories: [
      { id: "tech", label: "Technology", interests: ["technology", "ai", "crypto"] },
      { id: "other", label: "Other", interests: ["gaming", "wellness", "unknown-interest"] },
    ] });
  });
  const client = await import(`../src/lib/backend-api.ts?meta=${Date.now()}`);
  await assert.rejects(client.getAlertCategories("en"), /Down/);
  const categories = await client.getAlertCategories("en");
  assert.deepEqual(categories.map((category: { id: string }) => category.id), ["tech", "other"]);
  assert.deepEqual(categories[1].interests, ["gaming", "wellness"], "unknown interests are dropped");
  assert.equal(client.categoryIcon(categories[0]), "technology");
  assert.equal(client.categoryIcon(undefined), "bell");
  await client.getAlertCategories("en");
  assert.equal(calls, 2, "one failed and one successful request; the second call is cached");
});

test("buddy merges the open feed with my posts and requests and maps catalog event ids", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });
  const { CATALOG_EVENTS } = await import("../src/lib/catalog.ts");
  const local = CATALOG_EVENTS[0];
  const backendId = `json:${local.sourceEventId}`;
  const post = (id: string, own: boolean, status = "OPEN", eventId = backendId) => ({ id, eventId, displayName: own ? "Me" : "Alex", shortBio: "Hi there", language: null, status, own, createdAt: "2026-11-15T10:00:00Z", expiresAt: "2026-11-30T18:00:00Z" });
  const calls: { url: string; body?: unknown; method?: string }[] = [];
  t.mock.method(globalThis, "fetch", async (url: string, init?: RequestInit) => {
    calls.push({ url, method: init?.method, body: init?.body ? JSON.parse(String(init.body)) : undefined });
    if (url.endsWith("/api/guest/session")) return Response.json({ guestToken: "aaa.bbb.ccc", expiresAt: new Date(Date.now() + 3_600_000).toISOString() });
    if (url.includes("/api/buddy/posts?")) return Response.json({ posts: [post("bud_other", false), post("bud_mine", true)] });
    if (url.endsWith("/api/buddy/me")) return Response.json({
      posts: [
        { post: post("bud_mine", true), requests: [{ id: "breq_in", displayName: "Sam", message: "Can I join?", status: "PENDING", createdAt: "2026-11-15T11:00:00Z", resolvedAt: null, safetyNotice: null }] },
        { post: post("bud_elsewhere", true, "OPEN", "json:another-event"), requests: [] },
      ],
      requests: [{ id: "breq_out", displayName: "Me", message: "Hello!", status: "ACCEPTED", createdAt: "2026-11-15T12:00:00Z", resolvedAt: "2026-11-15T13:00:00Z", safetyNotice: "Meet in a public place.", post: post("bud_matched", false, "MATCHED") }],
    });
    if (url.endsWith("/api/blocks")) return Response.json({ scopeRef: "blk_1", createdAt: "2026-11-15T14:00:00Z" });
    if (url.endsWith("/api/reports") || init?.method === "DELETE") return new Response(null, { status: 204 });
    return Response.json(post("bud_new", true));
  });
  const client = await import(`../src/lib/backend-api.ts?buddy=${Date.now()}`);

  const posts = await client.getBuddyPosts(local.id);
  assert.ok(calls.some(call => call.url.includes(`eventId=${encodeURIComponent(backendId)}`)), "the catalog id is sent as json:<id>");
  assert.deepEqual(posts.map((item: { id: string }) => item.id), ["bud_other", "bud_mine", "bud_matched"], "other events are left out");
  const mine = posts.find((item: { id: string }) => item.id === "bud_mine");
  assert.equal(mine.isOwner, true);
  assert.equal(mine.requests[0].isOwner, false, "incoming requests are not the caller's");
  assert.equal(mine.eventId, local.id);
  const matched = posts.find((item: { id: string }) => item.id === "bud_matched");
  assert.equal(matched.requests[0].isOwner, true, "a sent request belongs to the caller");
  assert.equal(matched.requests[0].safetyNotice, "Meet in a public place.");

  await client.createBuddyPost(local.id, { displayName: "Me", bio: "Hi there", language: "English", ageConfirmed: true });
  assert.deepEqual(calls.at(-1)!.body, { eventId: backendId, displayName: "Me", shortBio: "Hi there", language: "English", ageConfirmed: true });
  await client.decideBuddyRequest("breq_in", "ACCEPT");
  assert.deepEqual(calls.at(-1)!.body, { decision: "ACCEPT" });
  await client.reportBuddy("REQUEST", "breq_in", "SPAM");
  assert.deepEqual(calls.at(-1)!.body, { targetType: "REQUEST", targetId: "breq_in", reason: "SPAM" });
  assert.equal(await client.blockBuddy("POST", "bud_other"), "blk_1");
  await client.unblockBuddy("blk_1");
  assert.equal(`${calls.at(-1)!.method} ${calls.at(-1)!.url.replace(/^.*\/api/, "")}`, "DELETE /blocks/blk_1");
});
