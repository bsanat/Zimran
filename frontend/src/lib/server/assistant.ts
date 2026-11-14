import { randomUUID } from "node:crypto";
import { CATALOG_EVENTS, catalogCategories, catalogMatchesDate } from "../catalog.ts";
import { addDays, getCityToday, getDateRange, INTERESTS, isDate, TIME_OPTIONS } from "../planner.ts";
import type { EventData } from "../../types/event.ts";
import type { Interest, PlannerRequest, PlannerResponse } from "../../types/planner.ts";
import type { AssistantChatResponse, AssistantPendingField } from "../../types/assistant.ts";
import { ServiceError } from "./serpapi.ts";

const cities = [...new Set(CATALOG_EVENTS.map(event => event.city))];
const synonyms: Record<Interest, string[]> = {
  music: ["jazz", "concert", "concerts", "live music"],
  sports: ["sport", "basketball", "football"],
  food: ["food", "drinks", "restaurant"],
  art: ["art", "museum", "museums", "gallery"],
  theater: ["theatre", "musical"],
  outdoors: ["outdoor", "nature", "park", "hiking"],
  nightlife: ["club", "clubbing", "nightclub"],
  technology: ["tech", "startup", "software"],
  comedy: ["standup", "stand up", "comedy"],
  gaming: ["games", "board games", "chess"],
  family: ["kids", "children", "family"],
  books: ["book", "reading", "literature"],
  ai: ["artificial intelligence"],
  running: ["run", "jogging"],
  climate: ["environment", "sustainability"],
  fitness: ["gym", "workout"],
  wellness: ["yoga", "meditation"],
  crypto: ["cryptocurrency", "bitcoin"],
};
const weekdays = [
  ["sunday", "sun"],
  ["monday", "mon"],
  ["tuesday", "tue"],
  ["wednesday", "wed"],
  ["thursday", "thu"],
  ["friday", "fri"],
  ["saturday", "sat"],
];

function normalize(value: string) { return value.toLocaleLowerCase("en-US").replace(/[‐‑–—-]/g, " ").replace(/\s+/g, " ").trim(); }
function contains(message: string, term: string) {
  const escaped = normalize(term).replace(/[.*+?^${}()|[\]\\]/g, "\\$&");
  return new RegExp(`(?<![\\p{L}\\p{N}])${escaped}(?![\\p{L}\\p{N}])`, "u").test(message);
}
function invalid(field: string, message = "Please check your preferences and try again."): never {
  throw new ServiceError(400, "VALIDATION_ERROR", message, field);
}

