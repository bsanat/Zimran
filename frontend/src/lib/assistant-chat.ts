import { isPlannerResponse } from "./api.ts";
import { INTERESTS, isDate, TIME_OPTIONS } from "./planner.ts";
import type { AssistantChatResponse } from "../types/assistant.ts";

export interface AssistantTurn {
  id: string;
  message: string;
  response?: AssistantChatResponse;
  error?: string;
  errorCode?: string;
}

const record = (value: unknown): value is Record<string, unknown> => typeof value === "object" && value !== null && !Array.isArray(value);

export function isAssistantChatResponse(value: unknown): value is AssistantChatResponse {
  if (!record(value) || value.engine !== "catalog" || !record(value.preferences) ||
    !Array.isArray(value.clarifications) || value.clarifications.length > 10 || !value.clarifications.every(item => typeof item === "string" && item.length <= 500) ||
    !(value.result === null || isPlannerResponse(value.result)) ||
    (value.conversationId !== undefined && (typeof value.conversationId !== "string" || !/^[a-zA-Z0-9_-]{16,128}$/.test(value.conversationId))) ||
    !Array.isArray(value.pendingFields) || !value.pendingFields.every(item => ["city", "interests", "dates", "budgetMax", "timePreference"].includes(String(item)))) return false;
  const input = value.preferences;
  return (input.city === undefined || typeof input.city === "string" && input.city.length <= 100) &&
    (input.interests === undefined || Array.isArray(input.interests) && input.interests.length <= INTERESTS.length && input.interests.every(id => INTERESTS.some(interest => interest.id === id))) &&
    ["startDate", "endDate"].every(key => input[key] === undefined || typeof input[key] === "string" && isDate(input[key])) &&
    (input.budgetMax === undefined || typeof input.budgetMax === "number" && Number.isFinite(input.budgetMax) && input.budgetMax >= 0) &&
    (input.timePreference === undefined || TIME_OPTIONS.some(option => option.value === input.timePreference)) &&
    (input.notes === undefined || typeof input.notes === "string" && input.notes.length <= 500);
}

export function restoreAssistantTurns(value: unknown): AssistantTurn[] {
  if (!Array.isArray(value) || value.length > 30 || !value.every(turn => record(turn) && typeof turn.id === "string" && turn.id.length <= 100 &&
    typeof turn.message === "string" && turn.message.trim().length > 0 && turn.message.length <= 500 &&
    (turn.response === undefined || isAssistantChatResponse(turn.response)) &&
    (turn.error === undefined || typeof turn.error === "string" && turn.error.length <= 1000) &&
    (turn.errorCode === undefined || typeof turn.errorCode === "string" && turn.errorCode.length <= 100))) return [];
  return (value as AssistantTurn[]).map(turn => turn.response || turn.error ? turn : { ...turn, error: "This message did not finish. Please try again." });
}
