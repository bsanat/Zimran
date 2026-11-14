import { EmptyState } from "@/components/States";
export default function NotFound() { return <div className="container page-section"><EmptyState title="This one wandered off" message="We couldn’t find that page. There’s still plenty to explore." href="/explore" action="Explore events" /></div>; }
