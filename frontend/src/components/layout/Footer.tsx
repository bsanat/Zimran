import Link from "next/link";
import { getLanguage } from "@/lib/language-server";

export async function Footer() {
  const { t } = await getLanguage();
  return <footer className="site-footer"><div className="container footer-inner"><div className="footer-brand"><Link className="brand" href="/">outly<span className="brand-dot">.</span><span className="usa-label">USA</span></Link><p>{t("Discover your city. Make it your day.")}</p></div><nav aria-label={t("Footer navigation")}><div><strong>{t("Discover")}</strong><Link href="/explore">{t("Explore events")}</Link><Link href="/#interests-title">{t("Event categories")}</Link><Link href="/#locations">{t("States & cities")}</Link></div><div><strong>{t("Make it personal")}</strong><Link href="/planner">{t("Plan your day")}</Link><Link href="/#how-it-works">{t("How it works")}</Link><span>{t("No account needed")}</span></div></nav></div><div className="container footer-bottom"><span>Outly USA</span><span>{t("Real events. Your kind of experience.")}</span></div></footer>;
}
