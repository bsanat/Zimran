"use client";

import { EventLink } from "@/components/events/EventLink";
import { useEffect, useRef, useState } from "react";
import { getCityEvents } from "@/lib/api";
import { nextOccurrences } from "@/lib/discovery";
import { formatEventDate, INTERESTS, isDate } from "@/lib/planner";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EventImage } from "@/components/events/EventCard";
import { ErrorState } from "@/components/States";
import { InterestStrip } from "./InterestStrip";
import type { EventData } from "@/types/event";

type Pick = { event: EventData; date?: string };

function EventRail({ id, title, picks, loading }: { id: string; title: string; picks: Pick[]; loading: boolean }) {
  const { t, locale } = useLanguage();
  const track = useRef<HTMLUListElement>(null);
  const [edges, setEdges] = useState({ first: true, last: true });
  useEffect(() => {
    const list = track.current;
    if (!list) return;
    const update = () => setEdges({ first: list.scrollLeft < 2, last: list.scrollLeft + list.clientWidth >= list.scrollWidth - 2 });
    const observer = new ResizeObserver(update);
    observer.observe(list);
    list.addEventListener("scroll", update, { passive: true });
    update();
    return () => { observer.disconnect(); list.removeEventListener("scroll", update); };
  }, [picks.length]);

  function scroll(direction: number) {
    const list = track.current;
    if (list) list.scrollBy({ left: direction * (list.firstElementChild?.getBoundingClientRect().width || list.clientWidth), behavior: "auto" });
  }

  const dateFormat = new Intl.DateTimeFormat(locale, { day: "numeric", month: "short", timeZone: "UTC" });
  function schedule({ event, date }: Pick) {
    if (date) return dateFormat.format(new Date(date));
    if (event.startDate && isDate(event.startDate) && !event.startAt) {
      const start = new Date(event.startDate);
      return event.endDate && isDate(event.endDate) && event.endDate > event.startDate
        ? dateFormat.formatRange(start, new Date(event.endDate)) : dateFormat.format(start);
    }
    return formatEventDate(event, locale);
  }

  const eventIds = picks.map(pick => pick.event.id);
  return <section className="container event-rail" aria-labelledby={id}>
    <div className="rail-heading">
      <h2 id={id}>{title}</h2>
      {(!edges.first || !edges.last) && <div className="rail-controls">
        <button type="button" disabled={edges.first} aria-label={t("Previous events")} aria-controls={id + "-list"} onClick={() => scroll(-1)}><Icon name="back" size={18} /></button>
        <button type="button" disabled={edges.last} aria-label={t("Next events")} aria-controls={id + "-list"} onClick={() => scroll(1)}><Icon name="arrow" size={18} /></button>
      </div>}
    </div>
    {loading ? <p className="upcoming-loading" role="status"><span className="loader small" />{t("Looking for what’s coming up…")}</p>
      : picks.length ? <ul className="rail-track" ref={track} id={id + "-list"}>{picks.map(pick => {
        const { event } = pick;
        const category = INTERESTS.find(interest => interest.id === event.category);
        return <li key={event.id}><EventLink className="rail-card" eventId={event.id} eventIds={eventIds} aria-label={event.title}>
          <EventImage event={event} className="rail-photo" />
          <div className="rail-copy">
            <span className="rail-category"><Icon name={event.category} size={14} />{t(category?.label ?? event.category)}</span>
            <h3>{event.title}</h3>
            <p>{schedule(pick)} · {t(event.city)}</p>
            <span className="event-details-hint">{t("Show details")} <Icon name="arrow" size={14} /></span>
          </div>
        </EventLink></li>;
      })}</ul> : <p className="rail-empty">{t(id === "upcoming" ? "No upcoming events in the next 30 days." : "No events in this collection yet.")}</p>}
  </section>;
}

export function HomeEvents({ state, city }: { state: string; city: string }) {
  const { t } = useLanguage();
  const [events, setEvents] = useState<EventData[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [retry, setRetry] = useState(0);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setFailed(false);
    void getCityEvents(state, city, controller.signal)
      .then(cityEvents => { if (!controller.signal.aborted) setEvents(cityEvents); })
      .catch(() => { if (!controller.signal.aborted) setFailed(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [state, city, retry]);

  if (failed) return <>
    <div className="container"><ErrorState title={t("The city can wait a moment")} message={t("We couldn’t load upcoming events. Please try again.")} onRetry={() => setRetry(value => value + 1)} /></div>
    <InterestStrip />
  </>;
  const picks = events.map(event => ({ event }));
  return <>
    <EventRail id="trending" title={t("Trending in {city}").replace("{city}", t(city))} picks={picks.slice(0, 8)} loading={loading} />
    <EventRail id="upcoming" title={t("Upcoming in {city}").replace("{city}", t(city))} picks={nextOccurrences(events)} loading={loading} />
    <InterestStrip />
    <EventRail id="meet-someone-new" title={t("Meet someone new")} picks={picks.filter(pick => pick.event.isSocial)} loading={loading} />
  </>;
}
