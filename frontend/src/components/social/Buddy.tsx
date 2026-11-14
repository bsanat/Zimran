"use client";

import Link from "next/link";
import { useCallback, useEffect, useState, type FormEvent } from "react";
import { useLanguage } from "@/components/LanguageProvider";
import { useDemo } from "@/components/DemoProvider";
import { Icon } from "@/components/Icon";
import { EventImage } from "@/components/events/EventCard";
import { getEvent, getEvents } from "@/lib/api";
import { blockBuddy, cancelBuddyRequest, closeBuddyPost, createBuddyPost, decideBuddyRequest, getBuddyPosts, reportBuddy, REPORT_REASONS, requestToJoinBuddy, type ReportReason } from "@/lib/backend-api";
import { formatEventDate, formatPrice } from "@/lib/planner";
import type { EventData } from "@/types/event";
import type { BuddyPost } from "@/types/social";

export function Buddy({ eventId, createInitially = false }: { eventId?: string; createInitially?: boolean }) {
  const { t, locale } = useLanguage();
  const { subscription } = useDemo();
  const [event, setEvent] = useState<EventData | null>(null);
  const [events, setEvents] = useState<EventData[]>([]);
  const [posts, setPosts] = useState<BuddyPost[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [busy, setBusy] = useState(false);
  const [creating, setCreating] = useState(createInitially);
  const [requesting, setRequesting] = useState<string | null>(null);
  const [safety, setSafety] = useState<{ id: string; requestId?: string; kind: "REPORT" | "BLOCK" } | null>(null);
  const reasonLabels: Record<ReportReason, string> = { SPAM: "Spam", HARASSMENT: "Harassment", INAPPROPRIATE: "Inappropriate content", SCAM: "Scam or fraud", UNDERAGE: "Seems to be under 18", CONTACT_DETAILS: "Shares contact details", OTHER: "Something else" };
  const [notice, setNotice] = useState("");
  const premium = subscription?.status === "DEMO_PREMIUM";
  const load = useCallback(async () => {
    setLoading(true); setError("");
    try {
      if (eventId) {
        const [details, feed] = await Promise.all([getEvent(eventId), getBuddyPosts(eventId)]);
        setEvent(details.event); setPosts(feed);
      } else setEvents((await getEvents({ city: "New York" })).events);
    } catch (error) { setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    finally { setLoading(false); }
  }, [eventId]);
  useEffect(() => { void load(); }, [load]);
  useEffect(() => {
    if (!eventId) return;
    let active = true;
    const interval = setInterval(async () => {
      if (document.visibilityState !== "visible") return;
      try { const feed = await getBuddyPosts(eventId); if (active) { setPosts(feed); setError(""); } }
      catch (error) { if (active) setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    }, 5000);
    return () => { active = false; clearInterval(interval); };
  }, [eventId]);
  async function mutate(run: () => Promise<unknown>, done = "Saved. Your demo partner can see the update.") {
    setBusy(true); setError(""); setNotice("");
    try {
      await run();
      setPosts(await getBuddyPosts(eventId!));
      setCreating(false); setRequesting(null); setSafety(null); setNotice(done);
    } catch (error) { setError(error instanceof Error ? error.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  function sendPost(form: FormEvent<HTMLFormElement>) {
    form.preventDefault();
    const data = new FormData(form.currentTarget);
    void mutate(() => createBuddyPost(eventId!, { displayName: String(data.get("displayName")), bio: String(data.get("bio")), language: String(data.get("language")), ageConfirmed: data.get("ageConfirmed") === "on" }));
  }
  const upgrade = <div className="social-callout"><Icon name="spark" size={22} /><div><strong>{t("A little company, with Premium")}</strong><p>{t("Create posts and send requests with Premium. Try the clearly marked demo without payment.")}</p><Link href={`/premium?returnTo=${encodeURIComponent(eventId ? `/buddy/${encodeURIComponent(eventId)}` : "/buddy")}`} className="button button-primary">{t("Try Premium Demo")}</Link></div></div>;
  return <div className="container page-section social-shell">
    <div className="page-heading"><span className="eyebrow"><Icon name="user" size={17} />{t("EVENT BUDDY")} <span className="social-badge">{t("Demo")}</span></span><h1>{t("Good plans. Better company.")}</h1><p>{t("Find someone to share a real event with. Choose an event, introduce yourself, and decide who joins you.")}</p></div>
    <div className="social-note"><Icon name="alert" size={18} /><p>{t("Guest demo: posts and requests are from demo participants, not verified people. For adults 18+. Meet in public and keep personal contacts private.")}</p></div>
    {loading ? <p role="status" className="social-loading">{t("Loading…")}</p> : <>
      {event && <div className="social-event-summary"><Link href={`/events/${encodeURIComponent(event.id)}`} className="social-event-thumbnail"><EventImage event={event} /></Link><div><span className="eyebrow">{t("YOUR EVENT")}</span><h2><Link href={`/events/${encodeURIComponent(event.id)}`}>{event.title}</Link></h2><p>{t(event.city)} · {formatEventDate(event, locale)}</p><strong>{formatPrice(event, locale)}</strong><div className="social-actions"><Link href="/buddy" className="text-link">{t("Choose another event")}</Link><Link href={`/pair/new?city=${encodeURIComponent(event.city)}`} className="text-link">{t("Plan together")}</Link></div></div></div>}
      {!eventId && <><h2>{t("Start with an event")}</h2><p className="social-muted">{t("Verified New York events. Open any event in the catalog to find company in other cities.")}</p><div className="social-event-grid">{events.map(item => <Link key={item.id} href={`/buddy/${encodeURIComponent(item.id)}`} className="social-choice"><EventImage event={item} /><div><h3>{item.title}</h3><p>{formatEventDate(item, locale)}</p><strong>{formatPrice(item, locale)}</strong><span className="text-link">{t("Find a Buddy")} <Icon name="arrow" size={16} /></span></div></Link>)}</div><div className="social-actions"><Link href="/explore" className="button button-outline">{t("Explore events")}</Link></div></>}
      {eventId && event && <>
        <div className="social-section-heading"><div><h2>{t("Who's going?")}</h2><p className="social-muted">{t("An introduction is all it takes.")}</p></div><button className="button button-primary" onClick={() => { setCreating(value => !value); setRequesting(null); }} disabled={busy || event.status !== "scheduled"}>{t(creating ? "Cancel" : "Find someone to join me")}</button></div>
        {creating && (!premium ? upgrade : <section className="social-card"><h2>{t("Make the first move")}</h2><form className="social-fields" onSubmit={sendPost}><fieldset disabled={busy} className="social-fieldset"><label>{t("Display name")}<input name="displayName" required minLength={2} maxLength={50} autoComplete="nickname" /></label><label>{t("A short introduction")}<textarea name="bio" required minLength={5} maxLength={300} rows={3} placeholder={t("What would make this event a good plan for you?")} /></label><label>{t("Conversation language")}<select name="language" defaultValue={locale === "kk" ? "Kazakh" : locale === "ru" ? "Russian" : "English"}><option value="English">English</option><option value="Russian">Русский</option><option value="Kazakh">Қазақша</option></select></label><label className="social-checkbox"><input name="ageConfirmed" type="checkbox" required />{t("I am 18 or older. I will not post private contact details.")}</label><button className="button button-primary" type="submit" disabled={busy}>{t(busy ? "Saving…" : "Publish demo post")}</button></fieldset></form></section>)}
        {!posts.length && !creating && <div className="social-card social-empty"><Icon name="user" size={32} /><h3>{t("Be the first to say hello")}</h3><p>{t("There are no demo posts for this event yet. Create one and invite another demo participant to reply.")}</p></div>}
        <div className="social-stack">{posts.map(post => <article className="social-card" key={post.id}><div className="social-post-heading"><div className="social-person"><span className="social-avatar" aria-hidden="true">{post.displayName.slice(0, 1).toUpperCase()}</span><div><h3>{post.displayName} {post.isOwner && <span className="social-muted">· {t("You")}</span>}</h3><span className="social-muted">{post.language ? `${t(post.language)} · ` : ""}{t("Demo participant")}</span></div></div><span className="social-status">{t(post.status)}</span></div><p className="social-body">{post.bio}</p>
          {post.isOwner ? <>{post.requests.map(request => <div key={request.id} className="social-request"><div><strong>{request.displayName}</strong><span className="social-status">{t(request.status)}</span></div><p className="social-body">{request.message}</p>{request.status === "PENDING" && post.status === "OPEN" && <div className="social-actions"><button className="button button-primary" disabled={busy} onClick={() => void mutate(() => decideBuddyRequest(request.id, "ACCEPT"))}>{t("Accept")}</button><button className="button button-outline" disabled={busy} onClick={() => void mutate(() => decideBuddyRequest(request.id, "DECLINE"))}>{t("Decline")}</button></div>}{request.status === "ACCEPTED" && <p className="social-success">{t("You're matched. Your introductions are visible to each other here; no contact details are shared.")}</p>}{request.safetyNotice && <p className="social-note">{request.safetyNotice}</p>}<div className="social-actions social-safety-actions"><button className="social-text-button text-link" disabled={busy} onClick={() => setSafety({ id: post.id, requestId: request.id, kind: "REPORT" })}>{t("Report")}</button><button className="social-text-button text-link" disabled={busy} onClick={() => setSafety({ id: post.id, requestId: request.id, kind: "BLOCK" })}>{t("Block")}</button></div></div>)}{["OPEN", "MATCHED"].includes(post.status) && <button className="text-link social-text-button" disabled={busy} onClick={() => void mutate(() => closeBuddyPost(post.id))}>{t("Close my post")}</button>}</> : <>
            {post.requests.filter(request => request.isOwner).map(request => <div className="social-request" key={request.id}><strong>{t("Your request")}: {t(request.status)}</strong><p className="social-body">{request.message}</p>{request.status === "ACCEPTED" && <p className="social-success">{t("You're matched. Your introductions are visible to each other here; no contact details are shared.")}</p>}{request.safetyNotice && <p className="social-note">{request.safetyNotice}</p>}{["PENDING", "ACCEPTED"].includes(request.status) && <button disabled={busy} className="text-link social-text-button" onClick={() => void mutate(() => cancelBuddyRequest(request.id))}>{t("Cancel my request")}</button>}</div>)}
            {post.status === "OPEN" && !post.requests.some(request => request.isOwner && ["PENDING", "ACCEPTED"].includes(request.status)) && <button className="button button-outline" disabled={busy} onClick={() => { setRequesting(requesting === post.id ? null : post.id); setCreating(false); }}>{t("Request to join")}</button>}
            {requesting === post.id && (!premium ? upgrade : <form className="social-fields social-request" onSubmit={form => { form.preventDefault(); const data = new FormData(form.currentTarget); void mutate(() => requestToJoinBuddy(post.id, { displayName: String(data.get("displayName")), message: String(data.get("message")), ageConfirmed: data.get("ageConfirmed") === "on" })); }}><fieldset disabled={busy} className="social-fieldset"><label>{t("Display name")}<input name="displayName" required minLength={2} maxLength={50} autoComplete="nickname" /></label><label>{t("Say hello")}<textarea name="message" required minLength={5} maxLength={300} rows={3} /></label><label className="social-checkbox"><input name="ageConfirmed" type="checkbox" required />{t("I am 18 or older. I will not post private contact details.")}</label><button type="submit" disabled={busy} className="button button-primary">{t(busy ? "Saving…" : "Send demo request")}</button></fieldset></form>)}
            <div className="social-actions social-safety-actions"><button className="social-text-button text-link" disabled={busy} onClick={() => setSafety({ id: post.id, kind: "REPORT" })}>{t("Report")}</button><button className="social-text-button text-link" disabled={busy} onClick={() => setSafety({ id: post.id, kind: "BLOCK" })}>{t("Block")}</button></div>
          </>}
          {safety?.id === post.id && <form className="social-fields social-request" onSubmit={form => { form.preventDefault(); const data = new FormData(form.currentTarget); const target: ["POST" | "REQUEST", string] = safety.requestId ? ["REQUEST", safety.requestId] : ["POST", post.id]; void mutate(safety.kind === "BLOCK" ? () => blockBuddy(...target) : () => reportBuddy(...target, String(data.get("reason")) as ReportReason, String(data.get("comment") || "").trim() || undefined), safety.kind === "BLOCK" ? "Blocked. You will no longer see each other's posts or requests." : "Thank you. Your report was sent to our moderators."); }}>{safety.kind === "REPORT" ? <><label>{t("What is wrong?")}<select name="reason" required defaultValue="SPAM">{REPORT_REASONS.map(reason => <option key={reason} value={reason}>{t(reasonLabels[reason])}</option>)}</select></label><label>{t("Details (optional)")}<textarea name="comment" maxLength={500} rows={2} /></label></> : <p className="social-muted">{t("You will no longer see each other's posts or requests.")}</p>}<div className="social-actions"><button className="button button-outline" type="submit" disabled={busy}>{t(safety.kind === "BLOCK" ? "Block participant" : "Submit report")}</button><button type="button" className="social-text-button text-link" onClick={() => setSafety(null)}>{t("Cancel")}</button></div></form>}
        </article>)}</div>
      </>}
    </>}
    {error && <div className="social-error" role="alert"><p>{t(error)}</p><button className="button button-outline" disabled={busy} onClick={() => void load()}>{t("Try again")}</button></div>}
    {notice && <p className="social-success" role="status">{t(notice)}</p>}
  </div>;
}
