import type { EventData } from "../types/event.ts";
import { catalogMatchesDate } from "./catalog.ts";
import { translate, type Locale } from "./i18n.ts";
import { addDays, getCityToday, INTERESTS, isDate } from "./planner.ts";

export type CitySort = "earliest" | "latest" | "category";
export type CityListing = { event: EventData; date: string; time: string; category: string };

function schedule(event: EventData, now: Date): { date: string; time: string } | null {
  const timezone = event.timezone;
  const today = getCityToday(now, timezone);
  const instant = new Date(event.startAt ?? "");
  const dateOnlyStart = event.startAt && isDate(event.startAt) ? event.startAt : "";
  const timed = !dateOnlyStart && Number.isFinite(instant.getTime());
  const clock = new Intl.DateTimeFormat("en-GB", { timeZone: timezone, hour: "2-digit", minute: "2-digit", hourCycle: "h23" });
  let time = timed ? clock.format(instant) : "";
  if (!timed && event.isRecurring) {
    // Single weekly starts are unambiguous; multi-session schedules keep the organizer's text.
    const match = event.dateText?.match(/^(?:[\w ,]+)\s+(?:from|at)\s+(\d{1,2}):([0-5]\d)\s*(AM|PM)\s*$/i);
    if (match && +match[1] >= 1 && +match[1] <= 12) {
      const hour = +match[1] % 12 + (match[3].toLowerCase() === "pm" ? 12 : 0);
      time = String(hour).padStart(2, "0") + ":" + match[2];
    }
  }
  const start = event.startDate && isDate(event.startDate) ? event.startDate : dateOnlyStart || (timed ? getCityToday(instant, timezone) : "");
  const end = event.endDate && isDate(event.endDate) ? event.endDate : "";
  if (end && end < today) return null;
  const first = start > today ? start : today;
  if (event.occurrenceDates?.length) {
    const date = event.occurrenceDates.filter(day => day >= first && (!end || day <= end) && isDate(day)
      && catalogMatchesDate(event, day) && !(timed && day === start && instant < now)).sort()[0];
    return date ? { date, time: !timed || date === start ? time : "" } : null;
  }
  if (event.availableDateRanges?.length || event.isRecurring && event.recurrenceDays?.length) {
    const ranges = event.availableDateRanges?.length ? event.availableDateRanges.toSorted(([a], [b]) => a.localeCompare(b))
      : [[first, end || addDays(first, 366 + (event.excludedDates?.length ?? 0) * 7)]];
    for (const [from, to] of ranges) {
      if (!isDate(from) || !isDate(to)) continue;
      for (let day = from > first ? from : first; day <= to && (!end || day <= end); day = addDays(day, 1)) {
        const dayTime = !timed || day === start ? time : "";
        if (day === today && dayTime && dayTime < clock.format(now)) continue;
        if (catalogMatchesDate(event, day)) return { date: day, time: dayTime };
      }
    }
    return null;
  }
  if (start && (start < today || timed && instant < now)) {
    if (!end || end < today || timed && end === start) return null;
  }
  if (start && event.excludedDates?.includes(start) && (!end || end === start)) return null;
  return { date: start, time };
}

export function cityListings(events: EventData[], sort: CitySort, locale: Locale, now = new Date()): CityListing[] {
  const rows: CityListing[] = [];
  for (const event of events) {
    if (event.status !== "scheduled") continue;
    let when;
    try {
      when = schedule(event, now);
    } catch (error) {
      if (!(error instanceof RangeError)) throw error;
      when = { date: "", time: "" };
    }
    if (when) rows.push({ event, ...when, category: translate(locale, INTERESTS.find(item => item.id === event.category)?.label ?? event.category) });
  }
  const compare = new Intl.Collator(locale).compare;
  const direction = sort === "latest" ? -1 : 1;
  return rows.sort((a, b) => (sort === "category" ? compare(a.category, b.category) : 0)
    || Number(!a.date) - Number(!b.date) || direction * a.date.localeCompare(b.date)
    || Number(!a.time) - Number(!b.time) || direction * a.time.localeCompare(b.time)
    || compare(a.event.title, b.event.title));
}
