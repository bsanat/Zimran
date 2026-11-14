import { assistantBody, chatAssistant } from "../../../../lib/server/assistant.ts";
import { apiResponse, ServiceError } from "../../../../lib/server/serpapi.ts";
import { consumeAssistantChat } from "../../../../lib/server/social.ts";

// shortcut: Limits apply to one process; use a shared limiter before running multiple instances.
const requests = new Map<string, { start: number; count: number }>();

export async function POST(request: Request) {
  return apiResponse(async () => {
    const key = request.headers.get("x-forwarded-for")?.split(",")[0].trim() || "local";
    const now = Date.now();
    for (const [ip, window] of requests) if (now - window.start >= 60_000) requests.delete(ip);
    const window = requests.get(key) ?? { start: now, count: 0 };
    if (window.count >= 30 || requests.size >= 1000 && !requests.has(key)) throw new ServiceError(429, "RATE_LIMITED", "Too many requests. Please wait a moment before trying again.");
    window.count++; requests.set(key, window);
    const result = chatAssistant(await assistantBody(request));
    if (result.result?.recommendations.length) result.conversationId = await consumeAssistantChat(request, result.conversationId);
    return result;
  });
}
