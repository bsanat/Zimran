import { pageMetadata } from "@/lib/language-server";
import { EventDetails } from "@/components/events/EventDetails";
export async function generateMetadata() { return pageMetadata("Event details", "Event time, venue, prices, and details from the original event source."); }
export default async function EventPage({ params }: { params: Promise<{ id: string }> }) { const { id } = await params; return <div className="container page-section"><EventDetails id={decodeURIComponent(id)} /></div>; }
