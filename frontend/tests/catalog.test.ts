import assert from "node:assert/strict";
import { existsSync, readFileSync, statSync } from "node:fs";
import { fileURLToPath } from "node:url";
import test from "node:test";
import { CATALOG_EVENTS, catalogCategories, catalogMatchesDate, catalogMeta, catalogRegions, filterCatalogEvents, normalizeCategory } from "../src/lib/catalog.ts";
import { INTERESTS, safeExternalUrl } from "../src/lib/planner.ts";
import { US_STATES } from "../src/lib/locations.ts";
import { GET as eventsRoute } from "../src/app/api/events/route.ts";
import { GET as detailsRoute } from "../src/app/api/events/[id]/route.ts";
import type { EventData } from "../src/types/event.ts";

test("verified catalog covers all 18 interests, all 50 states and Washington DC", () => {
  assert.equal(catalogMeta.dataType, "VERIFIED_OFFICIAL_SOURCES");
  assert.deepEqual(catalogCategories.map(category => category.id).sort(), INTERESTS.map(category => category.id).sort());
  assert.deepEqual([...new Set(CATALOG_EVENTS.map(event => event.category))].sort(), INTERESTS.map(category => category.id).sort());
  assert.deepEqual([...new Set(CATALOG_EVENTS.map(event => event.state))].sort(), [...US_STATES.map(state => state.code), "DC"].sort());
  assert.equal(new Set(CATALOG_EVENTS.map(event => event.id)).size, CATALOG_EVENTS.length);
  assert.ok(CATALOG_EVENTS.length >= 51);
});

test("every verified listing has a working local image, an official HTTPS link and a review date", () => {
  for (const event of CATALOG_EVENTS) {
    assert.ok(event.title && event.description && event.city && event.state, event.id);
    assert.equal(event.source, "catalog");
    assert.equal(safeExternalUrl(event.sourceUrl, event.source), event.sourceUrl, event.id);
    assert.match(event.imageUrl, /^\/catalog\/[a-z0-9-]+\.(jpg|png|webp)$/);
    const image = fileURLToPath(new URL(`../public${event.imageUrl}`, import.meta.url));
    assert.ok(existsSync(image), event.id);
    assert.ok(statSync(image).size > 3000, event.id);
    const header = readFileSync(image).subarray(0, 12);
    assert.ok(header[0] === 0xff && header[1] === 0xd8 || header.subarray(0, 8).equals(Buffer.from([137, 80, 78, 71, 13, 10, 26, 10])) || header.toString("ascii", 0, 4) === "RIFF", event.id);
    assert.match(event.lastVerifiedAt, /^\d{4}-\d{2}-\d{2}$/);
    assert.equal(event.lastVerifiedAt, catalogMeta.lastVerifiedAt);
    assert.ok(event.startAt || event.dateText, event.id);
    assert.ok(catalogRegions.find(region => region.code === event.state)?.cities.includes(event.city));
    if (!event.priceKnown) assert.equal(event.priceMin, null);
    else assert.ok(event.priceMin !== null && event.priceMin >= 0 && event.currency);
  }
});

test("category aliases and compound geographic filters retain only matching catalog records", () => {
  assert.equal(normalizeCategory("games"), "gaming");
  assert.equal(normalizeCategory("food-drinks"), "food");
  assert.equal(normalizeCategory("arts-culture"), "art");
  for (const sample of CATALOG_EVENTS) {
    const filtered = filterCatalogEvents({ state: sample.state, city: sample.city, category: sample.category });
    if ((sample.endDate || sample.startDate || "9999") >= new Date().toISOString().slice(0, 10)) assert.ok(filtered.some(event => event.id === sample.id), sample.id);
    assert.ok(filtered.every(event => event.state === sample.state && event.city === sample.city && event.category === sample.category));
  }
});

test("exact date filters respect dark days, selected dates, seasonal recurrence and venue time", () => {
  const event = { startAt: null, timezone: "America/New_York", startDate: "2099-11-20", endDate: "2099-11-30" } as EventData;
  assert.equal(catalogMatchesDate(event, "2099-11-20"), true);
  assert.equal(catalogMatchesDate(event, "2099-11-30"), true);
  assert.equal(catalogMatchesDate(event, "2099-12-01"), false);
  assert.equal(catalogMatchesDate({ ...event, excludedDates: ["2099-11-21"] }, "2099-11-21"), false);
  assert.equal(catalogMatchesDate({ ...event, occurrenceDates: ["2099-11-22"] }, "2099-11-21"), false);
  assert.equal(catalogMatchesDate({ ...event, availableDateRanges: [["2099-11-25", "2099-11-27"]] }, "2099-11-24"), false);
  const recurring = { ...event, startDate: null, endDate: null, isRecurring: true, recurrenceDays: [6], recurrenceMonths: [11] };
  assert.equal(catalogMatchesDate(recurring, "2026-11-21"), true);
  assert.equal(catalogMatchesDate(recurring, "2026-12-05"), false);
  assert.equal(catalogMatchesDate({ ...recurring, recurrenceDays: undefined }, "2026-11-21"), false);
  assert.equal(catalogMatchesDate({ ...event, startDate: null, endDate: null, startAt: "2099-11-22T02:00:00Z" }, "2099-11-21"), true);
});

test("catalog API and details work without a provider key or external network requests", async t => {
  t.mock.method(globalThis, "fetch", async () => { throw new Error("Catalog must not contact the live provider"); });
  const ids: string[] = [];
  for (let page = 1; ; page++) {
    const response = await eventsRoute(new Request(`http://localhost/api/events?page=${page}`));
    assert.equal(response.status, 200);
    const body = await response.json();
    ids.push(...body.events.map((event: EventData) => event.id));
    if (!body.pagination.hasMore) break;
  }
  assert.equal(ids.length, filterCatalogEvents().length);
  assert.equal(new Set(ids).size, ids.length);
  const id = ids[0];
  const response = await detailsRoute(new Request("http://localhost"), { params: Promise.resolve({ id }) });
  assert.equal(response.status, 200);
  assert.equal((await response.json()).event.id, id);
  const missing = await detailsRoute(new Request("http://localhost"), { params: Promise.resolve({ id: "catalog:missing" }) });
  assert.equal(missing.status, 404);
});

test("catalog API validates direct callers and accepts the provided category IDs", async () => {
  for (const query of ["state=invalid", "city=Unknown", "category=unknown", "date=2099-02-30", "page=0", "page=1.5", "source=unknown", "keyword=" + "x".repeat(101)]) {
    assert.equal((await eventsRoute(new Request(`http://localhost/api/events?${query}`))).status, 400, query);
  }
  for (const category of ["games", "food-drinks", "arts-culture"]) {
    const response = await eventsRoute(new Request(`http://localhost/api/events?category=${category}`));
    assert.equal(response.status, 200);
    const body = await response.json();
    assert.ok(body.events.length > 0);
    assert.ok(body.events.every((event: EventData) => event.category === normalizeCategory(category)));
  }
});
