"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { ApiError, getEvent } from "@/lib/api";
import { formatEventDate, formatPrice, INTERESTS, safeExternalUrl } from "@/lib/planner";
import type { EventData } from "@/types/event";
import { EventImage } from "./EventCard";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";

export function EventDetails({ id }: { id: string }) {
  const { locale, t } = useLanguage();
  const [event, setEvent] = useState<EventData | null>(null);
  const [error, setError] = useState("");
  const [notFound, setNotFound] = useState(false);
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  useEffect(() => {
    const controller = new AbortController();
    setLoading(true); setError(""); setNotFound(false); setEvent(null);
    void getEvent(id, controller.signal).then(response => {
      if (!controller.signal.aborted) setEvent(response.event);
    }).catch(cause => {
      if (controller.signal.aborted) return;
      if (cause instanceof ApiError && cause.status === 404) setNotFound(true);
      else setError(cause instanceof Error ? cause.message : "We couldn’t open this event. Please try again.");
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, retry]);
  if (loading) return <LoadingState label={t("Getting the event details…")} />;
  if (notFound) return <EmptyState title={t("This event is out of reach")} message={t("It may no longer be available. Let’s find something else to look forward to.")} href="/explore" action={t("Back to events")} />;
  if (error || !event) return <ErrorState message={t(error || "We couldn’t load this event.")} onRetry={() => setRetry(current => current + 1)} />;
  const sourceUrl = safeExternalUrl(event.sourceUrl, event.source);
  const category = INTERESTS.find(item => item.id === event.category)?.label ?? event.category.replaceAll("_", " ");
  return <><Link className="text-link back-link" href="/explore"><Icon name="back" size={17} />{t("Back to events")}</Link><div className="detail-layout"><div><EventImage event={event} className="detail-image" /><section className="event-about"><span className="eyebrow">{t("A LITTLE MORE ABOUT IT")}</span><h2>{t("Worth knowing before you go.")}</h2><p>{event.description || t("No description is available for this event. Check the original source for more details.")}</p></section></div><aside className="detail-panel"><span className="eyebrow">{t(category)} · {t(event.city)}</span><h1>{event.title}</h1><span className={`detail-status ${event.status}`}><span className="live-dot" />{t(event.status === "scheduled" ? "Scheduled" : event.status === "cancelled" ? "Cancelled" : "Postponed")}</span>{event.status !== "scheduled" && <p className="status-notice">{t(event.status === "cancelled" ? "This event has been cancelled. Please check the source for updates." : "This event has been postponed. The original date may change; check the source for updates.")}</p>}<dl className="event-facts"><div><dt><Icon name="calendar" />{t("When")}</dt><dd>{formatEventDate(event, locale)}<small>{event.timezone}</small></dd></div><div><dt><Icon name="pin" />{t("Where")}</dt><dd>{event.venue || t("Venue unavailable")}<small>{event.address || t("Address unavailable")}</small></dd></div><div><dt>{t("Price per person")}</dt><dd className="detail-price">{formatPrice(event, locale)}</dd></div></dl>{sourceUrl ? <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className="button button-primary source-button">{t(event.source === "ticketmaster" ? "View on Ticketmaster" : "View event website")} <Icon name="external" size={17} /></a> : <p className="field-help">{t("The original event link is unavailable.")}</p>}<p className="source-note">{t("You’ll continue on the original event website. Confirm the latest prices and availability there.")}</p><div className="source-attribution"><Icon name="check" size={16} />{t("Source")}: {event.source}</div></aside></div></>;
}