export function parseAssistant(value: unknown, now = new Date()) {
  if (!value || typeof value !== "object" || !("message" in value) || typeof value.message !== "string" || !value.message.trim() || value.message.length > 500) {
    invalid("message", "Describe your plan in 1 to 500 characters.");
  }
  const raw = value.message;
  const message = normalize(raw);
  const clarifications: string[] = [];
  const detected: Partial<PlannerRequest> = { notes: raw.trim(), timePreference: "any" };
  const cityMatches = cities.filter(city =>
    [city, ...(city === "New York" ? ["NYC", "New York City"] : [])].some(term => contains(message, term)));
  if (cityMatches.length === 1) detected.city = cityMatches[0];
  else clarifications.push(cityMatches.length ? "Choose one city for this search." : "Which city? Choose a city from the verified catalog.");

  detected.interests = INTERESTS.filter(interest => {
    const keywords = catalogCategories.find(category => category.id === interest.id)?.keywords ?? [];
    return [interest.id, ...keywords, ...synonyms[interest.id]].some(term => contains(message, term));
  }).map(interest => interest.id);
  if (!detected.interests.length) clarifications.push("Choose at least one interest below.");

  const explicitDates = [...raw.matchAll(/\b\d{4}-\d{2}-\d{2}\b/g)].map(match => match[0]);
  const relativeDates: { startDate: string; endDate: string }[] = [];
  const today = getCityToday(now);
  if (["today", "tonight"].some(term => contains(message, term))) relativeDates.push({ startDate: today, endDate: today });
  if (["tomorrow"].some(term => contains(message, term))) relativeDates.push({ startDate: addDays(today, 1), endDate: addDays(today, 1) });
  if (["weekend"].some(term => contains(message, term))) {
    const weekend = getDateRange("weekend", "", now);
    const next = ["next weekend"].some(term => contains(message, term));
    relativeDates.push(next ? { startDate: addDays(weekend.startDate, 7), endDate: addDays(weekend.endDate, 7) } : weekend);
  }
  weekdays.forEach((terms, index) => {
    if (!terms.some(term => contains(message, term))) return;
    const weekday = new Date(`${today}T12:00:00Z`).getUTCDay();
    let distance = (index - weekday + 7) % 7;
    if (!distance && ["next"].some(term => contains(message, term))) distance = 7;
    const date = addDays(today, distance);
    relativeDates.push({ startDate: date, endDate: date });
  });
  if (explicitDates.length && explicitDates.length <= 2 && explicitDates.every(isDate) && explicitDates[0] <= explicitDates.at(-1)!) {
    detected.startDate = explicitDates[0]; detected.endDate = explicitDates.at(-1)!;
    if (relativeDates.some(range => range.startDate !== detected.startDate || range.endDate !== detected.endDate)) clarifications.push("Your dates differ. Confirm the date range below.");
  } else if (!explicitDates.length && relativeDates.length === 1) Object.assign(detected, relativeDates[0]);
  else clarifications.push(explicitDates.length || relativeDates.length ? "Your dates differ. Confirm the date range below." : "When would you like to go? Set a date or date range below.");
  if (detected.startDate && detected.startDate < today) clarifications.push("Choose today or a future date in New York.");

  const amounts = [...raw.matchAll(/(?:-\s*)?\$\s*(-?\d+(?:[.,]\d{1,2})?)|(?:under|up to|budget(?: of)?)\s*\$?\s*(-?\d+(?:[.,]\d{1,2})?)|(-?\d+(?:[.,]\d{1,2})?)\s*(?:dollars?|usd)(?!\p{L})/giu)].map(match => Number((match[1] ?? match[2] ?? match[3]).replace(",", ".")) * (/^-\s*\$/.test(match[0]) ? -1 : 1));
  const free = ["free"].some(term => contains(message, term));
  const budgets = [...new Set([...amounts, ...(free ? [0] : [])])];
  if (budgets.length === 1 && budgets[0] >= 0) detected.budgetMax = budgets[0];
  else if (budgets.length) clarifications.push("Choose one maximum budget in USD.");
  if (/[€£₸₽]/u.test(raw)) clarifications.push("Choose one maximum budget in USD.");

  const timeMatches = [
    { value: "morning" as const, terms: ["morning"] },
    { value: "afternoon" as const, terms: ["afternoon"] },
    { value: "evening" as const, terms: ["evening", "tonight"] },
  ].filter(option => option.terms.some(term => contains(message, term)));
  if (timeMatches.length === 1) detected.timePreference = timeMatches[0].value;
  else if (timeMatches.length > 1) clarifications.push("Choose one time of day, or keep any time.");
  const questions = [...new Set(clarifications)];
  return { engine: "catalog" as const, preferences: questions.length ? null : validateAssistantPreferences(detected, now), detected, clarifications: questions };
}

export function validateAssistantPreferences(value: unknown, now = new Date()): PlannerRequest {
  if (!value || typeof value !== "object" || Array.isArray(value)) invalid("body");
  const input = value as Record<string, unknown>;
  if (typeof input.city !== "string" || !cities.includes(input.city)) invalid("city");
  if (!Array.isArray(input.interests) || !input.interests.length || input.interests.length > INTERESTS.length || !input.interests.every(id => INTERESTS.some(interest => interest.id === id))) invalid("interests");
  if (typeof input.startDate !== "string" || !isDate(input.startDate) || input.startDate < getCityToday(now)) invalid("startDate");
  if (typeof input.endDate !== "string" || !isDate(input.endDate) || input.endDate < input.startDate || input.endDate > addDays(input.startDate, 366)) invalid("endDate");
  if (!TIME_OPTIONS.some(option => option.value === input.timePreference)) invalid("timePreference");
  if (input.budgetMax !== undefined && (typeof input.budgetMax !== "number" || !Number.isFinite(input.budgetMax) || input.budgetMax < 0)) invalid("budgetMax");
  if (input.notes !== undefined && (typeof input.notes !== "string" || input.notes.length > 500)) invalid("notes");
  return {
    city: input.city, interests: [...new Set(input.interests as Interest[])], startDate: input.startDate, endDate: input.endDate,
    timePreference: input.timePreference as PlannerRequest["timePreference"],
    ...(input.budgetMax === undefined ? {} : { budgetMax: input.budgetMax as number }),
    ...(typeof input.notes === "string" && input.notes.trim() ? { notes: input.notes.trim() } : {}),
  };
}

