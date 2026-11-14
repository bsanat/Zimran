"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useDemo } from "@/components/DemoProvider";
import { useLanguage } from "@/components/LanguageProvider";
import { Icon } from "@/components/Icon";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { categoryIcon, checkAlertsNow, getAlertCategories, getAlertPreferences, getNotifications, markNotificationRead, saveAlertPreferences, type AlertCategory } from "@/lib/backend-api";
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
  // Category labels come from the backend (GET /api/meta); until they load, the category id is shown.
  const [categories, setCategories] = useState<AlertCategory[]>([]);
  useEffect(() => { getAlertCategories(locale).then(setCategories, () => {}); }, [locale]);
  const category = (id: string) => categories.find(item => item.id === id);
  const load = useCallback(async () => {
    try {
      const [response, settings] = await Promise.all([getNotifications(), getAlertPreferences()]);
      setFeed(response); setPreferences(settings); setLoadError("");
    } catch (cause) { setLoadError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => {
    if (!premium) return;
    void load();
    const timer = setInterval(() => { if (!document.hidden) void load(); }, 15_000);
    return () => clearInterval(timer);
  }, [premium, load]);
  async function action(run: () => Promise<string | void>) {
    setBusy(true); setError(""); setNotice("");
    try {
      const message = await run();
      if (message) setNotice(message);
      await load();
      await refreshNotifications();
    } catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  const items = feed.items.filter(item => !unreadOnly || !item.readAt);
  return <div className="container page-section member-shell"><div className="page-heading member-title-row"><div><span className="eyebrow"><Icon name="bell" size={16} />{t("PREMIUM EVENT ALERTS")}</span><h1>{t("Your next good reason to go out.")}</h1><p>{t("New events that match your interests, all in one place.")}</p></div><Link className="button button-outline" href="/settings/alerts">{t("Notification settings")}</Link></div>
    {sessionLoading ? <LoadingState label={t("Checking demo access…")} /> : sessionError ? <ErrorState message={t(sessionError)} onRetry={() => void refresh()} /> : !premium ? <section className="member-card member-gate"><span className="member-icon"><Icon name="bell" size={30} /></span><h2>{t("Make your city a little more personal.")}</h2><p>{t("Premium alerts follow your favorite categories and city. Try them without an account or payment.")}</p><Link className="button button-primary" href="/premium">{t("Try Premium Demo")}</Link></section> : loading ? <LoadingState label={t("Loading your notifications…")} /> : loadError ? <ErrorState message={t(loadError)} onRetry={() => void load()} /> : <>
      <section className="member-card member-alert-toolbar"><div><span className="member-badge">{t("Premium Demo")}</span><h2>{t("Notification center")} <span className="member-count">{feed.unreadCount}</span></h2><p>{preferences ? `${t(preferences.city)} · ${preferences.categorySlugs.map(id => category(id)?.label ?? id).join(", ")}` : t("Choose your interests to get started.")}</p>{preferences?.paused && <p className="field-help">{t("Alerts are paused.")}</p>}</div><div className="member-actions"><button className="button button-primary" disabled={busy || !preferences || preferences.paused} onClick={() => void action(async () => {
        const { created } = await checkAlertsNow();
        return created ? t("New matching demo alerts are ready.") : t("No new matching events. Your filters and previously checked events are respected.");
      })}>{t(busy ? "Checking…" : "Check new events")}</button>{preferences && <button className="button button-outline" disabled={busy} onClick={() => void action(async () => { await saveAlertPreferences({ ...preferences, paused: !preferences.paused }); })}>{t(preferences.paused ? "Resume alerts" : "Pause alerts")}</button>}</div><p className="field-help member-full-width">{t("Demo previews use real events from the catalog. They are marked Demo and do not send email.")}</p></section>
      {error && <p className="inline-error" role="alert">{t(error)}</p>}{notice && <p className="member-success" role="status">{notice}</p>}
      <div className="catalog-tabs" aria-label={t("Notification filters")}><button className={`button ${!unreadOnly ? "button-primary" : "button-outline"}`} aria-pressed={!unreadOnly} onClick={() => setUnreadOnly(false)}>{t("All notifications")}</button><button className={`button ${unreadOnly ? "button-primary" : "button-outline"}`} aria-pressed={unreadOnly} onClick={() => setUnreadOnly(true)}>{t("Unread")}</button></div>
      {items.length ? <ul className="member-notifications">{items.map(item => <li key={item.id} className={`member-card ${!item.readAt ? "member-unread" : ""}`}><span className="member-icon"><Icon name={categoryIcon(category(item.categorySlug))} size={24} /></span><div className="member-notification-copy"><span className="eyebrow">{t("Demo")} · {category(item.categorySlug)?.label ?? item.categorySlug}</span><h2><Link href={`/events/${encodeURIComponent(item.eventId)}`}>{item.title}</Link></h2><time dateTime={item.createdAt}>{new Intl.DateTimeFormat(locale, { month: "short", day: "numeric", hour: "numeric", minute: "2-digit" }).format(new Date(item.createdAt))}</time><Link className="text-link" href={`/events/${encodeURIComponent(item.eventId)}`}>{t("View event")}<Icon name="arrow" size={16} /></Link></div>{!item.readAt ? <button className="button button-outline" disabled={busy} onClick={() => void action(() => markNotificationRead(item.id))}>{t("Mark as read")}</button> : <span className="field-help"><Icon name="check" size={16} /> {t("Read")}</span>}</li>)}</ul> : <EmptyState title={t(unreadOnly ? "You’re all caught up." : "Good plans are on their way.")} message={t(preferences ? "Check for new demo events or adjust your alert preferences." : "Choose your city and categories to see matching events here.")} href="/settings/alerts" action={t("Notification settings")} />}
    </>}
  </div>;
}
