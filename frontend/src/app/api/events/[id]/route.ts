import { apiResponse, eventDetails } from "../../../../lib/server/serpapi.ts";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  return apiResponse(async () => eventDetails((await params).id));
}
