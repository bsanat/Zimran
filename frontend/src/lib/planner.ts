import type { EventData } from "../types/event.ts";
import type { DatePreset, Interest, PlannerDraft, PlannerRequest, TimePreference } from "../types/planner.ts";
import { translate, type Locale } from "./i18n.ts";

export const INTERESTS: { id: Interest; label: string; icon: string; detail: string; color: string; image?: string }[] = [
  { id: "family", label: "Family", icon: "family", detail: "Time together", color: "lavender", image: "/categories/family.png" },
  { id: "books", label: "Books", icon: "books", detail: "Turn a new page", color: "peach", image: "/categories/books.png" },
  { id: "gaming", label: "Games", icon: "gaming", detail: "Find your players", color: "blue", image: "/categories/gaming.png" },
  { id: "technology", label: "Technology", icon: "technology", detail: "Meet what’s next", color: "sage", image: "/categories/technology.png" },
  { id: "food", label: "Food & Drinks", icon: "food", detail: "Follow your cravings", color: "yellow", image: "/categories/food.png" },
  { id: "ai", label: "AI", icon: "ai", detail: "Ideas for tomorrow", color: "lavender", image: "/categories/ai.png" },
  { id: "running", label: "Running", icon: "running", detail: "Find your stride", color: "peach", image: "/categories/running.png" },
  { id: "art", label: "Arts & Culture", icon: "art", detail: "A fresh perspective", color: "pink", image: "/categories/art.png" },
  { id: "climate", label: "Climate", icon: "climate", detail: "A greener connection", color: "sage", image: "/categories/climate.png" },
  { id: "fitness", label: "Fitness", icon: "fitness", detail: "Move your way", color: "blue", image: "/categories/fitness.png" },
  { id: "wellness", label: "Wellness", icon: "wellness", detail: "A moment for you", color: "pink", image: "/categories/wellness.png" },
  { id: "crypto", label: "Crypto", icon: "crypto", detail: "Explore new networks", color: "yellow", image: "/categories/crypto.png" },
  { id: "music", label: "Music", icon: "music", detail: "Find your rhythm", color: "peach" },
  { id: "sports", label: "Sports", icon: "sports", detail: "Be part of the crowd", color: "blue" },
  { id: "theater", label: "Theater", icon: "theater", detail: "Take in a story", color: "pink" },
  { id: "outdoors", label: "Outdoors", icon: "outdoors", detail: "A little fresh air", color: "sage" },
  { id: "nightlife", label: "Nightlife", icon: "nightlife", detail: "After the sun sets", color: "lavender" },
  { id: "comedy", label: "Comedy", icon: "comedy", detail: "Make room for a laugh", color: "yellow" },
];

export const BUDGETS = [
  { label: "Any budget", value: "any" },
  { label: "Free only", value: "0" },
  { label: "Up to $25", value: "25" },
  { label: "Up to $50", value: "50" },
  { label: "Up to $100", value: "100" },
];

export const TIME_OPTIONS: { label: string; value: TimePreference }[] = [
  { label: "Any time", value: "any" },
  { label: "Morning", value: "morning" },
  { label: "Afternoon", value: "afternoon" },
  { label: "Evening", value: "evening" },
];

export function getCityToday(now = new Date(), timeZone = "America/New_York"): string {
  const parts = new Intl.DateTimeFormat("en-US", {
    timeZone, year: "numeric", month: "2-digit", day: "2-digit",
  }).formatToParts(now);
  const value = (type: string) => parts.find((part) => part.type === type)?.value;
  return `${value("year")}-${value("month")}-${value("day")}`;
}

export function createDefaultDraft(): PlannerDraft {
  return {
    city: "New York", interests: [], datePreset: "weekend", customDate: "",
    budget: "any", timePreference: "any", notes: "",
  };
}

export function addDays(date: string, days: number): string {
  const value = new Date(`${date}T12:00:00Z`);
  value.setUTCDate(value.getUTCDate() + days);
  return value.toISOString().slice(0, 10);
}

export function isDate(value: string): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return false;
  const parsed = new Date(`${value}T12:00:00Z`);
  return !Number.isNaN(parsed.getTime()) && parsed.toISOString().slice(0, 10) === value;
}

export function isPlannerDraft(value: unknown): value is PlannerDraft {
  if (typeof value !== "object" || value === null || Array.isArray(value)) return false;
  const draft = value as Record<string, unknown>;
  return draft.city === "New York" && Array.isArray(draft.interests) && draft.interests.length <= INTERESTS.length &&
    draft.interests.every((id) => INTERESTS.some((interest) => interest.id === id)) &&
    ["today", "tomorrow", "weekend", "custom"].includes(draft.datePreset as string) &&
    typeof draft.customDate === "string" && (draft.customDate === "" || isDate(draft.customDate)) &&
    BUDGETS.some((option) => option.value === draft.budget) &&
    TIME_OPTIONS.some((option) => option.value === draft.timePreference) &&
    typeof draft.notes === "string" && draft.notes.length <= 500;
}

