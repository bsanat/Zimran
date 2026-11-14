import { PairPlanning } from "@/components/social/PairPlanning";
import { pageMetadata } from "@/lib/language-server";
import { catalogRegions } from "@/lib/catalog";

export async function generateMetadata() { return pageMetadata("Plan together", "Two independent sets of preferences, real events, and one shared choice."); }
export default async function NewPairPage({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const query = await searchParams;
  const city = typeof query.city === "string" && catalogRegions.some(region => region.cities.includes(query.city as string)) ? query.city : "New York";
  return <PairPlanning initialCity={city} />;
}
