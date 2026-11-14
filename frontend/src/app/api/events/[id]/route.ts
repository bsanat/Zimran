import { apiResponse, eventDetails } from "../../../../lib/server/serpapi.ts";
import { catalogEvent } from "../../../../lib/server/catalog.ts";

export async function GET(_request: Request, { params }: { params: Promise<{ id: string }> }) {
  return apiResponse(async () => {
    const { id } = await params;
    return id.startsWith("catalog:") ? catalogEvent(id) : eventDetails(id);
  });
}
