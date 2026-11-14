import assert from "node:assert/strict";
import test from "node:test";
import { isAssistantChatResponse, restoreAssistantTurns } from "../src/lib/assistant-chat.ts";

const reply = { engine: "catalog", preferences: { city: "New York", interests: ["gaming"], timePreference: "any" }, pendingFields: ["dates"], clarifications: ["When would you like to go? Tell me a date or date range."], result: null };

test("chat validates partial responses and rejects damaged event data before rendering", () => {
  assert.equal(isAssistantChatResponse(reply), true);
  for (const invalid of [null, { ...reply, preferences: { budgetMax: -1 } }, { ...reply, preferences: { startDate: "2026-02-30" } }, { ...reply, preferences: { interests: ["invented"] } }, { ...reply, pendingFields: ["unknown"] }, { ...reply, result: { recommendations: [{ event: { id: "incomplete" } }] } }, { ...reply, conversationId: "bad" }]) assert.equal(isAssistantChatResponse(invalid), false);
});

test("saved chat preserves context and offers retry for interrupted messages without replaying them", () => {
  const turns = restoreAssistantTurns([{ id: "one", message: "Games in New York", response: reply }, { id: "two", message: "Thursday" }]);
  assert.equal(turns[0].response?.preferences.city, "New York");
  assert.equal(turns[1].message, "Thursday");
  assert.equal(turns[1].error, "This message did not finish. Please try again.");
  for (const invalid of [null, {}, [{ id: "one", message: " ", response: reply }], [{ id: "one", message: "Games", response: { invalid: true } }], Array(31).fill({ id: "one", message: "Games" })]) assert.deepEqual(restoreAssistantTurns(invalid), []);
});
