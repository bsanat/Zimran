import assert from "node:assert/strict";
import test, { after } from "node:test";
import { getRewrittenUrl, unstable_getResponseFromNextConfig } from "next/experimental/testing/server.js";
import nextConfig from "../next.config.ts";

const previousOrigin = process.env.API_PROXY_ORIGIN;
after(() => {
  if (previousOrigin === undefined) delete process.env.API_PROXY_ORIGIN;
  else process.env.API_PROXY_ORIGIN = previousOrigin;
});

test("backend proxy preserves collection paths, nested paths and query filters", async () => {
  process.env.API_PROXY_ORIGIN = "https://backend.example/";
  const rewrites = await nextConfig.rewrites!();
  assert.ok(!Array.isArray(rewrites));
  for (const collection of ["events", "notifications", "blocks"]) {
    assert.equal(rewrites.beforeFiles?.find(rule => rule.source === `/api/${collection}`)?.destination, `https://backend.example/api/${collection}`);
  }
  for (const path of ["events?city=New+York&page=1", "events/catalog%3Aevent", "notifications?size=50", "notifications/123/read", "blocks", "blocks/123", "plan"]) {
    const response = await unstable_getResponseFromNextConfig({ url: `https://frontend.example/api/${path}`, nextConfig });
    assert.equal(getRewrittenUrl(response), `https://backend.example/api/${path}`);
  }
});

test("local routes remain available without a backend or for unsupported demo APIs", async () => {
  process.env.API_PROXY_ORIGIN = "https://backend.example";
  let response = await unstable_getResponseFromNextConfig({ url: "https://frontend.example/api/demo/session", nextConfig });
  assert.equal(getRewrittenUrl(response), null);
  delete process.env.API_PROXY_ORIGIN;
  response = await unstable_getResponseFromNextConfig({ url: "https://frontend.example/api/events", nextConfig });
  assert.equal(getRewrittenUrl(response), null);
});
