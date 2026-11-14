import type { EventData, EventsResponse } from "../types/event.ts";
import type { PlannerRequest, PlannerResponse } from "../types/planner.ts";

export class ApiError extends Error {
  status: number;
  code: string;
  field?: string;

  constructor(message: string, status = 0, code = "NETWORK_ERROR", field?: string) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.field = field;
  }
}

function record(value: unknown): value is Record<string, unknown> {
  return typeof value === "object" && value !== null && !Array.isArray(value);
}

export function isEvent(value: unknown): value is EventData {
  if (!record(value)) return false;
  const strings = ["id", "source", "sourceEventId", "title", "category", "city", "timezone"];
  const nullableStrings = ["startAt", "sourceUrl", "venue", "address", "description", "imageUrl", "currency"];
  return strings.every((key) => typeof value[key] === "string" && (value[key] as string).length > 0) &&
    nullableStrings.every((key) => value[key] === null || typeof value[key] === "string") &&
    ["dateText", "priceText"].every((key) => value[key] === undefined || value[key] === null || typeof value[key] === "string") &&
    ["state", "sourceName", "lastVerifiedAt", "startDate", "endDate"].every((key) => value[key] === undefined || value[key] === null || typeof value[key] === "string") &&
    ["imageIsIllustration", "isRecurring", "isSocial"].every((key) => value[key] === undefined || typeof value[key] === "boolean") &&
    (value.recurrenceDays === undefined || (Array.isArray(value.recurrenceDays) && value.recurrenceDays.every(day => Number.isInteger(day) && day >= 0 && day <= 6))) &&
    (value.recurrenceMonths === undefined || (Array.isArray(value.recurrenceMonths) && value.recurrenceMonths.every(month => Number.isInteger(month) && month >= 1 && month <= 12))) &&
    ["occurrenceDates", "excludedDates"].every(key => value[key] === undefined || (Array.isArray(value[key]) && value[key].every(date => typeof date === "string"))) &&
    (value.availableDateRanges === undefined || (Array.isArray(value.availableDateRanges) && value.availableDateRanges.every(range => Array.isArray(range) && range.length === 2 && range.every(date => typeof date === "string")))) &&
    ["priceMin", "priceMax"].every((key) => value[key] === null ||
      (typeof value[key] === "number" && Number.isFinite(value[key]) && (value[key] as number) >= 0)) &&
    typeof value.priceKnown === "boolean" &&
    (value.isMajor === undefined || typeof value.isMajor === "boolean") &&
    ["scheduled", "cancelled", "postponed"].includes(value.status as string);
}

function invalidResponse(): never {
  throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
}

function defaultMessage(status: number): string {
  if (status === 400) return "Please check your preferences and try again.";
  if (status === 404) return "This event could not be found.";
  if (status === 429) return "Too many requests. Please wait a moment before trying again.";
  if (status === 502 || status === 503) return "The event service is temporarily unavailable. Please try again.";
  return "We could not complete your request. Please try again.";
}

/**
 * Low-level JSON request to `${NEXT_PUBLIC_API_BASE_URL}/api${path}`. With allowEmpty, a 204 No Content
 * resolves to null instead of being treated as an invalid response.
 */
export async function apiRequest(path: string, init: RequestInit, signal?: AbortSignal, allowEmpty = false): Promise<unknown> {
  const controller = new AbortController();
  let timedOut = false;
  const abort = () => controller.abort();
  if (signal?.aborted) controller.abort();
  else signal?.addEventListener("abort", abort, { once: true });
  const timeout = setTimeout(() => { timedOut = true; controller.abort(); }, 30_000);
  try {
    const base = (process.env.NEXT_PUBLIC_API_BASE_URL ?? "").replace(/\/$/, "");
    // Headers instances do not spread into an object, so merge through Headers.
    const headers = new Headers(init.headers);
    if (!headers.has("Accept")) headers.set("Accept", "application/json");
    // A cross-origin backend (NEXT_PUBLIC_API_BASE_URL) never sees the outly-lang cookie, so name the page's
    // language; behind the proxy the cookie still wins.
    const pageLanguage = typeof document !== "undefined" ? document.documentElement.lang : "";
    if (pageLanguage && !headers.has("Accept-Language")) headers.set("Accept-Language", pageLanguage);
    const response = await fetch(`${base}/api${path}`, { ...init, signal: controller.signal, headers });
    let body: unknown;
    try { body = await response.json(); } catch (error) {
      if (controller.signal.aborted) throw error;
      body = null;
    }
    if (!response.ok) {
      const error = record(body) && record(body.error) ? body.error : null;
      const details = error && record(error.details) ? error.details : null;
      throw new ApiError(
        typeof error?.message === "string" ? error.message :
          response.status === 404 && !path.startsWith("/events/") ?
            "The event service is not available right now. Please try again." : defaultMessage(response.status),
        response.status,
        typeof error?.code === "string" ? error.code : "API_ERROR",
        typeof details?.field === "string" ? details.field : undefined,
      );
    }
    if (allowEmpty && response.status === 204) return null;
    if (!record(body)) invalidResponse();
    return body;
  } catch (error) {
    if (error instanceof ApiError) throw error;
    if (timedOut) throw new ApiError("The request took too long. Your preferences are saved; please try again.", 408, "TIMEOUT");
    if (signal?.aborted) throw error;
    throw new ApiError("We could not reach the event service. Check your connection and try again.");
  } finally {
    clearTimeout(timeout);
    signal?.removeEventListener("abort", abort);
  }
}