export function recommendCatalog(value: unknown, now = new Date(), events: readonly EventData[] = CATALOG_EVENTS): PlannerResponse {
  const input = validateAssistantPreferences(value, now);
  const dates: string[] = [];
  for (let date = input.startDate; date <= input.endDate; date = addDays(date, 1)) dates.push(date);
  const words = normalize(input.notes ?? "").match(/[\p{L}]{4,}/gu) ?? [];
  const candidates = events.filter(event => {
    if (event.status !== "scheduled" || event.city !== input.city || !input.interests.includes(event.category as Interest) || !dates.some(date => catalogMatchesDate(event, date))) return false;
    if (input.budgetMax !== undefined && (!event.priceKnown || event.currency !== "USD" || event.priceMin === null || event.priceMin > input.budgetMax)) return false;
    if (input.timePreference === "any") return true;
    if (!event.startAt || !/T.*(?:Z|[+-]\d{2}:\d{2})$/.test(event.startAt) || !Number.isFinite(new Date(event.startAt).getTime())) return false;
    let hour: number;
    try { hour = Number(new Intl.DateTimeFormat("en-US", { timeZone: event.timezone, hour: "numeric", hourCycle: "h23" }).format(new Date(event.startAt))); } catch { return false; }
    return input.timePreference === "morning" ? hour < 12 : input.timePreference === "afternoon" ? hour >= 12 && hour < 17 : hour >= 17;
  }).map(event => ({ event, weight: words.filter(word => normalize(`${event.title} ${event.description ?? ""}`).includes(word)).length }));
  candidates.sort((a, b) => b.weight - a.weight || a.event.title.localeCompare(b.event.title));
  return {
    planId: randomUUID(), summary: "Real catalog events that match your selected filters.",
    recommendations: candidates.slice(0, 5).map(({ event, weight }) => ({ event, matchScore: Math.min(100, 80 + weight * 5), reason: "Matches your interests, city and confirmed date filters." })),
    suggestions: ["Specific times and prices must be confirmed by the source. Unknown prices never pass a budget limit.", "Free-text notes help sort catalog descriptions. Check specific wishes with the organizer.", ...(candidates.length ? [] : ["Try any time, a wider date range or another interest."])],
  };
}

export function validateAssistantContext(value: unknown): Partial<PlannerRequest> {
  if (value === undefined) return {};
  if (!value || typeof value !== "object" || Array.isArray(value)) invalid("preferences");
  const input = value as Record<string, unknown>;
  if (Object.keys(input).some(key => !["city", "interests", "startDate", "endDate", "budgetMax", "timePreference", "notes"].includes(key))) invalid("preferences");
  if (input.city !== undefined && (typeof input.city !== "string" || !cities.includes(input.city))) invalid("city");
  if (input.interests !== undefined && (!Array.isArray(input.interests) || input.interests.length > INTERESTS.length || !input.interests.every(id => INTERESTS.some(interest => interest.id === id)))) invalid("interests");
  for (const key of ["startDate", "endDate"] as const) {
    if (input[key] !== undefined && (typeof input[key] !== "string" || !isDate(input[key]))) invalid(key);
  }
  if (typeof input.startDate === "string" && typeof input.endDate === "string" && (input.endDate < input.startDate || input.endDate > addDays(input.startDate, 366))) invalid("endDate");
  if (input.budgetMax !== undefined && (typeof input.budgetMax !== "number" || !Number.isFinite(input.budgetMax) || input.budgetMax < 0)) invalid("budgetMax");
  if (input.timePreference !== undefined && !TIME_OPTIONS.some(option => option.value === input.timePreference)) invalid("timePreference");
  if (input.notes !== undefined && (typeof input.notes !== "string" || input.notes.length > 500)) invalid("notes");
  return { ...input, ...(Array.isArray(input.interests) ? { interests: [...new Set(input.interests)] as Interest[] } : {}) } as Partial<PlannerRequest>;
}

const chatQuestions: Record<AssistantPendingField, string> = {
  city: "Which city would you like to explore?",
  interests: "What would you like to do? Try music, games, art or another interest.",
  dates: "When would you like to go? Tell me a date or date range.",
  budgetMax: "What is your maximum budget in USD? You can also say any budget.",
  timePreference: "Which time of day do you prefer? You can also say any time.",
};

