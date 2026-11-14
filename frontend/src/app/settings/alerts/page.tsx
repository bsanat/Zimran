import { AlertSettings } from "@/components/alerts/AlertSettings";
import { pageMetadata } from "@/lib/language-server";

export async function generateMetadata() { return pageMetadata("Notification settings", "Choose your city and interests for personal event alerts."); }
export default function AlertsPage() { return <AlertSettings />; }
