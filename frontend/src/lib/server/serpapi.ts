import { createHash, randomUUID } from "node:crypto";
import type { EventData, EventsResponse } from "../../types/event.ts";
import type { PlannerRequest, PlannerResponse } from "../../types/planner.ts";
import { getCityToday, INTERESTS, safeExternalUrl, TIME_OPTIONS } from "../planner.ts";
import { isSupportedCity } from "../locations.ts";

type RecordData = Record<string, unknown>;
type Filters = { city: string; state: string; category: string; date: string; keyword: string; page: number; major: boolean };
const terms: Record<string, string> = {
  music: "concerts", sports: "sports events", food: "food festivals", art: "art exhibitions",
  theater: "theater shows", outdoors: "outdoor events", nightlife: "nightlife events",
  technology: "technology events", comedy: "comedy shows", gaming: "gaming events",
  family: "family events", books: "book events", ai: "artificial intelligence events",
  running: "running events", climate: "climate events", fitness: "fitness events",
  wellness: "wellness events", crypto: "crypto events",
};
const cache = new Map<string, { expires: number; promise: Promise<RecordData> }>();
let windowStart = 0;
let providerRequests = 0;
const unavailable = "The event service is temporarily unavailable. Please try again.";

export class ServiceError extends Error {
  status: number;
  code: string;
  field?: string;

  constructor(status: number, code: string, message: string, field?: string) {
    super(message);
    this.status = status;
    this.code = code;
    this.field = field;
  }
}

