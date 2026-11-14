"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useDemo } from "@/components/DemoProvider";
import { useLanguage } from "@/components/LanguageProvider";
import { Icon } from "@/components/Icon";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { demoRequest } from "@/lib/demo-api";
import { INTERESTS } from "@/lib/planner";
import type { AlertPreferences, NotificationFeed } from "@/types/social";

export function Notifications() {
  const { t, locale } = useLanguage();
  const { subscription, loading: sessionLoading, error: sessionError, refresh, refreshNotifications } = useDemo();
  const premium = subscription?.status === "DEMO_PREMIUM";
  const [feed, setFeed] = useState<NotificationFeed>({ items: [], unreadCount: 0 });
  const [preferences, setPreferences] = useState<AlertPreferences | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [busy, setBusy] = useState(false);
  const [unreadOnly, setUnreadOnly] = useState(false);
  const load = useCallback(async () => {
    try {
      const [response, settings] = await Promise.all([demoRequest<NotificationFeed>("notifications"), demoRequest<{ preferences: AlertPreferences | null }>("alerts/preferences")]);
      setFeed(response); setPreferences(settings.preferences); setLoadError("");
    } catch (cause) { setLoadError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    if (!premium) return;
    void load();
    const timer = setInterval(() => { if (!document.hidden) void load(); }, 15_000);
    return () => clearInterval(timer);
  }, [premium, load]);
  async function action(path: string, method: string, body?: unknown) {
    setBusy(true); setError(""); setNotice("");
    try {
      const response = await demoRequest<NotificationFeed & { created?: number; pending?: number }>(path, { method, ...(body ? { body: JSON.stringify(body) } : {}) });
      if (path === "alerts/check") {
        setFeed(response);
        setNotice(response.created ? t("New matching demo alerts are ready.") : response.pending ? t("Matching events are waiting for your next digest.") : t("No new matching events. Your filters and previously checked events are respected."));
      } else await load();
      await refreshNotifications();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  const items = feed.items.filter(item => !unreadOnly || !item.readAt);
  return <div className="container page-section member-shell"><div className="page-heading member-title-row"><div><span className="eyebrow"><Icon name="bell" size={16} />{t("PREMIUM EVENT ALERTS")}</span><h1>{t("Your next good reason to go out.")}</h1><p>{t("New events that match your interests, all in one place.")}</p></div><Link className="button button-outline" href="/settings/alerts">{t("Notification settings")}</Link></div>
    {sessionLoading ? <LoadingState label={t("Checking demo access…")} /> : sessionError ? <ErrorState message={t(sessionError)} onRetry={() => void refresh()} /> : !premium ? <section className="member-card member-gate"><span className="member-icon"><Icon name="bell" size={30} /></span><h2>{t("Make your city a little more personal.")}</h2><p>{t("Premium alerts follow your favorite categories and city. Try them without an account or payment.")}</p><Link className="button button-primary" href="/premium">{t("Try Premium Demo")}</Link></section> : loading ? <LoadingState label={t("Loading your notifications…")} /> : loadError ? <ErrorState message={t(loadError)} onRetry={() => void load()} /> : <>
      <section className="member-card member-alert-toolbar"><div><span className="member-badge">{t("Premium Demo")}</span><h2>{t("Notification center")} <span className="member-count">{feed.unreadCount}</span></h2><p>{preferences ? `${t(preferences.city)} · ${preferences.categorySlugs.map(id => t(INTERESTS.find(item => item.id === id)?.label || id)).join(", ")}` : t("Choose your interests to get started.")}</p>{preferences?.paused && <p className="field-help">{t("Alerts are paused.")}</p>}</div><div className="member-actions"><button className="button button-primary" disabled={busy || !preferences || preferences.paused} onClick={() => void action("alerts/check", "POST", {})}>{t(busy ? "Checking…" : "Check new events")}</button>{preferences && <button className="button button-outline" disabled={busy} onClick={() => void action("alerts/preferences", "PUT", { ...preferences, paused: !preferences.paused })}>{t(preferences.paused ? "Resume alerts" : "Pause alerts")}</button>}</div><p className="field-help member-full-width">{t("Demo previews use real events from the catalog. They are marked Demo and do not send email.")}</p></section>
      {error && <p className="inline-error" role="alert">{t(error)}</p>}{notice && <p className="member-success" role="status">{notice}</p>}
      <div className="catalog-tabs" aria-label={t("Notification filters")}><button className={`button ${!unreadOnly ? "button-primary" : "button-outline"}`} aria-pressed={!unreadOnly} onClick={() => setUnreadOnly(false)}>{t("All notifications")}</button><button className={`button ${unreadOnly ? "button-primary" : "button-outline"}`} aria-pressed={unreadOnly} onClick={() => setUnreadOnly(true)}>{t("Unread")}</button></div>
      {items.length ? <ul className="member-notifications">{items.map(item => <li key={item.id} className={`member-card ${!item.readAt ? "member-unread" : ""}`}><span className="member-icon"><Icon name={INTERESTS.find(interest => interest.id === item.categorySlug)?.icon || "bell"} size={24} /></span><div className="member-notification-copy"><span className="eyebrow">{t("Demo")} · {t(INTERESTS.find(interest => interest.id === item.categorySlug)?.label || item.categorySlug)}</span><h2><Link href={`/events/${encodeURIComponent(item.eventId)}`}>{item.title}</Link></h2><time dateTime={item.createdAt}>{new Intl.DateTimeFormat(locale, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(new Date(item.createdAt))}</time><Link className="text-link" href={`/events/${encodeURIComponent(item.eventId)}`}>{t("View event")}<Icon name="arrow" size={16} /></Link></div>{!item.readAt ? <button className="button button-outline" disabled={busy} onClick={() => void action(`notifications/${encodeURIComponent(item.id)}/read`, "PATCH")}>{t("Mark as read")}</button> : <span className="field-help"><Icon name="check" size={16} /> {t("Read")}</span>}</li>)}</ul> : <EmptyState title={t(unreadOnly ? "You’re all caught up." : "Good plans are on their way.")} message={t(preferences ? "Check for new demo events or adjust your alert preferences." : "Choose your city and categories to see matching events here.")} href="/settings/alerts" action={t("Notification settings")} />}
    </>}
  </div>;
}
