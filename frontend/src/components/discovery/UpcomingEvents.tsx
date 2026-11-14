"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { getEvents } from "@/lib/api";
import { getUpcomingMajorEvents } from "@/lib/discovery";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EventCard } from "@/components/events/EventCard";
import type { EventData } from "@/types/event";

export function UpcomingEvents() {
  const { t } = useLanguage();
  const [events, setEvents] = useState<EventData[]>([]);
  const [loading, setLoading] = useState(true);
  const [failed, setFailed] = useState(false);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setFailed(false);
    void getEvents({ city: "New York", state: "NY", major: true, page: 1 }, controller.signal)
      .then(response => { if (!controller.signal.aborted) setEvents(getUpcomingMajorEvents(response.events)); })
      .catch(() => { if (!controller.signal.aborted) setFailed(true); })
      .finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [retry]);

  return <section className="container upcoming-section" aria-labelledby="upcoming-title">
    <div className="section-heading"><div><span className="eyebrow">{t("SOMETHING TO LOOK FORWARD TO")}</span><h2 id="upcoming-title">{t("Upcoming major events")}</h2></div><Link href="/explore" className="text-link">{t("Explore all events")} <Icon name="arrow" size={17} /></Link></div>
    <p className="section-intro">{t("Big moments on the horizon, starting with New York.")}</p>
    {loading ? <div className="upcoming-loading" role="status"><span className="loader small" />{t("Looking for what’s coming up…")}</div> : events.length > 0 ? <div className="events-grid">{events.map(event => <EventCard event={event} key={event.id} />)}</div> : <div className="discovery-notice" role={failed ? "alert" : undefined}><span className="discovery-notice-icon"><Icon name="calendar" size={26} /></span><div><h3>{t(failed ? "The next big thing can wait a moment." : "The calendar is still opening up.")}</h3><p>{t(failed ? "We couldn’t load upcoming events. Please try again." : "No confirmed major events to show right now. Explore the full calendar for more possibilities.")}</p></div>{failed ? <button className="button button-outline" onClick={() => setRetry(value => value + 1)}>{t("Try again")} <Icon name="arrow" size={16} /></button> : <Link className="text-link" href="/explore">{t("Explore events")} <Icon name="arrow" size={16} /></Link>}</div>}
  </section>;
}
