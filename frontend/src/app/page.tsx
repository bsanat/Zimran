import Link from "next/link";
import Image from "next/image";
import { Icon } from "@/components/Icon";
import { INTERESTS } from "@/lib/planner";
import { InterestStrip } from "@/components/discovery/InterestStrip";
import { UpcomingEvents } from "@/components/discovery/UpcomingEvents";
import { LocationDirectory } from "@/components/discovery/LocationDirectory";
import { getLanguage } from "@/lib/language-server";

const highlights = [
  { id: "music", title: "Live music", image: "/photos/music.jpg" },
  { id: "art", title: "Arts & culture", image: "/photos/art.jpg" },
  { id: "food", title: "Food & drinks", image: "/photos/food.jpg" },
];

export default async function Home() {
  const { t } = await getLanguage();
  return <>
    <section className="city-banner" aria-labelledby="city-title">
      <Image
        className="city-banner-photo" src="/photos/manhattan-panorama.webp" alt={t("Manhattan skyline seen from Liberty Island")}
        fill sizes="100vw" loading="eager" fetchPriority="high"
      />
      <div className="container city-banner-copy">
        <h1 id="city-title">
          <a href="#locations" aria-label={t("New York — choose a city")}>
            {t("New York")} <span><Icon name="chevron" size={16} /></span>
          </a>
        </h1>
        <p>{t("Concerts, exhibitions and more to explore in the city.")}</p>
      </div>
    </section>
    <InterestStrip />
    <section className="container home-discovery" aria-labelledby="city-picks">
      <div className="section-heading">
        <h2 id="city-picks">{t("Things to do in New York")}</h2>
        <Link className="text-link" href="/explore">{t("Explore all events")}</Link>
      </div>
      <nav className="home-highlights" aria-label={t("Event categories")}>
        {highlights.map(item => (
          <Link key={item.id} href={`/explore?category=${item.id}`} className="home-highlight">
            <span className="highlight-photo">
              <Image src={item.image} alt="" fill sizes="(min-width: 1344px) 416px, 32vw" loading="eager" />
            </span>
            <span className="highlight-caption">
              {t(item.title)} <Icon name="arrow" size={17} />
            </span>
          </Link>
        ))}
      </nav>
    </section>
    <UpcomingEvents />
    <section className="container interests-section" aria-labelledby="interests-title"><div className="section-heading"><div><span className="eyebrow">{t("FOLLOW YOUR CURIOSITY")}</span><h2 id="interests-title">{t("Event categories")}</h2></div><Link href="/planner" className="text-link">{t("Make it personal")} <Icon name="arrow" size={17} /></Link></div><p className="section-intro">{t("Your people, your interests, your next good reason to go out.")}</p><div className="category-grid discovery-category-grid">{INTERESTS.map(category => <Link href={`/explore?category=${category.id}`} className={`category-tile ${category.color}`} key={category.id} aria-label={`${t("Explore events")}: ${t(category.label)}`}><span className="category-artwork">{category.image ? <img src={category.image} alt="" width={64} height={64} loading="lazy" /> : <Icon name={category.id} size={31} />}</span><span className="category-name">{t(category.label)}</span><span className="category-detail">{t(category.detail)}</span><Icon name="arrow" size={16} className="category-arrow" /></Link>)}</div><p className="coverage-note">{t("Discover what’s available for your dates. Event coverage varies by interest.")}</p></section>
    <LocationDirectory />
    <section id="how-it-works" className="how-section"><div className="container"><div className="section-heading"><h2>{t("Your next great day, in three steps")}</h2></div><div className="how-grid"><article><span className="step-number">01</span><h3>{t("Tell us what you love")}</h3><p>{t("Pick your interests, your dates, and what you’d like to spend.")}</p></article><article><span className="step-number">02</span><h3>{t("Find your kind of plans")}</h3><p>{t("Get up to five real events, with a reason each one could be right for you.")}</p></article><article><span className="step-number">03</span><h3>{t("Make it happen")}</h3><p>{t("See the details and head to the original source when something catches your eye.")}</p></article></div></div></section>
    <section className="container"><div className="closing-cta"><span className="closing-mark"><Icon name="spark" size={40} /></span><div><span className="eyebrow">{t("LESS PLANNING. MORE LIVING.")}</span><h2>{t("Your interests. A whole city of possibilities.")}</h2><p>{t("Let’s find your next good reason to go out. No account needed.")}</p></div><Link className="button button-light" href="/planner">{t("Create my plan")}<Icon name="arrow" size={18} /></Link></div></section>
  </>;
}
