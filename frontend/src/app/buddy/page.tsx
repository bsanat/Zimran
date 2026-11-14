import { Buddy } from "@/components/social/Buddy";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Event Buddy", "Find someone to share a real event with. Try both roles in the guest demo."); }
export default function BuddyPage() { return <Buddy />; }
