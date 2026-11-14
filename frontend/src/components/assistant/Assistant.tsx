"use client";

import Link from "next/link";
import { useEffect, useRef, useState, type FormEvent } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { EventImage } from "@/components/events/EventCard";
import { EventLink } from "@/components/events/EventLink";
import { ApiError } from "@/lib/api";
import { isAssistantChatResponse, restoreAssistantTurns, type AssistantTurn } from "@/lib/assistant-chat";
import { getDemoToken } from "@/lib/demo-api";
import { formatEventDate, formatPrice, safeExternalUrl } from "@/lib/planner";
import type { AssistantChatResponse } from "@/types/assistant";

const storageKey = "outly-assistant-chat-v3";
const examples = ["NYC, free games this Thursday", "New York, family activities this weekend", "New York, free running next Tuesday"];
const endedChatCodes = ["ASSISTANT_CONVERSATION_NOT_FOUND", "ASSISTANT_CONVERSATION_EXPIRED", "ASSISTANT_TURN_LIMIT"];

async function chat(message: string, previous: AssistantChatResponse | undefined, signal: AbortSignal): Promise<AssistantChatResponse> {
  const bearer = await getDemoToken();
  let response: Response;
  try {
    response = await fetch("/api/ai/chat", {
      method: "POST", headers: { "Content-Type": "application/json", Authorization: `Bearer ${bearer}` },
      body: JSON.stringify({ message, preferences: previous?.preferences, pendingFields: previous?.pendingFields, conversationId: previous?.conversationId }),
      signal: AbortSignal.any([signal, AbortSignal.timeout(20_000)]),
    });
  } catch (cause) {
    if (signal.aborted) throw cause;
    throw new ApiError("We could not reach the event service. Check your connection and try again.");
  }
  const body: unknown = await response.json().catch(() => null);
  if (!response.ok) {
    const error = body && typeof body === "object" && "error" in body && body.error && typeof body.error === "object" ? body.error : null;
    throw new ApiError(error && "message" in error && typeof error.message === "string" ? error.message : "We could not complete your request. Please try again.", response.status, error && "code" in error && typeof error.code === "string" ? error.code : "API_ERROR");
  }
  if (!isAssistantChatResponse(body)) throw new ApiError("The server returned an incomplete response. Please try again.", 502, "INVALID_RESPONSE");
  return body;
}

