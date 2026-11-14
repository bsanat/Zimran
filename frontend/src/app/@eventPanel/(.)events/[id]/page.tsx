import { EventDrawer } from "@/components/events/EventDrawer";

export default async function EventOverlay({ params }: { params: Promise<{ id: string }> }) {
  const { id } = await params;
  return <EventDrawer id={decodeURIComponent(id)} />;
}
