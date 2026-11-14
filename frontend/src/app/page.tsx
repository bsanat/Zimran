import Link from "next/link";
import { Icon } from "@/components/Icon";
import { FeaturedCities } from "@/components/discovery/FeaturedCities";
import { LocationDirectory } from "@/components/discovery/LocationDirectory";
import { getLanguage } from "@/lib/language-server";

export default async function Home() {
  const { t } = await getLanguage();
  return <>
    <FeaturedCities />
    <LocationDirectory />
    <section className="container home-scenarios" aria-labelledby="scenarios-title"><div className="section-heading"><h2 id="scenarios-title">{t("Good plans are better together.")}</h2></div><p className="section-intro">{t("Find a real event, a little company, or a plan you both love.")}</p><div className="home-scenario-grid">{[
      { href: "/assistant", icon: "spark", title: "AI Assistant", text: "Tell us what you have in mind. Find real events that fit.", action: "Find my next plan" },
      { href: "/buddy", icon: "people", title: "Event Buddy", text: "Find someone who wants to go to the same event.", action: "Find a Buddy" },
      { href: "/pair/new", icon: "wellness", title: "Plan together", text: "Two people, two wish lists. One plan you both choose.", action: "Start a shared plan" },
      { href: "/settings/alerts", icon: "bell", title: "Personal event alerts", text: "Your favorite categories and city. Something to look forward to.", action: "Set up event alerts" },
    ].map(item => <Link key={item.href} href={item.href} className="home-scenario"><span className="member-icon"><Icon name={item.icon} size={23} /></span><h3>{t(item.title)}</h3><p>{t(item.text)}</p><span className="text-link">{t(item.action)}<Icon name="arrow" size={16} /></span></Link>)}</div></section>
    <section id="how-it-works" className="how-section"><div className="container"><div className="section-heading"><h2>{t("Your next great day, in three steps")}</h2></div><div className="how-grid"><article><span className="step-number">01</span><h3>{t("Tell us what you love")}</h3><p>{t("Pick your interests, your dates, and what you’d like to spend.")}</p></article><article><span className="step-number">02</span><h3>{t("Find your kind of plans")}</h3><p>{t("Get up to five real events, with a reason each one could be right for you.")}</p></article><article><span className="step-number">03</span><h3>{t("Make it happen")}</h3><p>{t("See the details and head to the original source when something catches your eye.")}</p></article></div></div></section>
    <section className="container"><div className="closing-cta"><span className="closing-mark"><Icon name="spark" size={40} /></span><div><span className="eyebrow">{t("LESS PLANNING. MORE LIVING.")}</span><h2>{t("Your interests. A whole city of possibilities.")}</h2><p>{t("Let’s find your next good reason to go out. No account needed.")}</p></div><Link className="button button-light" href="/planner">{t("Create my plan")}<Icon name="arrow" size={18} /></Link></div></section>
  </>;
}
