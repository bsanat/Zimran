"use client";
import { ErrorState } from "@/components/States";
export default function ErrorPage({ reset }: { error: Error & { digest?: string }; reset: () => void }) {
  return <div className="container page-section"><ErrorState message="We couldn’t open this page. Please try again." onRetry={reset} /></div>;
}
