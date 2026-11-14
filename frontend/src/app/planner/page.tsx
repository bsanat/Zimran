import { pageMetadata } from "@/lib/language-server";
import { PlannerWizard } from "@/components/planner/PlannerWizard";
export async function generateMetadata() { return pageMetadata("Plan your day", "Choose your New York interests, dates, and budget. Get personal event recommendations without an account."); }
export default function PlannerPage() { return <div className="container planner-container"><PlannerWizard /></div>; }