export function chatAssistant(value: unknown, now = new Date()): AssistantChatResponse {
  if (!value || typeof value !== "object" || Array.isArray(value)) invalid("body");
  const input = value as Record<string, unknown>;
  const preferences = validateAssistantContext(input.preferences);
  const fields: AssistantPendingField[] = ["city", "interests", "dates", "budgetMax", "timePreference"];
  if (input.pendingFields !== undefined && (!Array.isArray(input.pendingFields) || input.pendingFields.length > fields.length || input.pendingFields.some(field => !fields.includes(field as AssistantPendingField)))) invalid("pendingFields");
  if (input.conversationId !== undefined && (typeof input.conversationId !== "string" || !/^[a-f0-9]{8}-[a-f0-9]{4}-4[a-f0-9]{3}-[89ab][a-f0-9]{3}-[a-f0-9]{12}$/i.test(input.conversationId))) invalid("conversationId");
  const pending = new Set(input.pendingFields as AssistantPendingField[] | undefined);
  const parsed = parseAssistant({ message: input.message }, now);
  const message = normalize(input.message as string);
  const raw = input.message as string;
  const has = (terms: string[]) => terms.some(term => contains(message, term));
  let changed = false;
  function clear(field: AssistantPendingField) {
    pending.add(field); changed = true;
    if (field === "dates") { delete preferences.startDate; delete preferences.endDate; }
    else delete preferences[field];
  }

  const unsupportedCity = has(["London", "Paris", "Almaty"]);
  const unknownCityChange = !parsed.detected.city && (has(["another city", "different city"]) || /\bcity\s*[:=]\s*\p{L}/u.test(message) || /\bin\s+(?!the\b|a\b|an\b|\d)(\p{L}+)/u.test(message) && !has(["morning", "afternoon", "evening", "weekend"]));
  if (unsupportedCity || unknownCityChange || parsed.clarifications.includes("Choose one city for this search.")) clear("city");
  else if (parsed.detected.city) { preferences.city = parsed.detected.city; pending.delete("city"); changed = true; }
  if (parsed.detected.interests?.length) { preferences.interests = parsed.detected.interests; pending.delete("interests"); changed = true; }

  const ambiguousDates = parsed.clarifications.includes("Your dates differ. Confirm the date range below.") || parsed.clarifications.includes("Choose today or a future date in New York.");
  const unknownDateChange = !parsed.detected.startDate && has(["any date", "any day", "next week", "next month"]);
  if (ambiguousDates || unknownDateChange) clear("dates");
  else if (parsed.detected.startDate && parsed.detected.endDate) {
    preferences.startDate = parsed.detected.startDate; preferences.endDate = parsed.detected.endDate; pending.delete("dates"); changed = true;
  }
  if (preferences.startDate && preferences.startDate < getCityToday(now) || preferences.endDate && preferences.endDate < getCityToday(now)) clear("dates");
  const anyBudget = has(["any budget", "any price", "no budget limit", "unlimited budget"]);
  if (parsed.clarifications.includes("Choose one maximum budget in USD.") || anyBudget && parsed.detected.budgetMax !== undefined) clear("budgetMax");
  else if (anyBudget) { delete preferences.budgetMax; pending.delete("budgetMax"); changed = true; }
  else if (parsed.detected.budgetMax !== undefined) { preferences.budgetMax = parsed.detected.budgetMax; pending.delete("budgetMax"); changed = true; }

  const anyTime = has(["any time", "anytime", "time doesn't matter"]);
  const specificTime = parsed.detected.timePreference !== undefined && parsed.detected.timePreference !== "any";
  if (parsed.clarifications.includes("Choose one time of day, or keep any time.") || anyTime && specificTime) clear("timePreference");
  else if (anyTime || specificTime) { preferences.timePreference = parsed.detected.timePreference; pending.delete("timePreference"); changed = true; }
  else if (!preferences.timePreference && !pending.has("timePreference")) preferences.timePreference = "any";
  preferences.notes = raw.trim();
  if (!preferences.city) pending.add("city");
  if (!preferences.interests?.length) pending.add("interests");
  if (!preferences.startDate || !preferences.endDate) pending.add("dates");
  const pendingFields = fields.filter(field => pending.has(field));
  const clarifications = pendingFields.map(field => chatQuestions[field]);
  if (!changed && !clarifications.length && !has(["show events", "find events", "find my plan", "show me"])) {
    clarifications.push("Tell me what to change: city, interests, date, budget or time of day.");
  }
  return {
    engine: "catalog", preferences, pendingFields, clarifications,
    result: clarifications.length ? null : recommendCatalog(preferences, now),
    ...(typeof input.conversationId === "string" ? { conversationId: input.conversationId } : {}),
  };
}

export async function assistantBody(request: Request): Promise<unknown> {
  const body = await request.text();
  if (body.length > 10_000) throw new ServiceError(413, "PAYLOAD_TOO_LARGE", "Describe your plan in 1 to 500 characters.");
  try { return JSON.parse(body); } catch { return invalid("body"); }
}
