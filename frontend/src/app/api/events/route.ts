import { apiResponse, eventFilters, searchEvents } from "../../../lib/server/serpapi.ts";

export async function GET(request: Request) {
  return apiResponse(() => searchEvents(eventFilters(new URL(request.url).searchParams)));
}
