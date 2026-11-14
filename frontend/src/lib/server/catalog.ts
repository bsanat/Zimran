import { CATALOG_EVENTS, catalogRegions, filterCatalogEvents, normalizeCategory } from "../catalog.ts";
import { getCityToday, INTERESTS, isDate } from "../planner.ts";
import { ServiceError } from "./serpapi.ts";
import type { EventsResponse } from "../../types/event.ts";

export function searchCatalog(query: URLSearchParams): EventsResponse {
  const state = query.get("state") || "";
  const city = query.get("city")?.trim() || "";
  const category = normalizeCategory(query.get("category") || "");
  const date = query.get("date") || "";
  const keyword = query.get("keyword")?.trim() || "";
  const budgetMax = query.has("budgetMax") ? Number(query.get("budgetMax")) : undefined;
  const page = Number(query.get("page") ?? 1);
  const invalid = (field: string): never => { throw new ServiceError(400, "VALIDATION_ERROR", "Please check your preferences and try again.", field); };
  if (state && !catalogRegions.some(region => region.code === state)) invalid("state");
  if (city && !catalogRegions.some(region => (!state || region.code === state) && region.cities.includes(city))) invalid("city");
  if (category && !INTERESTS.some(interest => interest.id === category)) invalid("category");
  if (date && (!isDate(date) || date < getCityToday())) invalid("date");
  if (keyword.length > 100) invalid("keyword");
  if (budgetMax !== undefined && (!query.get("budgetMax")?.trim() || !Number.isFinite(budgetMax) || budgetMax < 0)) invalid("budgetMax");
  if (!Number.isInteger(page) || page < 1 || page > 100) invalid("page");
  if (query.has("source") && !["catalog", "live"].includes(query.get("source")!)) invalid("source");
  if (query.has("major") && !["true", "false"].includes(query.get("major")!)) invalid("major");
  const events = filterCatalogEvents({ state, city, category, date, keyword, budgetMax }).filter(event => query.get("major") !== "true" || event.isMajor === true);
  const pageSize = 12;
  const offset = (page - 1) * pageSize;
  return { events: events.slice(offset, offset + pageSize), pagination: { page, pageSize, hasMore: offset + pageSize < events.length } };
}

export function catalogEvent(id: string) {
  const event = CATALOG_EVENTS.find(item => item.id === id);
  if (!event) throw new ServiceError(404, "NOT_FOUND", "This event could not be found.");
  return { event };
}
