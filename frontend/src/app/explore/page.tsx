import { pageMetadata } from "@/lib/language-server";
import { ExploreEvents } from "@/components/events/ExploreEvents";
import { INTERESTS } from "@/lib/planner";
import { getStateByCode } from "@/lib/locations";

export async function generateMetadata() { return pageMetadata("Explore events by city", "Explore event categories, states, and cities. Discover real events in our New York pilot."); }
export default async function ExplorePage({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const query = await searchParams;
  const state = getStateByCode(typeof query.state === "string" ? query.state.toUpperCase() : "NY") ?? getStateByCode("NY")!;
  const city = typeof query.city === "string" && state.cities.includes(query.city) ? query.city : state.cities[0];
  const category = INTERESTS.find(interest => interest.id === query.category)?.id ?? "";
  const keyword = typeof query.q === "string" ? query.q.trim().slice(0, 100) : "";
  return <div className="container page-section"><ExploreEvents key={`${state.code}:${city}:${category}:${keyword}`} initialState={state.code} initialCity={city} initialCategory={category} initialQuery={keyword} /></div>;
}
