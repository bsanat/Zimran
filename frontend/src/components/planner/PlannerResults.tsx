"use client";

import Link from "next/link";
import { useEffect, useState } from "react";
import { readLastPlan } from "@/hooks/usePlanner";
import type { PlannerResponse } from "@/types/planner";
import { EventCard } from "@/components/events/EventCard";
import { EmptyState, LoadingState } from "@/components/States";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";

export function PlannerResults() {
  const { t } = useLanguage();
  const [plan, setPlan] = useState<PlannerResponse | null>(null);
  const [ready, setReady] = useState(false);
  useEffect(() => { setPlan(readLastPlan()); setReady(true); }, []);
  if (!ready) return <LoadingState label={t("Opening your picks…")} />;
  if (!plan) return <EmptyState title={t("Your next good day is waiting")} message={t("Tell us a little about what you love, and we’ll look for real events that fit. Your saved preferences are ready to pick up where you left off.")} action={t("Go to planner")} />;
  return <><div className="page-heading result-heading"><div><span className="eyebrow"><Icon name="spark" size={16} /> {t("YOUR INTERESTS. NEW POSSIBILITIES.")}</span><h1>{t("Your kind of")} <em>{t("New York.")}</em></h1><p>{t(plan.summary)}</p></div><Link className="button button-outline" href="/planner">{t("Edit preferences")} <Icon name="arrow" size={17} /></Link></div>{plan.recommendations.length > 0 ? <><div className="results-bar"><span>{t("Events for your day")}: {plan.recommendations.length}</span><span>{t("Real events. Original sources.")}</span></div><div className="events-grid recommendations-grid">{plan.recommendations.map((recommendation, index) => <EventCard key={recommendation.event.id} {...recommendation} index={index} />)}</div><p className="results-footnote">{t("A collection of ideas, at your own pace. Check the source for current availability, prices, and details.")}</p></> : <EmptyState />}{plan.suggestions && plan.suggestions.length > 0 && <div className="suggestions"><h2>{t("A few things to try")}</h2><ul>{plan.suggestions.map((suggestion, index) => <li key={index}>{t(suggestion)}</li>)}</ul></div>}</>;
}
