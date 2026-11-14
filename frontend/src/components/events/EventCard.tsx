"use client";

import Link from "next/link";
import Image from "next/image";
import { useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { formatEventDate, formatPrice, INTERESTS, safeExternalUrl } from "@/lib/planner";
import type { EventData } from "@/types/event";

export function EventImage({ event, className = "" }: { event: EventData; className?: string }) {
  const { t } = useLanguage();
  const [failed, setFailed] = useState(false);
  const image = event.imageUrl && (/^(https:\/\/|\/catalog\/)/i.test(event.imageUrl)) ? event.imageUrl : null;
  const category = INTERESTS.find(item => item.id === event.category)?.label ?? event.category.replaceAll("_", " ");
  return <div className={`event-image ${className}`}>{image && !failed ? <Image src={image} alt={event.title} fill sizes="(max-width: 640px) 100vw, (max-width: 1000px) 50vw, 33vw" unoptimized={!image.startsWith("/catalog/")} onError={() => setFailed(true)} /> : <div className="event-image-fallback"><Icon name={event.category} size={44} /><span>{t("Image unavailable")}</span></div>}{event.imageIsIllustration && <span className="event-image-caption">{t("Venue or organizer photo")}</span>}<span className="event-category">{t(category)}</span>{event.status !== "scheduled" && <span className={`status-badge ${event.status}`}>{t(event.status === "cancelled" ? "Cancelled" : "Postponed")}</span>}</div>;
}

export function EventCard({ event, reason, matchScore, index }: { event: EventData; reason?: string; matchScore?: number; index?: number }) {
  const { locale, t } = useLanguage();
  const sourceUrl = safeExternalUrl(event.sourceUrl, event.source);
  return <article className="event-card"><EventImage event={event} /><div className="event-card-content">{index !== undefined && <div className="recommendation-label"><span>0{index + 1} / {t("PICKED FOR YOU")}</span>{matchScore !== undefined && <span>{t("{score}% match").replace("{score}", String(matchScore))}</span>}</div>}<p className="event-date"><Icon name="calendar" size={15} />{formatEventDate(event, locale)}</p><h2>{event.title}</h2><p className="event-venue"><Icon name="pin" size={15} />{event.venue || t("Venue unavailable")}{event.city ? ` · ${t(event.city)}` : ""}</p>{reason && <div className="event-reason"><Icon name="spark" size={17} /><p>{t(reason)}</p></div>}<div className="event-card-bottom"><span className="event-price">{formatPrice(event, locale)}</span><div className="event-card-links"><Link href={`/events/${encodeURIComponent(event.id)}`} className="text-link">{t(reason !== undefined ? "View Event" : "View Details")}<Icon name="arrow" size={16} /></Link>{sourceUrl && <a href={sourceUrl} target="_blank" rel="noopener noreferrer" className="text-link">{t(event.source === "catalog" ? "Official page" : "Event website")}<Icon name="external" size={15} /></a>}</div></div>{event.lastVerifiedAt && <p className="event-verified"><Icon name="check" size={13} />{t("Verified source")}</p>}</div></article>;
}
