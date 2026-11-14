"use client";

import Link from "next/link";
import { useEffect, useRef, useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { INTERESTS } from "@/lib/planner";

const firstInterests = ["music", "art", "family", "nightlife", "outdoors", "food", "wellness"];
const interests = [
  ...firstInterests.map(id => INTERESTS.find(interest => interest.id === id)!),
  ...INTERESTS.filter(interest => !firstInterests.includes(interest.id)),
];

export function InterestStrip() {
  const { locale, t } = useLanguage();
  const strip = useRef<HTMLElement>(null);
  const [overflow, setOverflow] = useState(false);
  const [atEnd, setAtEnd] = useState(false);

  useEffect(() => {
    const nav = strip.current!;
    const update = () => {
      setOverflow(nav.scrollWidth > nav.clientWidth + 2);
      setAtEnd(nav.scrollLeft + nav.clientWidth >= nav.scrollWidth - 2);
    };
    const observer = new ResizeObserver(update);
    observer.observe(nav);
    nav.addEventListener("scroll", update, { passive: true });
    update();
    return () => { observer.disconnect(); nav.removeEventListener("scroll", update); };
  }, [locale]);

  function move() {
    const nav = strip.current!;
    const behavior = window.matchMedia("(prefers-reduced-motion: reduce)").matches ? "instant" : "smooth";
    if (atEnd) nav.scrollTo({ left: 0, behavior });
    else nav.scrollBy({ left: Math.max(160, nav.clientWidth * .75), behavior });
  }

  return <section className="container home-interests" aria-labelledby="home-interests-title">
    <h2 id="home-interests-title">{t("Explore by interest")}</h2>
    <div className="interest-strip-shell">
      <nav ref={strip} id="home-interest-strip" className="interest-strip" aria-labelledby="home-interests-title">
        <Link href="/explore" className="interest-chip interest-chip-all">{t("All")}</Link>
        {interests.map(interest => <Link key={interest.id} href={`/explore?category=${interest.id}`} className="interest-chip">{t(interest.label)}</Link>)}
      </nav>
      {overflow && <button type="button" className="interest-strip-next" onClick={move} aria-controls="home-interest-strip" aria-label={t(atEnd ? "Back to first interest" : "More interests")} title={t(atEnd ? "Back to first interest" : "More interests")}><Icon name={atEnd ? "back" : "arrow"} size={18} /></button>}
    </div>
  </section>;
}
