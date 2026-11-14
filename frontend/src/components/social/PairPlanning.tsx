"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useCallback, useEffect, useState } from "react";
import { Icon } from "@/components/Icon";
import { useDemo } from "@/components/DemoProvider";
import { useLanguage } from "@/components/LanguageProvider";
import { EventCard } from "@/components/events/EventCard";
import { PreferencesForm } from "@/components/social/PreferencesForm";
import { ApiError } from "@/lib/api";
import { createPairSession, getPairSession, joinPairSession, revokePairSession, votePair } from "@/lib/backend-api";
import { INTERESTS } from "@/lib/planner";
import type { PairPreferences, PairSession, PairVote } from "@/types/social";

// The partner link carries the one-time invite in the hash, so it never reaches a server log.
function inviteFromHash(): string {
  return new URLSearchParams(window.location.hash.slice(1)).get("invite") ?? "";
}

export function PairPlanning({ sessionId, inviteToken = "", initialCity = "New York", joinFromHash = false }: { sessionId?: string; inviteToken?: string; initialCity?: string; joinFromHash?: boolean }) {
  const { t } = useLanguage();
  const { activate } = useDemo();
  const router = useRouter();
  const [session, setSession] = useState<PairSession | null>(null);
  const [invite, setInvite] = useState(inviteToken);
  const [loading, setLoading] = useState(Boolean(sessionId && !inviteToken) || joinFromHash);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [invitation, setInvitation] = useState("");
  const [copied, setCopied] = useState(false);
  const [paywall, setPaywall] = useState(false);
  const joining = Boolean(invite && !session);
  useEffect(() => {
    if (!joinFromHash) return;
    const token = inviteFromHash();
    setInvite(token); setLoading(false);
    if (!token) setError("This invitation is invalid or has already been used.");
  }, [joinFromHash]);
  const load = useCallback(async () => {
    if (!sessionId || invite) return;
    setLoading(true); setError("");
    try { setSession(await getPairSession(sessionId)); }
    catch (error) { setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, [sessionId, invite]);
  useEffect(() => {
    void load();
    if (sessionId) {
      try { setInvitation(sessionStorage.getItem(`outly.pair.invite:${sessionId}`) || ""); } catch { /* The selectable invitation remains available until navigation. */ }
    }
  }, [load, sessionId]);
  useEffect(() => {
    if (!session || ["CLOSED", "EXPIRED"].includes(session.status)) return;
    let active = true;
    const interval = setInterval(async () => {
      if (document.visibilityState !== "visible") return;
      try { const response = await getPairSession(session.id); if (active) { setSession(response); setError(""); } }
      catch (error) { if (active) setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    }, 5000);
    return () => { active = false; clearInterval(interval); };
  }, [session?.id, session?.status]);
  async function submit(city: string, preferences: PairPreferences) {
    setBusy(true); setError("");
    try {
      if (joining) {
        const joined = await joinPairSession(invite, preferences);
        setSession(joined); setInvite("");
        router.replace(`/pair/${encodeURIComponent(joined.id)}`);
      } else {
        const response = await createPairSession(city, preferences);
        const url = new URL("/pair/join", window.location.origin);
        url.searchParams.set("city", response.session.city); url.hash = new URLSearchParams({ invite: response.inviteToken }).toString();
        setInvitation(url.href); setSession(response.session);
        try { sessionStorage.setItem(`outly.pair.invite:${response.session.id}`, url.href); } catch { /* The invitation field can still be copied. */ }
        router.replace(`/pair/${encodeURIComponent(response.session.id)}`);
      }
    } catch (error) {
      setError(error instanceof Error ? error.message : "We could not complete your request. Please try again.");
      if (error instanceof ApiError && ["PREMIUM_REQUIRED", "FREE_LIMIT_REACHED"].includes(error.code)) setPaywall(true);
    }
    finally { setBusy(false); }
  }
  async function update(run: () => Promise<PairSession>) {
    setBusy(true); setError("");
    try { setSession(await run()); }
    catch (error) { setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  async function copy() {
    try { await navigator.clipboard.writeText(invitation); setCopied(true); }
    catch { setError("Copy the invitation from the field below."); }
  }
  function preferenceSummary(preferences: PairPreferences, role: "A" | "B") {
    return <div className="social-card social-preference-summary"><span className="eyebrow">{t(role === "A" ? "PARTICIPANT A" : "PARTICIPANT B")}{session?.role === role && ` · ${t("You")}`}</span>{preferences.displayName && <h3>{preferences.displayName}</h3>}<p>{preferences.interests.map(id => t(INTERESTS.find(interest => interest.id === id)?.label || id)).join(" · ")}</p><dl><div><dt>{t("Dates")}</dt><dd>{preferences.startDate} — {preferences.endDate}</dd></div><div><dt>{t("Budget")}</dt><dd>{preferences.budgetMax === undefined ? t("Any budget") : `$${preferences.budgetMax} USD`}</dd></div><div><dt>{t("Time of day")}</dt><dd>{t({ any: "Any time", morning: "Morning", afternoon: "Afternoon", evening: "Evening" }[preferences.timePreference])}</dd></div></dl>{preferences.notes && <p className="social-body">{preferences.notes}</p>}</div>;
  }
  const shared = session?.recommendations.find(item => item.event.id === session.sharedEventId);
  return <div className="container page-section social-shell">
    <div className="page-heading"><span className="eyebrow"><Icon name="wellness" size={17} />{t("PAIR PLANNING")} <span className="social-badge">{t("Demo")}</span></span><h1>{t("Find your kind of together.")}</h1><p>{t("Two sets of wishes. One plan you both choose. Share your preferences separately, then find your mutual yes.")}</p></div>
    <div className="social-note"><Icon name="check" size={18} /><p>{t("Guest demo. Real catalog events, independent preferences, and a shared choice. No registration or payment needed.")}</p></div>
    {loading ? <p className="social-loading" role="status">{t("Loading…")}</p> : <>
      {!session && (!sessionId || joining) && <div className="social-split"><section className="social-card"><span className="eyebrow">{t(joining ? "YOUR TURN · PARTICIPANT B" : "STEP 1 · YOUR WISHES")}</span><h2>{t(joining ? "What would you enjoy?" : "Start with what you love")}</h2><p className="social-muted">{t(joining ? "Your answers are saved separately from your partner's. We apply both people's dates, time, and budget." : "Tell us what works for you. You'll get a private invitation for one planning partner.")}</p><PreferencesForm onSubmit={submit} busy={busy} action={joining ? "Join this plan" : "Create invitation"} initialCity={initialCity} cityLocked={joining} /></section><aside className="social-card social-tinted"><span className="social-large-icon"><Icon name="wellness" size={34} /></span><h2>{t("A plan with room for both of you")}</h2><ol className="social-steps"><li><strong>{t("Say what works for you")}</strong><p>{t("Your dates, your interests, your budget.")}</p></li><li><strong>{t("Invite your person")}</strong><p>{t("They add their own wishes through your private link.")}</p></li><li><strong>{t("Find your shared yes")}</strong><p>{t("Like, maybe, or pass. Two likes make a shared plan.")}</p></li></ol><p className="social-muted">{t("We only suggest events that meet both people's hard limits. If nothing fits, we'll say so.")}</p></aside></div>}
      {session && <>
        <div className="social-section-heading"><div><span className="eyebrow"><Icon name="pin" size={16} />{t(session.city)}</span><h2>{t(session.status === "MATCHED" ? "Your shared plan" : session.status === "WAITING" ? "Your invitation is ready" : session.status === "CLOSED" || session.status === "EXPIRED" ? "This plan has ended" : "Find your shared yes")}</h2></div><span className="social-status">{t(session.status)}</span></div>
        {session.status === "WAITING" && <section className="social-card social-tinted"><h3>{t("Good things are better shared")}</h3><p>{t("Send this one-time invitation to your planning partner. It expires in 48 hours.")}</p>{invitation ? <><label className="social-copy-label">{t("Private invitation link")}<input readOnly value={invitation} onFocus={event => event.target.select()} /></label><div className="social-actions"><button className="button button-primary" onClick={() => void copy()}>{t(copied ? "Copied!" : "Copy invitation link")}<Icon name={copied ? "check" : "arrow"} size={17} /></button><a className="button button-outline" href={invitation} target="_blank" rel="noopener noreferrer">{t("Open partner's window")}</a></div></> : <p>{t("The invitation was saved in the tab where you created this plan. Return to that tab to copy it.")}</p>}<p className="social-muted social-small">{t("For a two-role demo, open the invitation in a separate window or browser. Each participant keeps their own guest session.")}</p><p className="social-loading" role="status">{t("Waiting for your partner…")}</p></section>}
        <div className="social-participants">{preferenceSummary(session.participantA, "A")}{session.participantB ? preferenceSummary(session.participantB, "B") : <div className="social-card social-empty"><Icon name="user" size={30} /><h3>{t("A place for your person")}</h3><p>{t("Their preferences will appear here when they join.")}</p></div>}</div>
        {shared && <section className="social-card social-shared"><span className="eyebrow"><Icon name="check" size={19} />{t("TWO LIKES. ONE GOOD PLAN.")}</span><h2>{t("It's a yes from both of you.")}</h2><p className="social-muted">{t("Your choice is saved. Open the event for the organizer's latest details and ticket link.")}</p><div className="social-shared-event"><EventCard event={shared.event} reason={shared.reason} /></div></section>}
        {["READY", "MATCHED"].includes(session.status) && <>
          {!session.recommendations.length ? <div className="social-card social-empty"><Icon name="search" size={30} /><h3>{t("Let's give your plans a little more room")}</h3><p>{session.conflict ? session.conflict.message : t("No verified event meets both sets of constraints. Try a new plan with overlapping dates, a wider budget, or any time of day.")}</p>{session.conflict?.relaxMessage && <p className="social-muted">{session.conflict.relaxMessage}</p>}<Link href={`/pair/new?city=${encodeURIComponent(session.city)}`} className="button button-primary">{t("Start a new shared plan")}</Link></div> : <><div className="social-section-heading"><div><h2>{t("A few possibilities for both of you")}</h2><p className="social-muted">{t("All suggestions respect both budgets, dates, and time preferences. Interests may be a compromise.")}</p></div></div><div className="social-event-grid">{session.recommendations.map(({ event, reason, matchScore, voteId }, index) => <article className="social-vote-card" key={event.id}><EventCard event={event} reason={reason} matchScore={matchScore} index={index} /><div className="social-vote-controls" role="group" aria-label={`${t("Your vote")}: ${event.title}`}>{(["LIKE", "MAYBE", "PASS"] as PairVote[]).map(vote => <button key={vote} className={session.votes[session.role][event.id] === vote ? "selected" : ""} aria-pressed={session.votes[session.role][event.id] === vote} disabled={busy} onClick={() => void update(() => votePair(session.id, voteId ?? event.id, vote))}>{t(vote === "LIKE" ? "Like" : vote === "MAYBE" ? "Maybe" : "Pass")}</button>)}</div><p className="social-partner-vote">{t("Partner's vote")}: {session.votes[session.role === "A" ? "B" : "A"][event.id] ? t({ LIKE: "Like", MAYBE: "Maybe", PASS: "Pass" }[session.votes[session.role === "A" ? "B" : "A"][event.id]]) : t("Not yet")}</p></article>)}</div>{!shared && session.recommendations.every(item => session.votes.A[item.event.id] && session.votes.B[item.event.id]) && <div className="social-card social-tinted"><h3>{t("No mutual yes yet")}</h3><p>{t("You can change your votes, or start a new plan with one constraint relaxed.")}</p><Link className="text-link" href={`/pair/new?city=${encodeURIComponent(session.city)}`}>{t("Start a new shared plan")}</Link></div>}</>}
        </>}
        <div className="social-actions"><Link href="/explore" className="button button-outline">{t("Explore events")}</Link>{session.role === "A" && ["WAITING", "READY", "MATCHED"].includes(session.status) && <button className="social-text-button text-link" disabled={busy} onClick={() => void update(() => revokePairSession(session.id))}>{t("Close plan and revoke invitation")}</button>}{["CLOSED", "EXPIRED"].includes(session.status) && <Link href="/pair/new" className="button button-primary">{t("Start a new shared plan")}</Link>}</div>
      </>}
    </>}
    {error && <div className="social-error" role="alert"><p>{t(error)}</p>{sessionId && !joining && <button className="button button-outline" onClick={() => void load()} disabled={busy}>{t("Try again")}</button>}{sessionId && !session && !joining && <Link className="text-link" href="/pair/new">{t("Start a new shared plan")}</Link>}</div>}
    {paywall && <div className="social-callout"><div><strong>{t("Try Premium Demo")}</strong><p>{t("Your first shared plan is free. Demo Premium lets you try more plans without payment.")}</p><button className="button button-primary" disabled={busy} onClick={async () => {
      setBusy(true);
      try { await activate(); setPaywall(false); setError(""); }
      catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
      finally { setBusy(false); }
    }}>{t("Try Premium Demo")}</button></div></div>}
  </div>;
}
