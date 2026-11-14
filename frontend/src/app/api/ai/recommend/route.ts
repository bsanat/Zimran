import { assistantBody, recommendCatalog } from "../../../../lib/server/assistant.ts";
import { apiResponse } from "../../../../lib/server/serpapi.ts";
import { consumeAssistantSession } from "../../../../lib/server/social.ts";

export async function POST(request: Request) {
  return apiResponse(async () => {
    const result = recommendCatalog(await assistantBody(request));
    if (result.recommendations.length) await consumeAssistantSession(request);
    return result;
  });
}
