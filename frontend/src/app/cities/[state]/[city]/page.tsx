import Image from "next/image";
import Link from "next/link";
import { notFound } from "next/navigation";
import { CityClock } from "@/components/discovery/CityClock";
import { CityEvents } from "@/components/discovery/CityEvents";
import { Icon } from "@/components/Icon";
import { CATALOG_EVENTS, catalogRegions, filterCatalogEvents } from "@/lib/catalog";
import { PhotoCredit } from "@/components/discovery/PhotoCredit";
import { cityPhoto } from "@/lib/city-photos";
import { citySlug } from "@/lib/city-path";
import { getLanguage } from "@/lib/language-server";
import "./city.css";

type CityPageProps = { params: Promise<{ state: string; city: string }> };

async function resolveCity(params: CityPageProps["params"]) {
  const { state, city } = await params;
  const region = catalogRegions.find(item => item.code === state.toUpperCase());
  const slug = citySlug(city.replaceAll("%20", " "));
  const name = region?.cities.find(item => citySlug(item) === slug);
  if (!region || !name) notFound();
  return { region, name };
}

export async function generateMetadata({ params }: CityPageProps) {
  const { name } = await resolveCity(params);
  const { t } = await getLanguage();
  return { title: `${t(name)} · ${t("City guide")}` };
}

export default async function CityPage({ params }: CityPageProps) {
  const { region, name } = await resolveCity(params);
  const { locale, t } = await getLanguage();
  const filters = { state: region.code, city: name };
  const events = filterCatalogEvents(filters);
  const featured = events[0] ?? CATALOG_EVENTS.find(event => event.state === region.code && event.city === name)!;
  const isNewYork = region.code === "NY" && name === "New York";
  const photo = cityPhoto(region.code, name);
  const venues = [...new Set(events.map(event => event.venue).filter((venue): venue is string => Boolean(venue)))].slice(0, 3);
  let summary = t("Explore local events by date and interest.");
  if (isNewYork) summary = t("From chess nights in Brooklyn to group workouts in Central Park, find your next day out in New York.");
  else if (venues.length) summary = t("Explore local events at {places}.").replace("{places}", new Intl.ListFormat(locale).format(venues));
  const initialTime = new Intl.DateTimeFormat(locale, {
    timeZone: featured.timezone, hour: "2-digit", minute: "2-digit", timeZoneName: "shortOffset",
  }).format(new Date());

  return <>
    <section className={`city-guide-hero${isNewYork ? " city-guide-panorama" : ""}`} aria-labelledby="city-guide-title">
      <figure className="city-guide-media">
        <Image src={photo.src} alt={photo.title} fill sizes={isNewYork ? "100vw" : "70vw"} loading="eager" fetchPriority="high" />
      </figure>
      <div className="container city-guide-inner">
        <div className="city-guide-copy">
          <p className="city-guide-kicker">{t("City guide")} <span> / </span> {t(region.name)}</p>
          <h1 id="city-guide-title">{t(name)}</h1>
          <CityClock timezone={featured.timezone} initialTime={initialTime} />
          <p className="city-guide-intro">{summary}</p>
          <Link className="city-guide-switch" href="/#locations"><Icon name="pin" size={16} />{t("Choose another city")}<Icon name="arrow" size={16} /></Link>
        </div>
      </div>
      <PhotoCredit photo={photo} />
    </section>
    <CityEvents state={region.code} city={name} />
  </>;
}
