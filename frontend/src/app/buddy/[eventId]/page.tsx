import { Buddy } from "@/components/social/Buddy";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Event Buddy", "Find someone to share a real event with. Try both roles in the guest demo."); }
export default async function BuddyEventPage({ params }: { params: Promise<{ eventId: string }> }) { return <Buddy eventId={decodeURIComponent((await params).eventId)} />; }
