import { apiResponse, planEvents, ServiceError } from "../../../lib/server/serpapi.ts";

export async function POST(request: Request) {
  return apiResponse(async () => {
    const body = await request.text();
    if (body.length > 10_000) throw new ServiceError(413, "PAYLOAD_TOO_LARGE", "Please check your preferences and try again.");
    let value: unknown;
    try { value = JSON.parse(body); } catch { throw new ServiceError(400, "VALIDATION_ERROR", "Please check your preferences and try again.", "body"); }
    return planEvents(value);
  });
}
