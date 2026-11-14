import { PairPlanning } from "@/components/social/PairPlanning";
import { pageMetadata } from "@/lib/language-server";
import { catalogRegions } from "@/lib/catalog";

export async function generateMetadata() { return pageMetadata("Plan together", "Two independent sets of preferences, real events, and one shared choice."); }
export default async function PairPage({ params, searchParams }: { params: Promise<{ sessionId: string }>; searchParams: Promise<Record<string, string | string[] | undefined>> }) {
  const { sessionId } = await params;
  const query = await searchParams;
  const city = typeof query.city === "string" && catalogRegions.some(region => region.cities.includes(query.city as string)) ? query.city : "New York";
  return <PairPlanning sessionId={sessionId} inviteToken={typeof query.invite === "string" ? query.invite : ""} initialCity={city} />;
}
