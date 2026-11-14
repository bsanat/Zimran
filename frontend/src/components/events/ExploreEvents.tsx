"use client";

import { useEffect, useState } from "react";
import { ApiError, getEvents } from "@/lib/api";
import { getCityToday, INTERESTS } from "@/lib/planner";
import { getStateByCode, isSupportedCity, US_STATES } from "@/lib/locations";
import type { EventData } from "@/types/event";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { EventCard } from "./EventCard";

export function ExploreEvents({ initialState = "NY", initialCity = "New York", initialCategory = "", initialQuery = "" }: { initialState?: string; initialCity?: string; initialCategory?: string; initialQuery?: string }) {
  const { t } = useLanguage();
  const [stateCode, setStateCode] = useState(initialState);
  const [city, setCity] = useState(initialCity);
  const [date, setDate] = useState("");
  const [category, setCategory] = useState(initialCategory);
  const [filters, setFilters] = useState({ state: initialState, city: initialCity, date: "", category: initialCategory, keyword: initialQuery, page: 1 });
  const [events, setEvents] = useState<EventData[]>([]);
  const [hasMore, setHasMore] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [dateError, setDateError] = useState("");
  const [retry, setRetry] = useState(0);
  const selectedState = getStateByCode(stateCode) ?? getStateByCode("NY")!;
  const supported = isSupportedCity(filters.city, filters.state);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    if (!isSupportedCity(filters.city, filters.state)) {
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
  }, [filters, retry]);

  function apply(event: React.FormEvent) {
    event.preventDefault();
    if (date && date < getCityToday()) { setDateError("Choose today or a future date in New York."); return; }
    setDateError("");
    setEvents([]);
    setFilters({ state: stateCode, city, date, category, keyword: initialQuery, page: 1 });
  }

  return <>
    <div className="page-heading explore-heading"><span className="eyebrow">{t("DISCOVER YOUR CITY")}</span><h1>{initialQuery ? t("Search results for “{query}”").replace("{query}", () => initialQuery) : t("Events and experiences in {city}").replace("{city}", t(filters.city))}</h1><p>{t("Choose a state, find your city, and follow your interests. Event discovery starts with New York.")}</p></div>
    <form className="explore-filters" onSubmit={apply}>
      <div><label htmlFor="explore-state">{t("State")}</label><div className="select-control"><select id="explore-state" value={stateCode} onChange={event => { const state = getStateByCode(event.target.value)!; setStateCode(state.code); setCity(state.cities[0]); }}>{US_STATES.map(state => <option key={state.code} value={state.code}>{t(state.name)}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label htmlFor="explore-city">{t("City")}</label><div className="input-with-icon select-control"><Icon name="pin" size={18} /><select id="explore-city" value={city} onChange={event => setCity(event.target.value)}>{selectedState.cities.map(name => <option key={name} value={name}>{t(name)}{!isSupportedCity(name, stateCode) ? ` · ${t("Coming soon")}` : ""}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label htmlFor="explore-date">{t("When")}</label><input id="explore-date" type="date" value={date} min={getCityToday()} onChange={event => { setDate(event.target.value); setDateError(""); }} aria-invalid={!!dateError} aria-describedby={dateError ? "explore-date-error" : undefined} />{dateError && <p className="field-error" id="explore-date-error">{t(dateError)}</p>}</div>
      <div><label htmlFor="explore-category">{t("Your kind of thing")}</label><div className="select-control"><select id="explore-category" value={category} onChange={event => setCategory(event.target.value)}><option value="">{t("All interests")}</option>{INTERESTS.map(item => <option key={item.id} value={item.id}>{t(item.label)}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <button className="button button-primary" type="submit" disabled={loading}><Icon name="search" size={18} />{t("Find events")}</button>
    </form>
    <div className="explore-meta"><span><span className="live-dot" />{t(filters.city).toUpperCase()}, {filters.state}</span><span>{t(supported ? "Dates in local Eastern Time" : "More cities on the horizon")}</span></div>
    {!supported ? <EmptyState title={t("{city} is on our horizon").replace("{city}", t(filters.city))} message={t("Event discovery isn’t available here yet. Explore New York while we work toward more cities.")} href="/explore?state=NY&city=New%20York" action={t("Explore New York")} /> : loading && filters.page === 1 ? <LoadingState label={t("Looking around {city}…").replace("{city}", t(filters.city))} /> : <>
      {events.length > 0 && <div className="events-grid">{events.map(event => <EventCard key={event.id} event={event} />)}</div>}
      {error ? <ErrorState title={t("The city can wait a moment")} message={t(error)} onRetry={() => setRetry(current => current + 1)} /> : events.length === 0 ? <EmptyState title={t("No events for this combination")} message={t("Try another date or interest. You can also leave the date open to explore what’s coming up.")} href="/planner" action={t("Try a personal plan")} /> : null}
      {hasMore && !error && <div className="load-more"><button className="button button-outline" disabled={loading} onClick={() => setFilters(current => ({ ...current, page: current.page + 1 }))}>{t(loading ? "Loading more…" : "A few more possibilities")}<Icon name="arrow" size={17} /></button></div>}
    </>}
  </>;
}
