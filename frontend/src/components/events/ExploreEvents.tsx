"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, getEvents } from "@/lib/api";
import { BUDGETS, getCityToday, INTERESTS } from "@/lib/planner";
import { isSupportedCity } from "@/lib/locations";
import { catalogMeta, catalogRegions, filterCatalogEvents } from "@/lib/catalog";
import type { EventData } from "@/types/event";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { EventCard } from "./EventCard";

export function ExploreEvents({ initialState = "", initialCity = "", initialCategory = "", initialQuery = "", initialBudgetMax, source = "catalog" }: { initialState?: string; initialCity?: string; initialCategory?: string; initialQuery?: string; initialBudgetMax?: number; source?: "catalog" | "live" }) {
  const { t } = useLanguage();
  const [stateCode, setStateCode] = useState(initialState);
  const [city, setCity] = useState(initialCity);
  const [date, setDate] = useState("");
  const [category, setCategory] = useState(initialCategory);
  const [budget, setBudget] = useState(source === "live" || initialBudgetMax === undefined ? "any" : String(initialBudgetMax));
  const [filters, setFilters] = useState({ state: initialState, city: initialCity, date: "", category: initialCategory, keyword: initialQuery, budgetMax: source === "live" ? undefined : initialBudgetMax, page: 1, source });
  const [events, setEvents] = useState<EventData[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [dateError, setDateError] = useState("");
  const [retry, setRetry] = useState(0);
  const selectedState = catalogRegions.find(state => state.code === stateCode);
  const location = filters.city || catalogRegions.find(state => state.code === filters.state)?.name || "United States";
  const count = filterCatalogEvents(filters).length;
  const supported = source === "catalog" || isSupportedCity(filters.city, filters.state);
  const countFilters = { date, keyword: initialQuery, budgetMax: budget === "any" ? undefined : Number(budget) };

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    if (source === "live" && !isSupportedCity(filters.city, filters.state)) {
      setEvents([]); setHasMore(false); setLoading(false);
      return () => controller.abort();
    }
    void getEvents(filters, controller.signal).then(response => {
      if (controller.signal.aborted) return;
      setEvents(current => filters.page === 1 ? response.events : [...new Map([...current, ...response.events].map(event => [event.id, event])).values()]);
      setHasMore(response.pagination.hasMore);
    }).catch(cause => {
      if (!controller.signal.aborted) setError(cause instanceof ApiError && cause.status === 404 ? "We couldn’t load events right now. Please try again." : cause instanceof Error ? cause.message : "We couldn’t load events. Please try again.");
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [filters, retry, source]);

  const eventIds = events.map(event => event.id);

  function apply(event: React.FormEvent) {
    event.preventDefault();
    if (date && date < getCityToday()) { setDateError("Choose today or a future date in New York."); return; }
    setDateError("");
    setEvents([]);
    setFilters({ state: stateCode, city, date, category, keyword: initialQuery, budgetMax: source === "live" ? undefined : countFilters.budgetMax, page: 1, source });
  }

  return <>
    <div className="page-heading explore-heading"><span className="eyebrow">{t("DISCOVER YOUR CITY")}</span><h1>{initialQuery ? t("Search results for “{query}”").replace("{query}", () => initialQuery) : t("Events and experiences in {city}").replace("{city}", t(location))}</h1><p>{t("Choose a category, then a state and city. Every catalog listing links to its official source.")}</p></div>
    <nav className="catalog-tabs" aria-label={t("Event sources")}><Link href="/explore" aria-current={source === "catalog" ? "page" : undefined}>{t("Verified catalog")}</Link><Link href="/explore?source=live&state=NY&city=New%20York" aria-current={source === "live" ? "page" : undefined}>{t("Latest New York events")}</Link></nav>
    {source === "catalog" && <p className="catalog-verification"><Icon name="check" size={16} />{t("Checked against official pages on {date}.").replace("{date}", catalogMeta.lastVerifiedAt)}</p>}
    <form className="explore-filters" onSubmit={apply}>
      <div><label htmlFor="explore-category">{t("Your kind of thing")}</label><div className="select-control"><select id="explore-category" value={category} onChange={event => setCategory(event.target.value)}><option value="">{t("All interests")}</option>{INTERESTS.map(item => <option key={item.id} value={item.id}>{t(item.label)}{source === "catalog" ? ` (${filterCatalogEvents({ ...countFilters, state: stateCode, city, category: item.id }).length})` : ""}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label htmlFor="explore-state">{t("State")}</label><div className="select-control"><select id="explore-state" value={stateCode} disabled={source === "live"} onChange={event => { setStateCode(event.target.value); setCity(""); }}><option value="">{t("All states")}</option>{catalogRegions.map(state => <option key={state.code} value={state.code}>{t(state.name)}{source === "catalog" ? ` (${filterCatalogEvents({ ...countFilters, state: state.code, category }).length})` : ""}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label htmlFor="explore-city">{t("City")}</label><div className="input-with-icon select-control"><Icon name="pin" size={18} /><select id="explore-city" value={city} disabled={!selectedState || source === "live"} onChange={event => setCity(event.target.value)}><option value="">{t("All cities")}</option>{(source === "live" ? ["New York"] : selectedState?.cities || []).map(name => <option key={name} value={name}>{t(name)}{source === "catalog" ? ` (${filterCatalogEvents({ ...countFilters, state: stateCode, city: name, category }).length})` : ""}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label htmlFor="explore-date">{t("When")}</label><input id="explore-date" type="date" value={date} min={getCityToday()} onChange={event => { setDate(event.target.value); setDateError(""); }} aria-invalid={!!dateError} aria-describedby={dateError ? "explore-date-error" : undefined} />{dateError && <p className="field-error" id="explore-date-error">{t(dateError)}</p>}</div>
      <div><label htmlFor="explore-budget">{t("Maximum per person (USD)")}</label><div className="select-control"><select id="explore-budget" value={budget} disabled={source === "live"} aria-describedby={source === "live" ? "explore-budget-help" : undefined} onChange={event => setBudget(event.target.value)}>{BUDGETS.map(option => <option key={option.value} value={option.value}>{t(option.label)}</option>)}{!BUDGETS.some(option => option.value === budget) && <option value={budget}>≤ ${budget}</option>}</select><Icon name="chevron" size={18} className="select-chevron" /></div>{source === "live" && <p className="field-meta" id="explore-budget-help">{t("Budget filters are available in the verified catalog.")}</p>}</div>
      <button className="button button-primary" type="submit" disabled={loading}><Icon name="search" size={18} />{t("Find events")}</button>
    </form>
    <div className="explore-meta"><span><span className="live-dot" />{t(location).toUpperCase()}{filters.state && filters.city ? `, ${filters.state}` : ""}</span><span>{source === "catalog" ? t("Events: {count}").replace("{count}", String(count)) : t("Latest New York events")}</span></div>
    {!supported ? <EmptyState title={t("{city} is on our horizon").replace("{city}", t(location))} message={t("Event discovery isn’t available here yet. Explore New York while we work toward more cities.")} href="/explore?state=NY&city=New%20York" action={t("Explore New York")} /> : loading && filters.page === 1 ? <LoadingState label={t("Looking around {city}…").replace("{city}", t(location))} /> : <>
      {events.length > 0 && <div className="events-grid">{events.map(event => <EventCard key={event.id} event={event} eventIds={eventIds} />)}</div>}
      {error ? <ErrorState title={t("The city can wait a moment")} message={t(error)} onRetry={() => setRetry(current => current + 1)} /> : events.length === 0 ? <EmptyState title={t("No events for this combination")} message={t("Try another date or interest. You can also leave the date open to explore what’s coming up.")} href="/explore" action={t("View the entire catalog")} /> : null}
      {hasMore && !error && <div className="load-more"><button className="button button-outline" disabled={loading} onClick={() => setFilters(current => ({ ...current, page: current.page + 1 }))}>{t(loading ? "Loading more…" : "A few more possibilities")}<Icon name="arrow" size={17} /></button></div>}
    </>}
  </>;
}
