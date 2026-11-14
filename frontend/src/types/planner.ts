import type { EventData } from "./event.ts";

export type Interest =
  | "music" | "sports" | "food" | "art" | "theater"
  | "outdoors" | "nightlife" | "technology" | "comedy" | "gaming"
  | "family" | "books" | "ai" | "running" | "climate" | "fitness" | "wellness" | "crypto";
export type TimePreference = "any" | "morning" | "afternoon" | "evening";
export type DatePreset = "today" | "tomorrow" | "weekend" | "custom";

export interface PlannerDraft {
  city: string;
  interests: Interest[];
  datePreset: DatePreset;
  customDate: string;
  budget: string;
  timePreference: TimePreference;
  notes: string;
}

export interface PlannerRequest {
  city: string;
  interests: Interest[];
  startDate: string;
  endDate: string;
  budgetMax?: number;
  timePreference: TimePreference;
  notes?: string;
}

export interface Recommendation {
  event: EventData;
  matchScore: number;
  reason: string;
}

export interface PlannerResponse {
  planId: string;
  summary: string;
  recommendations: Recommendation[];
  suggestions?: string[];
}

export interface ErrorResponse {
  error: {
    code: string;
    message: string;
    details?: { field?: string; [key: string]: unknown };
  };
}
