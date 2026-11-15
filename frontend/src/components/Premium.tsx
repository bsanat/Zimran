"use client";

import Link from "next/link";
import { useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { useDemo } from "@/components/DemoProvider";
import { LoadingState } from "@/components/States";

export function Premium({ returnTo }: { returnTo?: string }) {
  const { t } = useLanguage();
  const { subscription, loading, error: serviceError, activate, deactivate, refresh } = useDemo();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [checkout, setCheckout] = useState(false);
  const active = subscription?.status === "DEMO_PREMIUM";
  async function update() {
    setBusy(true); setError("");
    try { if (active) await deactivate(); else await activate(); setCheckout(false); }
    catch (cause) { setError(cause instanceof Error ? cause.message : "We could not complete your request. Please try again."); }
    finally { setBusy(false); }
  }
  return <div className="container page-section member-shell">
    <div className="page-heading member-heading"><span className="eyebrow"><Icon name="spark" size={16} />OUTLY PREMIUM</span><h1>{t("More good plans. More people to share them with.")}</h1><p>{t("Find your company, choose together, and hear about the events you care about.")}</p></div>
    <div className="member-pricing">
      <section className="member-card"><span className="eyebrow">{t("START EXPLORING")}</span><h2>{t("Free")}</h2><div className="member-price">$0</div><p>{t("Your city is always open to explore.")}</p><ul className="member-benefits">{["Browse all verified events", "One assistant chat per week", "One shared planning trial", "Browse Event Buddy posts", "Report and block, always free"].map(text => <li key={text}><Icon name="check" size={18} />{t(text)}</li>)}</ul><Link className="button button-outline" href="/explore">{t("Explore events")}<Icon name="arrow" size={17} /></Link></section>
      <section className="member-card member-featured" aria-labelledby="premium-title"><span className="member-badge">{t(active ? "Premium Demo active" : "ONE PLAN, ALL FOUR EXPERIENCES")}</span><h2 id="premium-title">Premium</h2><div className="member-price">$4.99<span>/{t("month")}</span></div><p>{t("Proposed monthly price. No payment in demo.")}</p><ul className="member-benefits">{["More assistant recommendations", "Create Buddy posts and request to join", "More plans together with a private invitation", "Personal event alerts in your city", "Your categories, budget, and alert schedule"].map(text => <li key={text}><Icon name="check" size={18} />{t(text)}</li>)}</ul>
        {loading ? <LoadingState label={t("Checking demo access…")} /> : serviceError ? <div role="alert"><p>{t(serviceError)}</p><button className="button button-outline" onClick={() => void refresh()}>{t("Try again")}</button></div> : <div className="member-actions">{!active && <button className="button button-primary" onClick={() => setCheckout(true)}>{t("Subscribe")}<Icon name="arrow" size={17} /></button>}<button className={`button ${active ? "button-outline" : "button-plain"}`} disabled={busy} onClick={() => void update()}>{t(busy ? "Saving…" : active ? "End Premium Demo" : "Try Premium Demo")}</button></div>}
        <p className="field-help">{t("Demo access is limited to this guest session. No account or card required.")}</p>
      </section>
    </div>
    {checkout && <section className="member-card member-checkout" aria-labelledby="checkout-title"><h2 id="checkout-title">{t("Your Premium plan")}</h2><p className="member-price">$4.99<span>/{t("month")}</span></p><p>{t("Checkout is not connected yet. Try all Premium features in the free demo, with no charge or renewal.")}</p><div className="member-actions"><button className="button button-primary" disabled={busy} onClick={() => void update()}>{t("Continue with Premium Demo")}</button><button className="button button-outline" onClick={() => setCheckout(false)}>{t("Cancel")}</button></div></section>}
    {error && <p className="inline-error" role="alert">{t(error)}</p>}
    {active && <section className="member-card member-checkout"><h2>{t("Your demo is ready")}</h2><p>{t("Every social interaction and notification in this mode is clearly marked as a demo.")}</p><div className="member-actions">{returnTo && <Link href={returnTo} className="button button-primary">{t("Continue")}</Link>}<Link href="/buddy" className="button button-outline">{t("Find a Buddy")}</Link><Link href="/pair/new" className="button button-outline">{t("Plan together")}</Link><Link href="/settings/alerts" className="button button-primary">{t("Set up event alerts")}</Link></div></section>}
  </div>;
}
