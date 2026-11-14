import { catalogMatchesDate } from "./catalog.ts";
import { addDays, getCityToday } from "./planner.ts";
import type { EventData } from "../types/event.ts";

export function getUpcomingMajorEvents(events: EventData[], now = new Date()): EventData[] {
  return events.filter(event => event.isMajor === true && event.status === "scheduled" &&
    Date.parse(event.startAt ?? "") > now.getTime())
    .sort((a, b) => Date.parse(a.startAt ?? "") - Date.parse(b.startAt ?? "")).slice(0, 3);
}

export function nextOccurrences(events: EventData[], now = new Date()) {
  return events.flatMap(event => {
    if (event.status !== "scheduled") return [];
    const today = getCityToday(now, event.timezone);
    const days = Array.from({ length: 30 }, (_, offset) => addDays(today, offset));
    if (event.startAt && Date.parse(event.startAt) < now.getTime() && !event.isRecurring && (!event.endDate || event.endDate <= today)) return [];
    const detailedSchedule = event.isRecurring || event.occurrenceDates?.length || event.availableDateRanges?.length;
    const date = days.find(day =>
      catalogMatchesDate(event, day) && (detailedSchedule || !event.startDate || event.startDate === day)
    );
    return date ? [{ event, date }] : [];
  }).sort((a, b) => a.date.localeCompare(b.date) || (a.event.startAt ?? "").localeCompare(b.event.startAt ?? ""));
}

export function topEventCities(events: Pick<EventData, "city" | "state" | "status">[]) {
  const counts = new Map<string, { state: string; city: string; count: number }>();
  for (const { city, state, status } of events) {
    if (!state || status !== "scheduled") continue;
    const key = `${state}/${city}`;
    const existing = counts.get(key);
    if (existing) existing.count++;
    else counts.set(key, { state, city, count: 1 });
  }
  return [...counts.values()].sort((a, b) => b.count - a.count ||
    a.city.localeCompare(b.city, "en-US") || a.state.localeCompare(b.state)).slice(0, 5);
}
