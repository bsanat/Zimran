"use client";

import { useState, type FormEvent } from "react";
import { useLanguage } from "@/components/LanguageProvider";
import { catalogRegions } from "@/lib/catalog";
import { addDays, getCityToday, INTERESTS, TIME_OPTIONS } from "@/lib/planner";
import type { Interest, TimePreference } from "@/types/planner";
import type { PairPreferences } from "@/types/social";

const cities = [...new Set(catalogRegions.flatMap(region => region.cities))].sort();

export function PreferencesForm({ onSubmit, busy, action, initialCity = "New York", cityLocked = false }: {
  onSubmit: (city: string, preferences: PairPreferences) => Promise<void>;
  busy: boolean; action: string; initialCity?: string; cityLocked?: boolean;
}) {
  const { t } = useLanguage();
  const [city, setCity] = useState(initialCity);
  const [interests, setInterests] = useState<Interest[]>(["gaming"]);
  const [error, setError] = useState("");
  const today = getCityToday();
  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const data = new FormData(event.currentTarget);
    const startDate = String(data.get("startDate"));
    const endDate = String(data.get("endDate"));
    if (!interests.length) { setError("Choose at least one interest."); return; }
    if (endDate < startDate) { setError("The end date must be on or after the start date."); return; }
    setError("");
    await onSubmit(city, {
      interests, startDate, endDate,
      timePreference: String(data.get("timePreference")) as TimePreference,
      ...(data.get("budgetMax") !== "" ? { budgetMax: Number(data.get("budgetMax")) } : {}),
      notes: String(data.get("notes")).trim(),
    });
  }
  return <form onSubmit={submit} className="social-fields">
    <fieldset disabled={busy} className="social-fieldset">
      <div className="social-field-pair">
        <label>{t("City")}<select value={city} onChange={event => setCity(event.target.value)} disabled={cityLocked}>{cities.map(name => <option key={name} value={name}>{t(name)}</option>)}</select></label>
      </div>
      <fieldset className="social-fieldset"><legend>{t("Your interests")}</legend><div className="social-interest-options">{INTERESTS.map(interest => <label key={interest.id} className={interests.includes(interest.id) ? "selected" : ""}><input type="checkbox" checked={interests.includes(interest.id)} onChange={event => setInterests(current => event.target.checked ? [...current, interest.id] : current.filter(id => id !== interest.id))} />{t(interest.label)}</label>)}</div></fieldset>
      <div className="social-field-pair">
        <label>{t("From date")}<input type="date" name="startDate" required min={today} defaultValue={today} /></label>
        <label>{t("To date")}<input type="date" name="endDate" required min={today} defaultValue={addDays(today, 30)} /></label>
      </div>
      <div className="social-field-pair">
        <label>{t("Maximum budget per person")}<input name="budgetMax" type="number" min="0" max="10000" step="0.01" placeholder={t("Any budget")} /><small>{t("USD. Leave empty for any budget; 0 means free only.")}</small></label>
        <label>{t("Time of day")}<select name="timePreference" defaultValue="any">{TIME_OPTIONS.map(option => <option key={option.value} value={option.value}>{t(option.label)}</option>)}</select></label>
      </div>
      <label>{t("Anything else?")}<textarea name="notes" rows={3} maxLength={500} placeholder={t("Your wishes, in a few words")} /><small>{t("Up to 500 characters. Your partner does not see these wishes.")}</small></label>
      {error && <p className="social-error" role="alert">{t(error)}</p>}
      <button type="submit" className="button button-primary" disabled={busy}>{t(busy ? "Saving…" : action)}</button>
    </fieldset>
  </form>;
}
