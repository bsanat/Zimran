import assert from "node:assert/strict";
import test from "node:test";

test("demo client keeps independent tab credentials and never replays failed mutations", async t => {
  const originalStorage = Object.getOwnPropertyDescriptor(globalThis, "sessionStorage");
  const saved = new Map<string, string>();
  Object.defineProperty(globalThis, "sessionStorage", { configurable: true, value: { getItem: (key: string) => saved.get(key) || null, setItem: (key: string, value: string) => saved.set(key, value), removeItem: (key: string) => saved.delete(key) } });
  t.after(() => { if (originalStorage) Object.defineProperty(globalThis, "sessionStorage", originalStorage); else Reflect.deleteProperty(globalThis, "sessionStorage"); });
  const token = "a".repeat(64);
  const calls: { url: string; options: RequestInit | undefined }[] = [];
  const fetchMock = t.mock.method(globalThis, "fetch", async (url: string, options?: RequestInit) => {
    calls.push({ url, options });
    return Response.json(url.endsWith("/session") ? { token } : { status: "FREE", price: 9.99, currency: "USD" });
  });
  const client = await import(`../src/lib/demo-api.ts?test=${Date.now()}`);
  const tokens = await Promise.all([client.getDemoToken(), client.getDemoToken()]);
  assert.deepEqual(tokens, [token, token]);
  assert.equal(calls.length, 1, "concurrent bootstraps share one identity");
  await client.demoRequest("subscription");
  assert.equal(new Headers(calls[1].options?.headers).get("Authorization"), `Bearer ${token}`);
  assert.equal(calls[1].options?.cache, "no-store");
  assert.equal(client.isSubscription({ status: "DEMO_PREMIUM", price: 9.99, currency: "USD" }), true);
  assert.equal(client.isSubscription({ status: "PREMIUM_ACTIVE", price: 9.99, currency: "USD" }), false);
  fetchMock.mock.mockImplementation(async () => { calls.push({ url: "failed", options: undefined }); return Response.json({ error: { message: "Expired", code: "SESSION_REQUIRED" } }, { status: 401 }); });
  await assert.rejects(client.demoRequest("buddy/posts", { method: "POST", body: "{}" }), (error: unknown) => error instanceof Error && error.message === "Expired");
  assert.equal(calls.length, 3, "a POST is never repeated automatically");
  assert.equal(saved.size, 0, "expired credentials are removed");
  fetchMock.mock.mockImplementation(async () => Response.json({ token: "not-a-valid-token" }));
  await assert.rejects(client.getDemoToken(), /incomplete response/);
});
