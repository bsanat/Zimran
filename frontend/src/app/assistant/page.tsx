import { Assistant } from "@/components/assistant/Assistant";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Event Assistant", "Describe your perfect day and find real events from the verified catalog."); }
export default function AssistantPage() { return <Assistant />; }
