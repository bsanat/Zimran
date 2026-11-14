import assert from "node:assert/strict";
import test from "node:test";
import { cityListings } from "../src/lib/city-events.ts";
import type { EventData } from "../src/types/event.ts";

const now = new Date("2026-11-18T16:00:00Z");
const base = { id: "fixture", title: "Fixture", timezone: "America/New_York", status: "scheduled", category: "art", startAt: null } as EventData;

test("city timeline uses the next local occurrence, skips dark dates and keeps unconfirmed dates last in both directions", () => {
  const events: EventData[] = [
    { ...base, id: "unknown", dateText: "See the venue calendar" },
    { ...base, id: "weekly", isRecurring: true, recurrenceDays: [3], dateText: "Wednesdays at 6:28 AM", excludedDates: ["2026-11-25"] },
    { ...base, id: "timeslot", startAt: "2026-11-18T15:30:00-05:00" },
    { ...base, id: "later", startDate: "2027-02-15" },
    { ...base, id: "closed", occurrenceDates: ["2026-11-17", "2026-11-20"], excludedDates: ["2026-11-20"] },
    { ...base, id: "range", availableDateRanges: [["2026-11-15", "2026-11-18"], ["2026-11-23", "2026-11-25"]], excludedDates: ["2026-11-18"] },
  ];
  const soon = cityListings(events, "earliest", "en", now);
  assert.deepEqual(soon.map(row => [row.event.id, row.date, row.time]), [
    ["timeslot", "2026-11-18", "15:30"], ["range", "2026-11-23", ""],
    ["weekly", "2026-12-02", "06:28"], ["later", "2027-02-15", ""], ["unknown", "", ""],
  ]);
  assert.deepEqual(cityListings(events, "latest", "en", now).map(row => row.event.id), ["later", "weekly", "range", "timeslot", "unknown"]);
});

test("city ordering uses categories, venue time zone and preserves dates without inventing a time", () => {
  const late = new Date("2026-11-19T06:30:00Z"); // Still November 18 in California.
  const events: EventData[] = [
    { ...base, id: "run", timezone: "America/Los_Angeles", category: "running", startAt: "2026-11-19T07:00:00Z" },
    { ...base, id: "art", timezone: "America/Los_Angeles", category: "art", startAt: "2026-11-18" },
  ];
  assert.deepEqual(cityListings(events, "category", "en", late).map(row => row.event.id), ["art", "run"]);
  assert.deepEqual(cityListings(events, "earliest", "en", late).map(row => [row.date, row.time]), [["2026-11-18", "23:00"], ["2026-11-18", ""]]);
});
