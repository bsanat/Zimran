"use client";
import { useLanguage } from "@/components/LanguageProvider";
import Link from "next/link";
import { Icon } from "@/components/Icon";

export function LoadingState({ label = "Finding your next good thing…" }: { label?: string }) {
  const { t } = useLanguage();
  return <div role="status" className="loading-area"><span className="loader" /><p>{t(label)}</p><div className="skeleton-grid" aria-hidden="true">{[1, 2, 3].map(i => <div className="skeleton-card" key={i}><div /><span /><span /></div>)}</div></div>;
}

export function ErrorState({ message, onRetry, title = "A little pause in the plans" }: { message: string; onRetry?: () => void; title?: string }) {
  const { t } = useLanguage();
  return <div className="state-panel" role="alert"><span className="state-icon"><Icon name="alert" size={28} /></span><h2>{t(title)}</h2><p>{t(message)}</p>{onRetry && <button className="button button-primary" onClick={onRetry}>{t("Try again")} <Icon name="arrow" size={17} /></button>}<Link href="/planner" className="text-link">{t("Back to your preferences")}</Link></div>;
}

export function EmptyState({ title = "Let’s try a different kind of day", message = "No matching events this time. Try another date, a wider budget, or a few more interests.", href = "/planner", action = "Edit preferences" }: { title?: string; message?: string; href?: string; action?: string }) {
  const { t } = useLanguage();
  return <div className="state-panel"><span className="state-icon"><Icon name="search" size={30} /></span><h2>{t(title)}</h2><p>{t(message)}</p><Link className="button button-primary" href={href}>{t(action)} <Icon name="arrow" size={17} /></Link></div>;
}
