"use client";

import Link from "next/link";
import { useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { formatEventDate, formatPrice, INTERESTS } from "@/lib/planner";
import type { EventData } from "@/types/event";

export function EventImage({ event, className = "" }: { event: EventData; className?: string }) {
  const { t } = useLanguage();
  const [failed, setFailed] = useState(false);
  const image = event.imageUrl && /^https:\/\//i.test(event.imageUrl) ? event.imageUrl : null;
  const category = INTERESTS.find(item => item.id === event.category)?.label ?? event.category.replaceAll("_", " ");
  return <div className={`event-image ${className}`}>{image && !failed ? <img src={image} alt={event.title} loading="lazy" onError={() => setFailed(true)} /> : <div className="event-image-fallback"><Icon name={event.category} size={44} /><span>{t("Image unavailable")}</span></div>}<span className="event-category">{t(category)}</span>{event.status !== "scheduled" && <span className={`status-badge ${event.status}`}>{t(event.status === "cancelled" ? "Cancelled" : "Postponed")}</span>}</div>;
}

export function EventCard({ event, reason, matchScore, index }: { event: EventData; reason?: string; matchScore?: number; index?: number }) {
  const { locale, t } = useLanguage();
  return <article className="event-card"><EventImage event={event} /><div className="event-card-content">{index !== undefined && <div className="recommendation-label"><span>0{index + 1} / {t("PICKED FOR YOU")}</span>{matchScore !== undefined && <span>{t("{score}% match").replace("{score}", String(matchScore))}</span>}</div>}<p className="event-date"><Icon name="calendar" size={15} />{formatEventDate(event, locale)}</p><h2>{event.title}</h2><p className="event-venue"><Icon name="pin" size={15} />{event.venue || t("Venue unavailable")}{event.city ? ` · ${t(event.city)}` : ""}</p>{reason && <div className="event-reason"><Icon name="spark" size={17} /><p>{t(reason)}</p></div>}<div className="event-card-bottom"><span className="event-price">{formatPrice(event, locale)}</span><Link href={`/events/${encodeURIComponent(event.id)}`} className="text-link">{t(reason !== undefined ? "View Event" : "View Details")}<Icon name="arrow" size={16} /></Link></div></div></article>;
}
