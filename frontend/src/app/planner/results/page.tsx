import { pageMetadata } from "@/lib/language-server";
import { PlannerResults } from "@/components/planner/PlannerResults";
export async function generateMetadata() { return pageMetadata("Your personal picks", "Your personal collection of real New York events, with reasons they fit your interests."); }
export default function ResultsPage() { return <div className="container page-section"><PlannerResults /></div>; }
