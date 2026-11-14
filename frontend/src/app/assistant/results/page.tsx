import { Assistant } from "@/components/assistant/Assistant";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Your personal picks", "Describe your perfect day and find real events from the verified catalog."); }
export default function AssistantResultsPage() { return <Assistant />; }
