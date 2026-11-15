import assert from "node:assert/strict";
import test from "node:test";
import {
  BUDGETS, createDefaultDraft, formatEventDate, formatPrice, getCityToday, getDateRange, INTERESTS, TIME_OPTIONS,
  isPlannerDraft, safeExternalUrl, toPlannerRequest, validateDraft,
} from "../src/lib/planner.ts";

test("presets use the New York calendar, including midnight and year boundaries", () => {
  const now = new Date("2026-11-15T02:00:00Z");
  assert.equal(getCityToday(now), "2026-11-14");
  assert.deepEqual(getDateRange("today", "", now), { startDate: "2026-11-14", endDate: "2026-11-14" });
  assert.deepEqual(getDateRange("weekend", "", now), { startDate: "2026-11-14", endDate: "2026-11-15" });
  assert.deepEqual(getDateRange("weekend", "", new Date("2026-11-15T18:00:00Z")), {
    startDate: "2026-11-15", endDate: "2026-11-15",
  });
  assert.deepEqual(getDateRange("weekend", "", new Date("2026-11-16T18:00:00Z")), {
    startDate: "2026-11-21", endDate: "2026-11-22",
  });
  assert.deepEqual(getDateRange("tomorrow", "", new Date("2026-12-31T18:00:00Z")), {
    startDate: "2027-01-01", endDate: "2027-01-01",
  });
});

test("planner rejects absent interests, invalid calendar dates, old dates and oversized notes", () => {
  const now = new Date("2026-11-14T18:00:00Z");
  const draft = createDefaultDraft();
  assert.ok(validateDraft(draft, now).interests);
  draft.interests = ["music"];
  assert.deepEqual(validateDraft(draft, now), {});
  draft.datePreset = "custom";
  draft.customDate = "2027-02-30";
  assert.ok(validateDraft(draft, now).customDate);
  draft.customDate = "2026-11-13";
  assert.ok(validateDraft(draft, now).customDate);
  draft.customDate = "2026-11-14";
  draft.notes = "x".repeat(501);
  assert.ok(validateDraft(draft, now).notes);
  draft.notes = "x".repeat(500);
  draft.budget = "NaN";
  assert.ok(validateDraft(draft, now).budget);
  assert.throws(() => toPlannerRequest(draft, now), /budget/i);
});

test("cached drafts require the complete shape but can keep an unfinished selection", () => {
  assert.equal(isPlannerDraft(createDefaultDraft()), true);
  assert.equal(isPlannerDraft({ interests: [] }), false);
  assert.equal(isPlannerDraft(null), false);
  assert.equal(isPlannerDraft({ ...createDefaultDraft(), interests: ["unknown"] }), false);
  assert.equal(isPlannerDraft({ ...createDefaultDraft(), notes: "x".repeat(501) }), false);
  assert.equal(isPlannerDraft({ ...createDefaultDraft(), customDate: "2026-02-30" }), false);
  assert.equal(isPlannerDraft({ ...createDefaultDraft(), budget: "5000" }), false);
});

test("unbounded budget is omitted while Free only sends zero; requests use a single date contract", () => {
  const now = new Date("2026-11-14T18:00:00Z");
  const draft = { ...createDefaultDraft(), interests: ["music", "music"] as const };
  const request = toPlannerRequest({ ...draft, interests: [...draft.interests], notes: "  Quiet venues  " }, now);
  assert.equal("budgetMax" in request, false);
  assert.deepEqual(request.interests, ["music"]);
  assert.equal(request.notes, "Quiet venues");
  assert.equal(request.startDate, "2026-11-14");
  assert.equal(request.endDate, "2026-11-15");
  assert.equal(toPlannerRequest({ ...draft, interests: [...draft.interests], budget: "0" }, now).budgetMax, 0);
});

test("unknown prices are not free and known ranges keep their source currency", () => {
  const unknown = { priceKnown: false, priceMin: null, priceMax: null, currency: "USD" };
  assert.equal(formatPrice(unknown), "Price unavailable");
  assert.equal(formatPrice({ ...unknown, priceKnown: true }), "Price unavailable");
  assert.equal(formatPrice({ ...unknown, priceKnown: true, priceMin: 0, priceMax: 0 }), "Free");
  assert.equal(formatPrice({ ...unknown, priceKnown: true, priceMin: 25, priceMax: 50 }), "$25 – $50");
  assert.equal(formatPrice({ ...unknown, priceKnown: true, priceMin: 25, currency: null }), "Price unavailable");
  assert.equal(formatPrice({ ...unknown, priceKnown: true, priceMin: 25, currency: "EUR" }), "From €25");
});

test("event dates use venue time and unknown zones fall back to explicit UTC", () => {
  const startAt = "2026-11-15T01:00:00Z";
  assert.match(formatEventDate({ startAt, timezone: "America/New_York" }), /Nov 14.*8:00 PM.*EST/);
  assert.match(formatEventDate({ startAt, timezone: "Not/AZone" }), /Nov 15.*UTC/);
  assert.equal(formatEventDate({ startAt: "bad", timezone: "America/New_York" }), "Date and time unavailable");
  assert.equal(formatEventDate({ startAt: null, dateText: "Nov 21 · 7:00 PM", timezone: "America/New_York" }), "Nov 21 · 7:00 PM");
  assert.equal(formatEventDate({ startAt: null, timezone: "America/New_York" }), "Date and time unavailable");
});

test("source links only accept HTTPS Ticketmaster destinations", () => {
  assert.equal(safeExternalUrl("https://www.ticketmaster.com/event/123"), "https://www.ticketmaster.com/event/123");
  for (const url of [
    "javascript:alert(1)", "http://ticketmaster.com/event/123", "https://ticketmaster.com.evil.test/event/1",
    "https://evil.test/?next=ticketmaster.com", "https://user:pass@ticketmaster.com/event/1", "https://ticketmaster.com:8443/event/1",
  ]) assert.equal(safeExternalUrl(url), null);
});

test("Spring JSON catalog links accept organizer HTTPS URLs and retain safety checks", () => {
  assert.equal(safeExternalUrl("https://jazz.org/dizzys/", "json"), "https://jazz.org/dizzys/");
  assert.equal(safeExternalUrl("https://jazz.org/dizzys/", "unknown"), null);
  for (const url of [
    "javascript:alert(1)", "data:text/html,unsafe", "ftp://example.org/event", "http://example.org/event",
    "https://user:pass@example.org/event", "https://example.org:8443/event",
  ]) assert.equal(safeExternalUrl(url, "json"), null);
});

test("SerpApi source links accept HTTPS event websites without credentials or custom ports", () => {
  assert.equal(safeExternalUrl("https://example.org/tickets", "serpapi"), "https://example.org/tickets");
  assert.equal(safeExternalUrl(null, "serpapi"), null);
  for (const url of ["javascript:alert(1)", "http://example.org/event", "https://user:pass@example.org/event", "https://example.org:8443/event"]) {
    assert.equal(safeExternalUrl(url, "serpapi"), null);
  }
  assert.equal(formatPrice({ priceKnown: false, priceMin: null, priceMax: null, currency: null, priceText: "Tickets from $30" }), "Tickets from $30");
});
