import catalog from "../data/catalog.json" with { type: "json" };
import type { EventData } from "../types/event.ts";
import { US_STATES, type USState } from "./locations.ts";
import { getCityToday } from "./planner.ts";

export type CatalogEvent = EventData & { state: string; lastVerifiedAt: string; sourceUrl: string; imageUrl: string };
export const CATALOG_EVENTS: CatalogEvent[] = (catalog.events as CatalogEvent[]).map(event => ({
  ...event, id: `catalog:${event.id}`, source: "catalog", sourceEventId: event.id, status: "scheduled",
  sourceName: new URL(event.sourceUrl).hostname.replace(/^www\./, ""),
}));
export const catalogMeta = catalog.meta;
export const catalogCategories = catalog.categories;

export const catalogRegions: USState[] = [...US_STATES, { code: "DC", name: "District of Columbia", cities: ["Washington"] }]
  .map(state => ({ ...state, cities: [...new Set(CATALOG_EVENTS.filter(event => event.state === state.code).map(event => event.city))].sort((a, b) => a.localeCompare(b, "en-US")) }))
  .sort((a, b) => a.name.localeCompare(b.name, "en-US"));

export function catalogMatchesDate(event: EventData, date: string): boolean {
  if (event.excludedDates?.includes(date)) return false;
  if (event.occurrenceDates?.length) return event.occurrenceDates.includes(date);
  if (event.availableDateRanges?.length) return event.availableDateRanges.some(([start, end]) => start <= date && date <= end);
  if (event.startDate && (date < event.startDate || date > (event.endDate || event.startDate))) return false;
  if (event.recurrenceMonths?.length && !event.recurrenceMonths.includes(Number(date.slice(5, 7)))) return false;
  if (event.isRecurring) return Boolean(event.recurrenceDays?.includes(new Date(`${date}T12:00:00Z`).getUTCDay()));
  if (event.startDate) return true;
  if (!event.startAt) return false;
  const parts = new Intl.DateTimeFormat("en-CA", { timeZone: event.timezone, year: "numeric", month: "2-digit", day: "2-digit" }).formatToParts(new Date(event.startAt));
  const value = (type: string) => parts.find(part => part.type === type)?.value;
  return `${value("year")}-${value("month")}-${value("day")}` === date;
}

export function filterCatalogEvents(filters: { state?: string; city?: string; category?: string; date?: string; keyword?: string; budgetMax?: number } = {}): CatalogEvent[] {
  const keyword = filters.keyword?.trim().toLocaleLowerCase("en-US");
  const today = getCityToday();
  return CATALOG_EVENTS.filter(event => (!(event.endDate || event.startDate) || (event.endDate || event.startDate)! >= today) &&
    (!filters.state || event.state === filters.state) &&
    (!filters.city || event.city === filters.city) && (!filters.category || event.category === filters.category) &&
    (!filters.date || catalogMatchesDate(event, filters.date)) &&
    (filters.budgetMax === undefined || event.priceKnown && event.currency === "USD" && event.priceMin !== null && event.priceMin <= filters.budgetMax) &&
    (!keyword || [event.title, event.description, event.city, event.venue, event.category].some(value => value?.toLocaleLowerCase("en-US").includes(keyword))));
}

export function normalizeCategory(value: string): string {
  return ({ games: "gaming", "food-drinks": "food", "arts-culture": "art" } as Record<string, string>)[value] || value;
}