function record(value: unknown): value is RecordData {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

function text(value: unknown): string | null {
  return typeof value === "string" && value.trim() ? value.trim() : null;
}

function validDate(value: string): boolean {
  const parsed = new Date(`${value}T12:00:00Z`);
  return /^\d{4}-\d{2}-\d{2}$/.test(value) && Number.isFinite(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

function invalid(field: string): never {
  throw new ServiceError(400, "VALIDATION_ERROR", "Please check your preferences and try again.", field);
}

export function eventFilters(query: URLSearchParams): Filters {
  const city = query.get("city")?.trim() || "New York";
  const state = query.get("state") || "NY";
  const category = query.get("category") || "";
  const date = query.get("date") || "";
  const keyword = query.get("keyword")?.trim() || "";
  const page = Number(query.get("page") ?? 1);
  if (!isSupportedCity(city, state)) invalid("city");
  if (category && !INTERESTS.some(({ id }) => id === category)) invalid("category");
  if (date && (!validDate(date) || date < getCityToday())) invalid("date");
  if (keyword.length > 100) invalid("keyword");
  if (!Number.isInteger(page) || page < 1 || page > 100) invalid("page");
  if (query.has("major") && !["true", "false"].includes(query.get("major")!)) invalid("major");
  return { city, state, category, date, keyword, page, major: query.get("major") === "true" };
}

async function provider(parameters: Record<string, string>): Promise<RecordData> {
  const apiKey = process.env.SERPAPI_KEY?.trim() || process.env.API_KEY?.trim();
  if (!apiKey) throw new ServiceError(503, "SERVICE_NOT_CONFIGURED", unavailable);
  const key = JSON.stringify(parameters);
  const now = Date.now();
  const hit = cache.get(key);
  if (hit && hit.expires > now) return hit.promise;
  cache.delete(key);
  if (now - windowStart >= 60_000) { windowStart = now; providerRequests = 0; }
  if (providerRequests >= 30) throw new ServiceError(429, "RATE_LIMITED", "Too many requests. Please wait a moment before trying again.");
  providerRequests++;
  const promise = (async () => {
    const url = new URL("https://serpapi.com/search.json");
    url.search = new URLSearchParams({ ...parameters, hl: "en", gl: "us", api_key: apiKey }).toString();
    try {
      const response = await fetch(url, { signal: AbortSignal.timeout(10_000), cache: "no-store" });
      const body: unknown = await response.json();
      if (response.status === 429) throw new ServiceError(429, "RATE_LIMITED", "Too many requests. Please wait a moment before trying again.");
      if (!response.ok || !record(body) || body.error) throw new ServiceError(502, "PROVIDER_ERROR", unavailable);
      return body;
    } catch (error) {
      if (error instanceof ServiceError) throw error;
      throw new ServiceError(502, "PROVIDER_ERROR", unavailable);
    }
  })();
  // shortcut: Cache and quota guard belong to one server process; use shared storage before running multiple instances.
  if (cache.size >= 100) cache.delete(cache.keys().next().value!);
  cache.set(key, { expires: now + 30 * 60_000, promise });
  try { return await promise; } catch (error) { cache.delete(key); throw error; }
}

function sourceDate(raw: RecordData): string | null {
  const date = record(raw.date) ? [raw.date.start_date, raw.date.when, raw.time] : [raw.date, raw.time];
  const parts = [...new Set(date.map(text).filter((value): value is string => value !== null))];
  return parts.filter(part => !parts.some(other => other !== part && other.includes(part))).join(" · ") || null;
}

function normalize(raw: RecordData, filters: Filters, identity?: string): EventData | null {
  const title = text(raw.title);
  if (!title) return null;
  const dateText = sourceDate(raw);
  const address = Array.isArray(raw.address) ? raw.address.map(text).filter(Boolean).join(", ") || null : text(raw.address);
  const hash = identity ?? createHash("sha256").update(JSON.stringify([title, dateText, address])).digest("hex").slice(0, 24);
  const context = [filters.city, filters.state, filters.category, filters.date, filters.keyword, hash];
  const ticketLinks = Array.isArray(raw.ticket_info) ? raw.ticket_info.filter(record).map(ticket => text(ticket.link)) : [];
  const sourceUrl = [...ticketLinks, text(raw.link)].map(link => safeExternalUrl(link, "serpapi")).find(Boolean) ?? null;
  const priceText = text(raw.price);
  const free = /^free$/i.test(priceText ?? "");
  const priceKnown = free || (typeof raw.extracted_price === "number" && Number.isFinite(raw.extracted_price) &&
    raw.extracted_price >= 0 && /^(?:from\s+)?(?:US)?\$/i.test(priceText ?? ""));
  const iso = [raw.startAt, record(raw.date) ? raw.date.when : raw.date]
    .find(value => typeof value === "string" && /^\d{4}-\d{2}-\d{2}T.+(?:Z|[+-]\d{2}:\d{2})$/.test(value) && Number.isFinite(Date.parse(value)));
  return {
    id: `serpapi:${Buffer.from(JSON.stringify(context)).toString("base64url")}`,
    source: "serpapi", sourceEventId: hash, title, category: filters.category || "events", city: filters.city,
    startAt: typeof iso === "string" ? iso : null, dateText, timezone: "America/New_York",
    venue: text(record(raw.venue) ? raw.venue.name : raw.venue) ||
      (Array.isArray(raw.address) && raw.address.length > 1 ? text(raw.address[0]) : null),
    address, description: text(raw.description), imageUrl: safeExternalUrl(text(raw.thumbnail), "serpapi"),
    priceMin: priceKnown ? free ? 0 : raw.extracted_price as number : null,
    priceMax: free ? 0 : null, priceKnown, priceText, currency: priceKnown ? "USD" : null,
    sourceUrl, status: raw.status === "cancelled" || raw.status === "postponed" ? raw.status : "scheduled",
    isMajor: false,
  };
}

async function candidates(filters: Filters): Promise<{ event: EventData; raw: RecordData }[]> {
  const q = `${terms[filters.category] || "events"} in ${filters.city}, USA${filters.keyword ? ` ${filters.keyword}` : ""}${filters.date ? ` on ${filters.date}` : ""}`;
  const body = await provider({ engine: "google", q });
  if (body.events_results !== undefined && !Array.isArray(body.events_results)) throw new ServiceError(502, "PROVIDER_ERROR", unavailable);
  const results: { event: EventData; raw: RecordData }[] = [];
  for (const raw of (body.events_results ?? []) as unknown[]) {
    if (!record(raw)) continue;
    const event = normalize(raw, filters);
    if (event && !results.some(item => item.event.sourceEventId === event.sourceEventId)) results.push({ event, raw });
  }
  return results;
}

function calendarDate(event: EventData): string | null {
  if (event.startAt) return getCityToday(new Date(event.startAt));
  const value = event.dateText ?? "";
  const isoDate = value.match(/\b(\d{4}-\d{2}-\d{2})\b/)?.[1];
  if (isoDate && validDate(isoDate)) return isoDate;
  const full = value.match(/\b(Jan\w*|Feb\w*|Mar\w*|Apr\w*|May|Jun\w*|Jul\w*|Aug\w*|Sep\w*|Oct\w*|Nov\w*|Dec\w*)\s+(\d{1,2}),?\s+(\d{4})\b/i);
  if (!full) return null;
  const month = ["jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec"].indexOf(full[1].slice(0, 3).toLowerCase()) + 1;
  const date = `${full[3]}-${String(month).padStart(2, "0")}-${full[2].padStart(2, "0")}`;
  return validDate(date) ? date : null;
}

export async function searchEvents(filters: Filters): Promise<EventsResponse> {
  // Google does not verify major-event status; ordinary results must not be labelled as major events.
  const events = filters.major ? [] : (await candidates(filters)).map(item => item.event)
    .filter(event => !filters.date || calendarDate(event) === filters.date);
  const pageSize = 10;
  const offset = (filters.page - 1) * pageSize;
  return { events: events.slice(offset, offset + pageSize), pagination: { page: filters.page, pageSize, hasMore: offset + pageSize < events.length } };
}

async function enrich(event: EventData, raw: RecordData, filters: Filters): Promise<EventData> {
  const token = text(raw.event_token);
  if (!token) return event;
  const body = await provider({ engine: "google_event_details", event_token: token });
  if (!record(body.event_details)) throw new ServiceError(502, "PROVIDER_ERROR", unavailable);
  return normalize({ ...raw, ...body.event_details, title: text(body.event_details.title) || event.title }, filters, event.sourceEventId)!;
}

export async function eventDetails(id: string): Promise<{ event: EventData }> {
  let filters: Filters;
  let hash: string;
  try {
    if (!id.startsWith("serpapi:") || id.length > 1000) throw new Error();
    const context: unknown = JSON.parse(Buffer.from(id.slice(8), "base64url").toString("utf8"));
    if (!Array.isArray(context) || context.length !== 6 || !context.every(item => typeof item === "string") || !/^[a-f0-9]{24}$/.test(context[5])) throw new Error();
    filters = eventFilters(new URLSearchParams({ city: context[0], state: context[1], category: context[2], date: context[3], keyword: context[4] }));
    hash = context[5];
  } catch { throw new ServiceError(404, "NOT_FOUND", "This event could not be found."); }
  const item = (await candidates(filters)).find(item => item.event.sourceEventId === hash);
  if (!item) throw new ServiceError(404, "NOT_FOUND", "This event could not be found.");
  return { event: await enrich(item.event, item.raw, filters) };
}

function plannerInput(value: unknown): PlannerRequest {
  if (!record(value)) invalid("body");
  if (value.city !== "New York") invalid("city");
  if (!Array.isArray(value.interests) || !value.interests.length || value.interests.length > INTERESTS.length ||
    !value.interests.every(id => typeof id === "string" && INTERESTS.some(interest => interest.id === id))) invalid("interests");
  if (typeof value.startDate !== "string" || !validDate(value.startDate) || value.startDate < getCityToday()) invalid("startDate");
  if (typeof value.endDate !== "string" || !validDate(value.endDate) || value.endDate < value.startDate) invalid("endDate");
  if (!TIME_OPTIONS.some(option => option.value === value.timePreference)) invalid("timePreference");
  if (value.budgetMax !== undefined && (typeof value.budgetMax !== "number" || !Number.isFinite(value.budgetMax) || value.budgetMax < 0)) invalid("budgetMax");
  if (value.notes !== undefined && (typeof value.notes !== "string" || value.notes.length > 500)) invalid("notes");
  return value as unknown as PlannerRequest;
}

function matches(event: EventData, input: PlannerRequest): boolean {
  const date = calendarDate(event);
  if (!date || date < input.startDate || date > input.endDate || event.status !== "scheduled") return false;
  if (input.budgetMax !== undefined && (!event.priceKnown || event.currency !== "USD" || event.priceMin === null || event.priceMin > input.budgetMax)) return false;
  if (input.timePreference === "any") return true;
  const time = event.dateText?.match(/\b(\d{1,2})(?::([0-5]\d))?\s*(AM|PM)\b/i);
  const ambiguousTime = time && /\d\s*[–—-]\s*$/.test(event.dateText!.slice(0, time.index));
  const hour = event.startAt ? Number(new Intl.DateTimeFormat("en-US", { timeZone: event.timezone, hour: "numeric", hourCycle: "h23" }).format(new Date(event.startAt))) :
    time && !ambiguousTime && Number(time[1]) >= 1 && Number(time[1]) <= 12 ? Number(time[1]) % 12 + (time[3].toLowerCase() === "pm" ? 12 : 0) : null;
  return hour !== null && (input.timePreference === "morning" ? hour < 12 : input.timePreference === "afternoon" ? hour >= 12 && hour < 17 : hour >= 17);
}

export async function planEvents(value: unknown): Promise<PlannerResponse> {
  const input = plannerInput(value);
  const searches = await Promise.all([...new Set(input.interests)].map(async category => {
    const filters = eventFilters(new URLSearchParams({ city: input.city, category }));
    return (await candidates(filters)).map(item => ({ ...item, filters }));
  }));
  const unique = [...new Map(searches.flat().map(item => [item.event.sourceEventId, item])).values()];
  // shortcut: Enrich at most five search candidates to bound API usage; add an event index for broader planner coverage.
  const events = await Promise.all(unique.slice(0, 5).map(item => enrich(item.event, item.raw, item.filters)));
  return {
    planId: randomUUID(), summary: "Events found for your interests. Confirm the latest details on the event website.",
    recommendations: events.filter(event => matches(event, input)).map(event => ({ event, matchScore: 100, reason: "Matches your interest search and the confirmed date, time and budget filters." })),
    suggestions: ["Events with unconfirmed dates or prices are excluded when those filters apply.",
      ...(input.notes?.trim() ? ["Additional wishes are not interpreted automatically. Check the event description."] : [])],
  };
}

export async function apiResponse(action: () => Promise<unknown>): Promise<Response> {
  try { return Response.json(await action(), { headers: { "Cache-Control": "no-store" } }); }
  catch (error) {
    const problem = error instanceof ServiceError ? error : new ServiceError(502, "PROVIDER_ERROR", unavailable);
    return Response.json({ error: { code: problem.code, message: problem.message, ...(problem.field ? { details: { field: problem.field } } : {}) } }, { status: problem.status, headers: { "Cache-Control": "no-store" } });
  }
}
