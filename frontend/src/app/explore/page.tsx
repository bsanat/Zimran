import { pageMetadata } from "@/lib/language-server";
import { ExploreEvents } from "@/components/events/ExploreEvents";
import { INTERESTS } from "@/lib/planner";
import { catalogRegions, normalizeCategory } from "@/lib/catalog";

export async function generateMetadata() { return pageMetadata("Explore events by city", "Browse verified events across the USA by category, state and city."); }
export default async function ExplorePage({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const query = await searchParams;
  const source = query.source === "live" ? "live" : "catalog";
  const state = catalogRegions.find(state => state.code === (source === "live" ? "NY" : typeof query.state === "string" ? query.state.toUpperCase() : ""));
  const city = source === "live" ? "New York" : typeof query.city === "string" && state?.cities.includes(query.city) ? query.city : "";
  const category = INTERESTS.find(interest => interest.id === normalizeCategory(typeof query.category === "string" ? query.category : ""))?.id ?? "";
  const keyword = typeof query.q === "string" ? query.q.trim().slice(0, 100) : "";
  return <div className="container page-section"><ExploreEvents key={`${source}:${state?.code}:${city}:${category}:${keyword}`} initialState={state?.code ?? ""} initialCity={city} initialCategory={category} initialQuery={keyword} source={source} /></div>;
}
