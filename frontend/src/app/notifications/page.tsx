import { Notifications } from "@/components/alerts/Notifications";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Notification center", "New events that match your interests, all in one place."); }
export default function NotificationsPage() { return <Notifications />; }
