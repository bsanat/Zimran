"use client";

import Link from "next/link";
import { useEffect, useState, type ReactNode } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EmptyState, ErrorState, LoadingState } from "@/components/States";
import { ApiError, getEvent } from "@/lib/api";
import { formatEventDate, formatPrice, INTERESTS, safeExternalUrl } from "@/lib/planner";
import type { EventData } from "@/types/event";
import { EventImage } from "./EventCard";
import "./event-details.css";

type EventDetailsProps = { id: string; closeControl?: ReactNode; navigation?: ReactNode };

export function EventDetails({ id, closeControl, navigation }: EventDetailsProps) {
  const { locale, t } = useLanguage();
  const [event, setEvent] = useState<EventData | null>(null);
  const [error, setError] = useState("");
  const [notFound, setNotFound] = useState(false);
  const [loading, setLoading] = useState(true);
  const [retry, setRetry] = useState(0);
  const [copied, setCopied] = useState(false);
  const [manualLink, setManualLink] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError("");
    setNotFound(false);
    setEvent(null);
    setCopied(false);
    setManualLink("");
    void getEvent(id, controller.signal).then(response => {
      if (!controller.signal.aborted) setEvent(response.event);
    }).catch(cause => {
      if (controller.signal.aborted) return;
      if (cause instanceof ApiError && cause.status === 404) setNotFound(true);
      else setError(cause instanceof Error ? cause.message : "We couldn’t open this event. Please try again.");
    }).finally(() => { if (!controller.signal.aborted) setLoading(false); });
    return () => controller.abort();
  }, [id, retry]);

  async function copyEventLink() {
    const link = new URL(`/events/${encodeURIComponent(id)}`, window.location.origin).href;
    try {
      await navigator.clipboard.writeText(link);
      setCopied(true);
      setManualLink("");
    } catch {
      setManualLink(link);
    }
  }

  const sourceUrl = event && safeExternalUrl(event.sourceUrl, event.source);
  const category = event && (INTERESTS.find(item => item.id === event.category)?.label ?? event.category);
  return <article className={closeControl ? "event-sheet" : "event-sheet event-sheet-standalone"}>
    <div className="event-sheet-toolbar">
      {closeControl ?? <Link className="event-sheet-back" href="/explore" aria-label={t("Back to events")} title={t("Back to events")}><Icon name="back" size={18} /></Link>}
      {event && <button type="button" onClick={copyEventLink}><Icon name={copied ? "check" : "copy"} size={16} /><span aria-live="polite">{t(copied ? "Copied!" : "Copy link")}</span></button>}
      {sourceUrl && <a href={sourceUrl} target="_blank" rel="noopener noreferrer">
        {t(event?.source === "catalog" ? "Official page" : event?.source === "ticketmaster" ? "View on Ticketmaster" : "View event website")} <Icon name="external" size={15} />
      </a>}
      {navigation && <div className="event-sheet-nav">{navigation}</div>}
    </div>
    {manualLink && <label className="event-manual-link">{t("Event link")}
      <input aria-label={t("Event link")} value={manualLink} readOnly onFocus={e => e.target.select()} ref={input => { if (input) input.select(); }} />
    </label>}
    {loading ? <LoadingState label={t("Getting the event details…")} />
      : notFound ? <EmptyState title={t("This event is out of reach")} message={t("It may no longer be available. Let’s find something else to look forward to.")} href="/explore" action={t("Back to events")} />
      : error || !event ? <ErrorState message={t(error || "We couldn’t load this event.")} onRetry={() => setRetry(current => current + 1)} />
      : <div className="event-sheet-content">
        <EventImage key={event.id} event={event} className="event-sheet-poster" sizes="(max-width: 650px) 95vw, 650px" />
        <header className="event-sheet-heading">
          <p className="event-sheet-category"><Icon name={event.category} size={16} />{t(category || "")} <span>·</span> {t(event.city)}</p>
          <h1 id="event-details-title">{event.title}</h1>
          <p className="event-sheet-source"><Icon name="globe" size={15} /><span>{event.sourceName || event.source}</span></p>
        </header>
        {event.status !== "scheduled" && <p className="status-notice">{t(event.status === "cancelled" ? "This event has been cancelled. Please check the source for updates." : "This event has been postponed. The original date may change; check the source for updates.")}</p>}
        <dl className="event-sheet-facts">
          <div>
            <dt><Icon name="calendar" size={21} /><span className="sr-only">{t("When")}</span></dt>
            <dd>{formatEventDate(event, locale)}<small>{event.isRecurring && <>{t("Recurring program")} · </>}{event.timezone}</small></dd>
          </div>
          <div>
            <dt><Icon name="pin" size={21} /><span className="sr-only">{t("Where")}</span></dt>
            <dd>{event.venue || t("Venue unavailable")}<small>{event.address || t(event.city)}</small></dd>
          </div>
          <div>
            <dt><Icon name="ticket" size={21} /><span className="sr-only">{t("Price per person")}</span></dt>
            <dd>{formatPrice(event, locale)}<small>{t("Price per person")}</small></dd>
          </div>
        </dl>
        {!sourceUrl && <p className="field-help">{t("The original event link is unavailable.")}</p>}
        <section className="event-sheet-about"><h2>{t("About this event")}</h2>
          <p>{event.description || t("No description is available for this event. Check the original source for more details.")}</p>
        </section>
        {event.lastVerifiedAt && <p className="event-verified"><Icon name="check" size={14} />{t("Checked against official pages on {date}.").replace("{date}", event.lastVerifiedAt)}</p>}
        {event.status === "scheduled" && <div className="event-social-actions">
          <Link className="button button-outline" href={`/buddy/create?eventId=${encodeURIComponent(event.id)}`}><Icon name="people" size={18} />{t("Find someone to join me")}</Link>
          <Link className="text-link" href={`/buddy/${encodeURIComponent(event.id)}`}>{t("Join someone")}<Icon name="arrow" size={16} /></Link>
          <Link className="button button-primary" href={`/pair/new?city=${encodeURIComponent(event.city)}`}><Icon name="wellness" size={18} />{t("Plan together")}</Link>
          <p className="field-help">{t("Guest demo · real events, demo interactions")}</p>
        </div>}
      </div>}
  </article>;
}
