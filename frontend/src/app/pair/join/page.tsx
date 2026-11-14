import { PairPlanning } from "@/components/social/PairPlanning";
import { pageMetadata } from "@/lib/language-server";
import { catalogRegions } from "@/lib/catalog";

export async function generateMetadata() { return pageMetadata("Plan together", "Two independent sets of preferences, real events, and one shared choice."); }
// Partner link: /pair/join?city=…#invite=… (the backend builds the same path). The invite is read on the client.
export default async function JoinPairPage({ searchParams }: { searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const query = await searchParams;
  const city = typeof query.city === "string" && catalogRegions.some(region => region.cities.includes(query.city as string)) ? query.city : "New York";
  return <PairPlanning initialCity={city} joinFromHash />;
}
