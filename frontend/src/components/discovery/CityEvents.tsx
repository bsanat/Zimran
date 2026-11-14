"use client";

import { useEffect, useMemo, useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { ErrorState } from "@/components/States";
import { EventImage } from "@/components/events/EventCard";
import { EventLink } from "@/components/events/EventLink";
import { getCityEvents } from "@/lib/api";
import { cityListings, type CityListing, type CitySort } from "@/lib/city-events";
import type { EventData } from "@/types/event";
import "./city-events.css";

export function CityEvents({ city, state }: { city: string; state: string }) {
  const { t, locale } = useLanguage();
  const [events, setEvents] = useState<EventData[] | null>(null);
  const [error, setError] = useState("");
  const [retry, setRetry] = useState(0);
  const [sort, setSort] = useState<CitySort>("earliest");
  const [category, setCategory] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setEvents(null);
    setError("");
    void getCityEvents(state, city, controller.signal).then(data => {
      if (!controller.signal.aborted) setEvents(data);
    }).catch(cause => {
      if (!controller.signal.aborted) setError(cause instanceof Error ? cause.message : "We couldn’t load events. Please try again.");
    });
    return () => controller.abort();
  }, [state, city, retry]);

  const listings = useMemo(() => cityListings(events ?? [], sort, locale), [events, sort, locale]);
  const categories = [...new Map(listings.map(row => [row.event.category, row.category])).entries()]
    .sort(([, a], [, b]) => a.localeCompare(b, locale));
  const filtered = listings.filter(row => !category || row.event.category === category);
  const ids = filtered.map(row => row.event.id);
  const dateFormat = new Intl.DateTimeFormat(locale, { month: "long", day: "numeric", year: "numeric", weekday: "long", timeZone: "UTC" });
  const groups = new Map<string, CityListing[]>();
  for (const row of filtered) {
    const key = sort === "category" ? row.category : row.date;
    const group = groups.get(key);
    if (group) group.push(row);
    else groups.set(key, [row]);
  }

  return <section className="container city-schedule" aria-labelledby="city-schedule-title">
    <div className="city-schedule-bar">
      <h2 id="city-schedule-title">{t("Events")}</h2>
      <div className="city-schedule-filters">
        <label>{t("Category")}
          <select value={category} onChange={e => setCategory(e.target.value)}>
            <option value="">{t("All categories")}</option>
            {categories.map(([id, label]) => <option key={id} value={id}>{label}</option>)}
          </select>
        </label>
        <label>{t("Sort by")}
          <select value={sort} onChange={e => setSort(e.target.value as CitySort)}>
            <option value="earliest">{t("Earliest first")}</option>
            <option value="latest">{t("Latest first")}</option>
            <option value="category">{t("Category A–Z")}</option>
          </select>
        </label>
      </div>
    </div>
    {error ? <ErrorState message={error} onRetry={() => setRetry(n => n + 1)} />
      : !events ? <p role="status">{t("Looking for what’s coming up…")}</p>
      : !groups.size ? <p role="status">{t("No upcoming events here yet.")}</p>
      : Array.from(groups, ([key, rows]) => <section className="city-schedule-day" key={key}>
        <h3>{sort === "category" ? key : key ? dateFormat.format(new Date(key)) : t("Date to be confirmed")}</h3>
        <ul>{rows.map(({ event, date, time, category: categoryName }) => <li key={event.id}>
          <EventLink className="city-schedule-card" eventId={event.id} eventIds={ids} aria-label={event.title}>
            <div className="city-schedule-copy">
              <p className="city-schedule-when">{sort === "category" && date && <>{dateFormat.format(new Date(date))} · </>}{time || event.dateText || t("Time to be confirmed")}</p>
              <h4>{event.title}</h4>
              <p className="city-schedule-venue"><Icon name="pin" size={14} />{event.venue || t(event.city)}</p>
              <div className="city-schedule-actions"><span><Icon name={event.category} size={13} />{categoryName}</span><span>{t("Show details")} <Icon name="arrow" size={14} /></span></div>
            </div>
            <EventImage event={event} className="city-schedule-photo" />
          </EventLink>
        </li>)}</ul>
      </section>)}
  </section>;
}
