import Link from "next/link";
import { getLanguage } from "@/lib/language-server";

export async function Footer() {
  const { t } = await getLanguage();
  return <footer className="site-footer">
    <div className="container footer-inner">
      <p className="footer-tagline">{t("Discover your city. Make it your day.")}</p>
      <nav aria-label={t("Footer navigation")}>
        <div>
          <strong>{t("Discover")}</strong>
          <Link href="/explore">{t("Explore events")}</Link>
          <Link href="/#interests-title">{t("Event categories")}</Link>
          <Link href="/#locations">{t("States & cities")}</Link>
        </div>
        <div>
          <strong>{t("Make it personal")}</strong>
          <Link href="/planner">{t("Plan your day")}</Link>
          <Link href="/assistant">{t("AI Assistant")}</Link>
          <Link href="/#how-it-works">{t("How it works")}</Link>
          <span>{t("No account needed")}</span>
        </div>
        <div><strong>{t("Go together")}</strong><Link href="/buddy">{t("Find a Buddy")}</Link><Link href="/pair/new">{t("Plan together")}</Link><Link href="/premium">Premium</Link><Link href="/notifications">{t("Notifications")}</Link></div>
      </nav>
    </div>
    <div className="container footer-bottom"><span>Outly USA</span><span>{t("Real events. Your kind of experience.")}</span></div>
  </footer>;
}
