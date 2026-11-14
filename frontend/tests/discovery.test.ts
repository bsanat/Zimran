import assert from "node:assert/strict";
import test from "node:test";
import { getUpcomingMajorEvents } from "../src/lib/discovery.ts";
import { INTERESTS, createDefaultDraft, toPlannerRequest } from "../src/lib/planner.ts";
import type { EventData } from "../src/types/event.ts";

const now = new Date("2026-11-14T12:00:00Z");
const fixture = { id: "test", isMajor: true, status: "scheduled", startAt: "2026-11-21T23:00:00Z" } as EventData;

test("major events require explicit source metadata, a future date, and active status", () => {
  const events = [fixture, { ...fixture, id: "ordinary", isMajor: undefined },
    { ...fixture, id: "cancelled", status: "cancelled" as const },
    { ...fixture, id: "old", startAt: "2026-11-01T23:00:00Z" },
    { ...fixture, id: "invalid", startAt: "unknown" }];
  assert.deepEqual(getUpcomingMajorEvents(events, now).map(event => event.id), ["test"]);
});

test("major event picks are chronological and limited to three without mutating input", () => {
  const events = [24, 20, 22, 21].map(day => ({ ...fixture, id: String(day), startAt: `2026-11-${day}T23:00:00Z` }));
  assert.deepEqual(getUpcomingMajorEvents(events, now).map(event => event.id), ["20", "21", "22"]);
  assert.equal(events[0].id, "24");
});

test("the requested discovery categories also work in the planner contract", () => {
  const ids = ["family", "books", "gaming", "technology", "food", "ai", "running", "art", "climate", "fitness", "wellness", "crypto"];
  assert.equal(new Set(INTERESTS.map(interest => interest.id)).size, INTERESTS.length);
  for (const id of ids) {
    const interest = INTERESTS.find(item => item.id === id);
    assert.ok(interest);
    assert.deepEqual(toPlannerRequest({ ...createDefaultDraft(), interests: [interest.id] }, now).interests, [id]);
  }
});
