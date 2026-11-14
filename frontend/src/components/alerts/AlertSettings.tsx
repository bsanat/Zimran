"use client";

import Link from "next/link";
import { useCallback, useEffect, useState } from "react";
import { useDemo } from "@/components/DemoProvider";
import { useLanguage } from "@/components/LanguageProvider";
import { Icon } from "@/components/Icon";
import { ErrorState, LoadingState } from "@/components/States";
import { catalogRegions } from "@/lib/catalog";
import { categoryIcon, getAlertCategories, getAlertPreferences, saveAlertPreferences, type AlertCategory } from "@/lib/backend-api";
import type { AlertPreferences } from "@/types/social";

const cities = [...new Set(catalogRegions.flatMap(region => region.cities))].sort();

export function AlertSettings() {
  const { t, locale } = useLanguage();
  const { subscription, loading: sessionLoading, error: sessionError, refresh } = useDemo();
  const premium = subscription?.status === "DEMO_PREMIUM";
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [loadError, setLoadError] = useState("");
  const [saved, setSaved] = useState(false);
  const [busy, setBusy] = useState(false);
  const [categories, setCategories] = useState<string[]>(["music"]);
  const [city, setCity] = useState("New York");
  const [budget, setBudget] = useState("");
  const [frequency, setFrequency] = useState<AlertPreferences["frequency"]>("instant");
  const [paused, setPaused] = useState(false);
  // Alert categories are the backend's (GET /api/meta), labelled in the page language.
  const [options, setOptions] = useState<AlertCategory[]>([]);
  const [optionsError, setOptionsError] = useState("");
  const loadOptions = useCallback(async () => {
    setOptionsError("");
    try { setOptions(await getAlertCategories(locale)); }
    catch (cause) { setOptionsError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
  }, [locale]);
  useEffect(() => { void loadOptions(); }, [loadOptions]);
  const load = useCallback(async () => {
    setLoading(true); setLoadError("");
    try {
      const preferences = await getAlertPreferences();
      if (preferences) {
        setCategories(preferences.categorySlugs); setCity(preferences.city); setBudget(preferences.budgetMax === undefined ? "" : String(preferences.budgetMax));
        setFrequency(preferences.frequency); setPaused(preferences.paused);
      }
    } catch (cause) { setLoadError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, []);
  useEffect(() => { if (premium) void load(); }, [premium, load]);
  async function save(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault(); setError(""); setSaved(false);
    if (!categories.length) { setError("Choose at least one interest."); return; }
    setBusy(true);
    try {
      await saveAlertPreferences({ city, categorySlugs: categories, frequency, channels: ["in_app"], paused, ...(budget !== "" ? { budgetMax: Number(budget) } : {}) });
      setSaved(true);
    } catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  return <div className="container page-section member-shell"><div className="page-heading"><span className="eyebrow"><Icon name="bell" size={16} />{t("PREMIUM EVENT ALERTS")}</span><h1>{t("Only the plans you want to hear about.")}</h1><p>{t("Choose your city and interests. New matching events will appear in your notification center.")}</p></div>
    <div className="member-layout"><section className="member-card">
      {sessionLoading || loading || (!options.length && !optionsError) ? <LoadingState label={t("Loading alert preferences…")} /> : sessionError || loadError || optionsError ? <ErrorState message={t(sessionError || loadError || optionsError)} onRetry={() => { if (sessionError) void refresh(); else if (optionsError) void loadOptions(); else void load(); }} /> : <form className="member-form" onSubmit={event => void save(event)} onChange={() => setSaved(false)}>
        <div className="member-section-title"><h2>{t("Your interests")}</h2><span className="member-badge">{t(premium ? "Premium Demo" : "Premium only")}</span></div><p className="field-help">{t("Choose one or more categories.")}</p>
        <fieldset className="member-categories"><legend className="sr-only">{t("Event categories")}</legend>{options.map(category => <label key={category.id} className={categories.includes(category.id) ? "selected" : ""}><input type="checkbox" checked={categories.includes(category.id)} onChange={event => setCategories(current => event.target.checked ? [...current, category.id] : current.filter(id => id !== category.id))} /><Icon name={categoryIcon(category)} size={18} />{category.label}</label>)}</fieldset>
        <div className="member-field-pair"><div><label className="field-label" htmlFor="alert-city">{t("City")}</label><select id="alert-city" value={city} onChange={event => setCity(event.target.value)}>{cities.map(name => <option key={name} value={name}>{t(name)}</option>)}</select></div><div><label className="field-label" htmlFor="alert-budget">{t("Maximum price per person")}</label><input id="alert-budget" type="number" min="0" max="10000" step="0.01" value={budget} onChange={event => setBudget(event.target.value)} placeholder={t("Any budget")} /><p className="field-help">{t("Unknown prices do not match a price limit.")}</p></div></div>
        <div><label className="field-label" htmlFor="alert-frequency">{t("How often")}</label><select id="alert-frequency" value={frequency} onChange={event => setFrequency(event.target.value as AlertPreferences["frequency"])}><option value="instant">{t("Instant")}</option><option value="daily">{t("Daily digest")}</option><option value="weekly">{t("Weekly digest")}</option></select><p className="field-help">{t("Daily and weekly alerts wait for their next delivery window. Choose Instant to preview a notification now.")}</p></div>
                <div><span className="field-label">{t("Delivery")}</span><p className="member-channel"><Icon name="bell" size={18} />{t("In-app notifications")}</p></div><label className="member-toggle"><input type="checkbox" checked={paused} onChange={event => setPaused(event.target.checked)} />{t("Pause alerts")}</label>
        {error && <p className="inline-error" role="alert">{t(error)}</p>}{saved && <p className="member-success" role="status"><Icon name="check" size={18} />{t("Your alert preferences are saved.")}</p>}
        <div className="member-actions">{premium ? <button className="button button-primary" disabled={busy}>{t(busy ? "Saving…" : "Save preferences")}</button> : <Link className="button button-primary" href="/premium">{t("Try Premium Demo")}</Link>}<Link className="button button-outline" href="/notifications">{t("Notification center")}</Link></div>
      </form>}
    </section><aside className="member-card member-aside"><span className="member-icon"><Icon name="bell" size={28} /></span><h2>{t("A little heads-up for your next day out.")}</h2><p>{t("Music means music. Your alerts follow the categories, city, and budget you choose.")}</p><p className="field-help">{t("Demo previews use real events from the catalog. They are marked Demo and do not send email.")}</p><Link className="text-link" href="/premium">{t("See Premium benefits")}<Icon name="arrow" size={16} /></Link></aside></div>
  </div>;
}
