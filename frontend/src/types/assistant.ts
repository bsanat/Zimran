import type { PlannerRequest, PlannerResponse } from "./planner.ts";

export type AssistantPendingField = "city" | "interests" | "dates" | "budgetMax" | "timePreference";

export interface AssistantChatRequest {
  message: string;
  preferences?: Partial<PlannerRequest>;
  pendingFields?: AssistantPendingField[];
  conversationId?: string;
}

export interface AssistantChatResponse {
  /** "ai": the Spring backend with Google Gemini; "catalog": the built-in rule-based chat. */
  engine: "catalog" | "ai";
  preferences: Partial<PlannerRequest>;
  pendingFields: AssistantPendingField[];
  clarifications: string[];
  result: PlannerResponse | null;
  conversationId?: string;
}
