import { apiResponse, eventFilters, searchEvents } from "../../../lib/server/serpapi.ts";
import { searchCatalog } from "../../../lib/server/catalog.ts";

export async function GET(request: Request) {
  const query = new URL(request.url).searchParams;
  return apiResponse(async () => query.get("source") === "live" ? searchEvents(eventFilters(query)) : searchCatalog(query));
}