export function getDateRange(preset: DatePreset, customDate = "", now = new Date()) {
  const today = getCityToday(now);
  let startDate = today;
  let endDate = today;
  if (preset === "tomorrow") startDate = endDate = addDays(today, 1);
  if (preset === "custom") startDate = endDate = customDate;
  if (preset === "weekend") {
    const weekday = new Date(`${today}T12:00:00Z`).getUTCDay();
    startDate = weekday === 0 ? today : addDays(today, (6 - weekday + 7) % 7);
    endDate = weekday === 0 ? today : addDays(startDate, 1);
  }
  return { startDate, endDate };
}

export function validateDraft(draft: PlannerDraft, now = new Date()) {
  const errors: Partial<Record<keyof PlannerDraft, string>> = {};
  if (draft.city !== "New York") errors.city = "New York is the supported city for this pilot.";
  if (!Array.isArray(draft.interests) || !draft.interests.length ||
      draft.interests.some((id) => !INTERESTS.some((interest) => interest.id === id))) {
    errors.interests = "Choose at least one interest.";
  }
  if (!["today", "tomorrow", "weekend", "custom"].includes(draft.datePreset)) {
    errors.datePreset = "Choose when you want to go.";
  } else if (draft.datePreset === "custom" &&
      (!isDate(draft.customDate) || draft.customDate < getCityToday(now))) {
    errors.customDate = "Choose today or a future date in New York.";
  }
  if (!BUDGETS.some((option) => option.value === draft.budget)) errors.budget = "Choose a valid budget.";
  if (!TIME_OPTIONS.some((option) => option.value === draft.timePreference)) errors.timePreference = "Choose a valid time of day.";
  if (typeof draft.notes !== "string" || draft.notes.length > 500) errors.notes = "Keep your notes to 500 characters or fewer.";
  return errors;
}

export function toPlannerRequest(draft: PlannerDraft, now = new Date()): PlannerRequest {
  const errors = validateDraft(draft, now);
  if (Object.keys(errors).length) throw new Error(Object.values(errors)[0]);
  return {
    city: draft.city,
    interests: [...new Set(draft.interests)],
    ...getDateRange(draft.datePreset, draft.customDate, now),
    ...(draft.budget === "any" ? {} : { budgetMax: Number(draft.budget) }),
    timePreference: draft.timePreference,
    ...(draft.notes.trim() ? { notes: draft.notes.trim() } : {}),
  };
}

export function formatEventDate(event: Pick<EventData, "startAt" | "timezone" | "dateText">, locale: Locale = "en"): string {
  const date = new Date(event.startAt ?? "");
  if (Number.isNaN(date.getTime())) return event.dateText || translate(locale, "Date and time unavailable");
  const options: Intl.DateTimeFormatOptions = {
    weekday: "short", month: "short", day: "numeric", hour: "numeric", minute: "2-digit", timeZoneName: "short",
  };
  try {
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: event.timezone }).format(date);
  } catch {
    return new Intl.DateTimeFormat(locale, { ...options, timeZone: "UTC" }).format(date);
  }
}

export function formatPrice(event: Pick<EventData, "priceKnown" | "priceMin" | "priceMax" | "currency" | "priceText">, locale: Locale = "en"): string {
  if (!event.priceKnown || event.priceMin === null || !Number.isFinite(event.priceMin) ||
      event.priceMin < 0 || !event.currency || !/^[A-Z]{3}$/.test(event.currency)) return event.priceText || translate(locale, "Price unavailable");
  const format = (value: number) => new Intl.NumberFormat(locale, {
    style: "currency", currency: event.currency!, maximumFractionDigits: Number.isInteger(value) ? 0 : 2,
  }).format(value);
  if (event.priceMin === 0 && event.priceMax === 0) return translate(locale, "Free");
  if (event.priceMax !== null && Number.isFinite(event.priceMax) && event.priceMax > event.priceMin) {
    return `${format(event.priceMin)} – ${format(event.priceMax)}`;
  }
  return event.priceMax === event.priceMin ? format(event.priceMin) : translate(locale, "From {price}").replace("{price}", format(event.priceMin));
}

export function safeExternalUrl(value: string | null, source = "ticketmaster"): string | null {
  if (!value) return null;
  try {
    const url = new URL(value);
    return url.protocol === "https:" && !url.username && !url.password &&
      (!url.port || url.port === "443") &&
      (source === "serpapi" || source === "catalog" || source === "json" || url.hostname === "ticketmaster.com" || url.hostname.endsWith(".ticketmaster.com")) ? url.href : null;
  } catch {
    return null;
  }
}