export async function getEvents(
  filters: { city?: string; state?: string; date?: string; category?: string; budgetMax?: number; page?: number; major?: boolean; keyword?: string; source?: "catalog" | "live" },
  signal?: AbortSignal,
): Promise<EventsResponse> {
  const query = new URLSearchParams({ page: String(filters.page ?? 1) });
  if (filters.city) query.set("city", filters.city);
  if (filters.date) query.set("date", filters.date);
  if (filters.category) query.set("category", filters.category);
  if (filters.state) query.set("state", filters.state);
  if (filters.major) query.set("major", "true");
  if (filters.keyword) query.set("keyword", filters.keyword);
  if (filters.budgetMax !== undefined) query.set("budgetMax", String(filters.budgetMax));
  if (filters.source) query.set("source", filters.source);
  const body = await apiRequest(`/events?${query}`, {}, signal);
  if (!record(body) || !Array.isArray(body.events) || !body.events.every(isEvent) ||
      !record(body.pagination) || !Number.isInteger(body.pagination.page) || (body.pagination.page as number) < 1 ||
      !Number.isInteger(body.pagination.pageSize) || (body.pagination.pageSize as number) < 1 ||
      typeof body.pagination.hasMore !== "boolean") invalidResponse();
  return body as unknown as EventsResponse;
}

export async function getCityEvents(state: string, city: string, signal: AbortSignal): Promise<EventData[]> {
  const events = new Map<string, EventData>();
  for (let page = 1; ; page++) {
    const response = await getEvents({ state, city, page }, signal);
    for (const event of response.events) {
      if (event.city === city && (!event.state || event.state === state) && event.status === "scheduled") {
        events.set(event.id, event);
      }
    }
    if (!response.pagination.hasMore || !response.events.length) return [...events.values()];
    if (response.pagination.page !== page) throw new ApiError("The server returned an incomplete response. Please try again.");
  }
}

export async function getEvent(id: string, signal?: AbortSignal): Promise<{ event: EventData }> {
  const body = await apiRequest(`/events/${encodeURIComponent(id)}`, {}, signal);
  if (!record(body) || !isEvent(body.event)) invalidResponse();
  return { event: body.event };
}

export async function generatePlan(payload: PlannerRequest, signal?: AbortSignal): Promise<PlannerResponse> {
  const body = await apiRequest("/plan", {
    method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify(payload),
  }, signal);
  if (!isPlannerResponse(body)) invalidResponse();
  return body;
}

export function isPlannerResponse(value: unknown): value is PlannerResponse {
  if (!record(value) || typeof value.planId !== "string" || !value.planId || typeof value.summary !== "string" ||
      !Array.isArray(value.recommendations) || value.recommendations.length > 5 ||
      !value.recommendations.every((item) => record(item) && isEvent(item.event) &&
        item.event.status === "scheduled" && typeof item.matchScore === "number" &&
        Number.isFinite(item.matchScore) && item.matchScore >= 0 && item.matchScore <= 100 &&
        typeof item.reason === "string") ||
      (value.suggestions !== undefined && (!Array.isArray(value.suggestions) ||
        !value.suggestions.every((suggestion) => typeof suggestion === "string")))) return false;
  const recommendations = value.recommendations as PlannerResponse["recommendations"];
  return new Set(recommendations.map(({ event }) => event.id)).size === recommendations.length;
}
