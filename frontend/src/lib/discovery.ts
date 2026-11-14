import type { EventData } from "../types/event.ts";

export function getUpcomingMajorEvents(events: EventData[], now = new Date()): EventData[] {
  return events.filter(event => event.isMajor === true && event.status === "scheduled" &&
    Date.parse(event.startAt ?? "") > now.getTime())
    .sort((a, b) => Date.parse(a.startAt ?? "") - Date.parse(b.startAt ?? "")).slice(0, 3);
}