export function Assistant() {
  const { t, locale } = useLanguage();
  const [message, setMessage] = useState("");
  const [turns, setTurns] = useState<AssistantTurn[]>([]);
  const [busy, setBusy] = useState(false);
  const [ready, setReady] = useState(false);
  const request = useRef<AbortController | null>(null);
  const log = useRef<HTMLDivElement>(null);
  const input = useRef<HTMLTextAreaElement>(null);
  useEffect(() => {
    try { setTurns(restoreAssistantTurns(JSON.parse(sessionStorage.getItem(storageKey) || "[]"))); } catch { /* Start a fresh chat if this tab's saved data is unavailable. */ }
    setReady(true);
    return () => request.current?.abort();
  }, []);
  useEffect(() => {
    if (!ready) return;
    try { sessionStorage.setItem(storageKey, JSON.stringify(turns)); } catch { /* Keep the conversation in memory until reload. */ }
    if (log.current) log.current.scrollTop = log.current.scrollHeight;
  }, [turns, ready]);

  async function send(text: string, retryId?: string) {
    text = text.trim();
    if (!ready || request.current || !text || text.length > 500) return;
    const controller = new AbortController(); request.current = controller;
    const id = retryId || crypto.randomUUID();
    const previous = [...turns].reverse().find(turn => turn.response)?.response;
    setBusy(true);
    if (retryId) setTurns(current => current.map(turn => turn.id === retryId ? { id, message: text } : turn));
    else { setTurns(current => [...current, { id, message: text }].slice(-30)); setMessage(""); }
    input.current?.focus({ preventScroll: true });
    try {
      const response = await chat(text, previous, controller.signal);
      if (!controller.signal.aborted) setTurns(current => current.map(turn => turn.id === id ? { id, message: text, response } : turn));
    } catch (cause) {
      if (!controller.signal.aborted) setTurns(current => current.map(turn => turn.id === id ? { id, message: text, error: cause instanceof Error ? cause.message : "We could not complete your request. Please try again.", errorCode: cause instanceof ApiError ? cause.code : undefined } : turn));
    } finally { if (!controller.signal.aborted) { request.current = null; setBusy(false); } }
  }
  function submit(event: FormEvent) { event.preventDefault(); void send(message); }
  function reset(draft = "") { setTurns([]); setMessage(draft); input.current?.focus(); }

  return <div className="assistant-shell">
    <section className="assistant-chat" aria-label={t("Chat with Outly Assistant")}>
      <div className="assistant-chat-header"><Link href="/explore" className="assistant-chat-back" aria-label={t("Back to events")} title={t("Back to events")}><Icon name="back" size={21} /></Link><span className="assistant-chat-avatar"><Icon name="spark" size={21} /></span><div><h1>Outly Assistant</h1><span><span className="live-dot" />{t("Connected to our event catalog")}</span></div><button className="assistant-new-chat" disabled={busy || !ready || !turns.length} onClick={() => reset()}><Icon name="spark" size={15} />{t("New chat")}</button></div>
      <div className="assistant-chat-log" role="log" aria-label={t("Conversation")} aria-live="polite" aria-relevant="additions text" ref={log}>
        <div className="assistant-chat-message"><span className="assistant-message-icon"><Icon name="spark" size={16} /></span><div className="assistant-chat-bubble"><p>{t("Hi! What would you like to do?")}</p><p>{t("Tell me your city, when you’re free, and what you enjoy. I’ll find real events from our catalog.")}</p>{!turns.length && <div className="assistant-chat-examples">{examples.map(example => <button type="button" disabled={!ready || busy} key={example} onClick={() => void send(t(example))}>{t(example)}<Icon name="arrow" size={15} /></button>)}</div>}</div></div>
        {turns.map((turn, index) => <div className="assistant-chat-turn" key={turn.id}>
          <div className="assistant-chat-message assistant-chat-user"><span className="sr-only">{t("You")}: </span><div className="assistant-chat-bubble"><p>{turn.message}</p></div></div>
          {turn.response && <div className="assistant-chat-message"><span className="assistant-message-icon"><Icon name="spark" size={16} /></span><div className="assistant-chat-reply">
            {turn.response.clarifications.length > 0 ? <div className="assistant-chat-bubble"><p>{t("A little more detail will help me find the right events.")}</p>{turn.response.clarifications.map(question => <p key={question}>{t(question)}</p>)}</div> : turn.response.result && <>
              <div className="assistant-chat-bubble"><p>{t(turn.response.result.recommendations.length ? "Here are the events that match your plans." : "I couldn’t find an event matching all of that. Try another date, interest, or budget.")}</p></div>
              {turn.response.result.recommendations.length > 0 ? <ol className="assistant-chat-events">{turn.response.result.recommendations.map(({ event }) => {
                const sourceUrl = safeExternalUrl(event.sourceUrl, event.source);
                const eventIds = turn.response!.result!.recommendations.map(item => item.event.id);
                return <li className="assistant-chat-event" key={event.id}><EventLink eventId={event.id} eventIds={eventIds} className="assistant-chat-thumbnail" aria-label={event.title}><EventImage event={event} sizes="120px" /></EventLink><div className="assistant-chat-event-copy"><EventLink eventId={event.id} eventIds={eventIds} className="assistant-chat-event-title">{event.title}</EventLink><p className="assistant-chat-date"><Icon name="calendar" size={13} />{formatEventDate(event, locale)}</p><p className="assistant-chat-venue"><Icon name="pin" size={13} />{event.venue || t(event.city)}</p><strong className="assistant-chat-price">{formatPrice(event, locale)}</strong><div className="assistant-chat-links"><EventLink eventId={event.id} eventIds={eventIds}>{t("View event")}<Icon name="arrow" size={13} /></EventLink>{sourceUrl && <a href={sourceUrl} target="_blank" rel="noopener noreferrer">{t("Official page")}<Icon name="external" size={13} /></a>}</div></div></li>;
              })}</ol> : <Link className="text-link assistant-browse-link" href="/explore">{t("Browse all events")}<Icon name="arrow" size={15} /></Link>}
            </>}
          </div></div>}
          {turn.error && <div className="assistant-chat-message"><span className="assistant-message-icon"><Icon name="alert" size={16} /></span><div className="assistant-chat-bubble assistant-chat-error" role="alert"><p>{t(turn.error)}</p><div className="assistant-chat-error-actions">{index === turns.length - 1 && !endedChatCodes.includes(turn.errorCode || "") && <button className="text-link" disabled={busy} onClick={() => void send(turn.message, turn.id)}>{t("Try again")}</button>}{turn.errorCode === "ASSISTANT_LIMIT" && <Link className="text-link" href="/premium?returnTo=/assistant">{t("Try Premium Demo")}</Link>}{endedChatCodes.includes(turn.errorCode || "") && <button className="text-link" disabled={busy} onClick={() => reset(turn.message)}>{t("New chat")}</button>}</div></div></div>}
          {busy && index === turns.length - 1 && <div className="assistant-chat-message"><span className="assistant-message-icon"><Icon name="spark" size={16} /></span><p className="assistant-chat-bubble assistant-chat-typing" role="status"><span className="loader" />{t("Looking through our events…")}</p></div>}
        </div>)}
      </div>
      <form className="assistant-chat-composer" onSubmit={submit} aria-label={t("Send a message")}>
        <label htmlFor="assistant-message" className="sr-only">{t("Your message")}</label>
        <div className="assistant-chat-input"><textarea ref={input} id="assistant-message" rows={2} required maxLength={500} value={message} disabled={!ready} onChange={event => setMessage(event.target.value)} placeholder={t("Ask me to find something for you…")} onKeyDown={event => { if (event.key === "Enter" && !event.shiftKey && !event.nativeEvent.isComposing) { event.preventDefault(); event.currentTarget.form?.requestSubmit(); } }} /><button className="button button-primary" type="submit" disabled={!ready || busy || !message.trim()} aria-label={t("Send message")}><Icon name="arrow" size={20} /></button></div>
        <div className="assistant-chat-composer-note"><span>{t("Real events. Direct links. Your next plan.")}</span><span>{message.length}/500</span></div>
      </form>
    </section>
  </div>;
}
