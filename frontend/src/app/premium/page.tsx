import { Premium } from "@/components/Premium";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Outly Premium", "Find company, plan together, and get personal event alerts."); }
export default async function PremiumPage({ searchParams }: { searchParams: Promise<{ returnTo?: string }> }) {
  const { returnTo } = await searchParams;
  return <Premium returnTo={typeof returnTo === "string" && /^\/(assistant(?:\?|$)|buddy(?:\/|\?|$)|pair\/|settings\/alerts(?:\?|$))/.test(returnTo) ? returnTo : undefined} />;
}
