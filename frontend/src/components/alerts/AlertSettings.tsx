"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useDemo } from "@/components/DemoProvider";
import { useLanguage } from "@/components/LanguageProvider";
import { Icon } from "@/components/Icon";
import { ErrorState, LoadingState } from "@/components/States";
import { catalogRegions } from "@/lib/catalog";
import { demoRequest } from "@/lib/demo-api";
import { getCityToday, INTERESTS } from "@/lib/planner";
import type { Interest } from "@/types/planner";
import type { AlertPreferences } from "@/types/social";

const cities = [...new Set(catalogRegions.flatMap(region => region.cities))].sort();

export function AlertSettings() {
  const { t } = useLanguage();
  const { subscription, loading: sessionLoading, error: sessionError, refresh } = useDemo();
  const premium = subscription?.status === "DEMO_PREMIUM";
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const [categories, setCategories] = useState<Interest[]>(["gaming"]);
  const [city, setCity] = useState("New York");
  const [budget, setBudget] = useState("");
  const [startDate, setStartDate] = useState("");
  const [endDate, setEndDate] = useState("");
  const [frequency, setFrequency] = useState<AlertPreferences["frequency"]>("instant");
  const [paused, setPaused] = useState(false);
  const load = useCallback(async () => {
    setLoading(true); setLoadError("");
    try {
      const { preferences } = await demoRequest<{ preferences: AlertPreferences | null }>("alerts/preferences");
      if (preferences) {
        setCategories(preferences.categorySlugs); setCity(preferences.city); setBudget(preferences.budgetMax === undefined ? "" : String(preferences.budgetMax));
        setStartDate(preferences.startDate || ""); setEndDate(preferences.endDate || ""); setFrequency(preferences.frequency); setPaused(preferences.paused);
      }
    } catch (cause) { setLoadError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { if (premium) void load(); }, [premium, load]);
  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setSaved(false);
    if (!categories.length) { setError("Choose at least one interest."); return; }
    if ((startDate && !endDate) || (endDate && !startDate) || (startDate && endDate < startDate)) { setError("Choose a valid start and end date."); return; }
    setBusy(true);
    try {
      await demoRequest("alerts/preferences", { method: "PUT", body: JSON.stringify({ city, categorySlugs: categories, frequency, channels: ["in_app"], paused, ...(budget !== "" ? { budgetMax: Number(budget) } : {}), ...(startDate ? { startDate, endDate } : {}) }) });
      setSaved(true);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  return <div className="container page-section member-shell"><div className="page-heading"><span className="eyebrow"><Icon name="bell" size={16} />{t("PREMIUM EVENT ALERTS")}</span><h1>{t("Only the plans you want to hear about.")}</h1><p>{t("Choose your city and interests. New matching events will appear in your notification center.")}</p></div>
    <div className="member-layout"><section className="member-card">
      {sessionLoading || loading ? <LoadingState label={t("Loading alert preferences…")} /> : sessionError || loadError ? <ErrorState message={t(sessionError || loadError)} onRetry={() => { if (sessionError) void refresh(); else void load(); }} /> : <form className="member-form" onSubmit={event => void save(event)} onChange={() => setSaved(false)}>
        <div className="member-section-title"><h2>{t("Your interests")}</h2><span className="member-badge">{t(premium ? "Premium Demo" : "Premium only")}</span></div><p className="field-help">{t("Choose one or more categories.")}</p>
        <fieldset className="member-categories"><legend className="sr-only">{t("Event categories")}</legend>{INTERESTS.map(interest => <label key={interest.id} className={categories.includes(interest.id) ? "selected" : ""}><input type="checkbox" checked={categories.includes(interest.id)} onChange={event => setCategories(current => event.target.checked ? [...current, interest.id] : current.filter(id => id !== interest.id))} /><Icon name={interest.icon} size={18} />{t(interest.label)}</label>)}</fieldset>
        <div className="member-field-pair"><div><label className="field-label" htmlFor="alert-city">{t("City")}</label><select id="alert-city" value={city} onChange={event => setCity(event.target.value)}>{cities.map(name => <option key={name} value={name}>{t(name)}</option>)}</select></div><div><label className="field-label" htmlFor="alert-budget">{t("Maximum price per person")}</label><input id="alert-budget" type="number" min="0" max="10000" step="0.01" value={budget} onChange={event => setBudget(event.target.value)} placeholder={t("Any budget")} /><p className="field-help">{t("Unknown prices do not match a price limit.")}</p></div></div>
        <div><label className="field-label" htmlFor="alert-frequency">{t("How often")}</label><select id="alert-frequency" value={frequency} onChange={event => setFrequency(event.target.value as AlertPreferences["frequency"])}><option value="instant">{t("Instant")}</option><option value="daily">{t("Daily digest")}</option><option value="weekly">{t("Weekly digest")}</option></select><p className="field-help">{t("Daily and weekly alerts wait for their next delivery window. Choose Instant to preview a notification now.")}</p></div>
        <fieldset className="member-date-range"><legend className="field-label">{t("Event date range (optional)")}</legend><div className="member-field-pair"><div><label className="field-label" htmlFor="alert-from">{t("From")}</label><input id="alert-from" type="date" min={getCityToday()} value={startDate} onChange={event => setStartDate(event.target.value)} /></div><div><label className="field-label" htmlFor="alert-to">{t("To")}</label><input id="alert-to" type="date" min={startDate || getCityToday()} value={endDate} onChange={event => setEndDate(event.target.value)} /></div></div></fieldset>
        <div><span className="field-label">{t("Delivery")}</span><p className="member-channel"><Icon name="bell" size={18} />{t("In-app notifications")}</p></div><label className="member-toggle"><input type="checkbox" checked={paused} onChange={event => setPaused(event.target.checked)} />{t("Pause alerts")}</label>
        {error && <p className="inline-error" role="alert">{t(error)}</p>}{saved && <p className="member-success" role="status"><Icon name="check" size={18} />{t("Your alert preferences are saved.")}</p>}
        <div className="member-actions">{premium ? <button className="button button-primary" disabled={busy}>{t(busy ? "Saving…" : "Save preferences")}</button> : <Link className="button button-primary" href="/premium">{t("Try Premium Demo")}</Link>}<Link className="button button-outline" href="/notifications">{t("Notification center")}</Link></div>
      </form>}
    </section><aside className="member-card member-aside"><span className="member-icon"><Icon name="bell" size={28} /></span><h2>{t("A little heads-up for your next day out.")}</h2><p>{t("Music means music. Your alerts follow the categories, city, dates, and budget you choose.")}</p><p className="field-help">{t("Demo previews use real events from the catalog. They are marked Demo and do not send email.")}</p><Link className="text-link" href="/premium">{t("See Premium benefits")}<Icon name="arrow" size={16} /></Link></aside></div>
  </div>;
}
