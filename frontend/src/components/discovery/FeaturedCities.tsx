"use client";

import { createContext, useContext, useState, type ReactNode } from "react";
import Image from "next/image";
import Link from "next/link";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { cityPath } from "@/lib/city-path";
import type { CityPhoto } from "@/lib/city-photos";
import { HomeEvents } from "./HomeEvents";
import { PhotoCredit } from "./PhotoCredit";

type FeaturedCity = { state: string; city: string; photo: CityPhoto };
type CitySelection = {
  city: FeaturedCity | undefined;
  position: number;
  cities: FeaturedCity[];
  select: (index: number) => void;
};

const FeaturedContext = createContext<CitySelection | null>(null);

export function FeaturedCitiesProvider({ cities, children }: { cities: FeaturedCity[]; children: ReactNode }) {
  const [index, setIndex] = useState(0);
  const total = cities.length;
  const position = total ? index % total : 0;
  return <FeaturedContext.Provider value={{ city: cities[position], position, cities, select: setIndex }}>{children}</FeaturedContext.Provider>;
}

export function useFeaturedCity() {
  const selected = useContext(FeaturedContext);
  if (!selected) throw new Error("FeaturedCitiesProvider is required");
  return selected;
}

export function FeaturedCities() {
  const { city: current, position, cities, select } = useFeaturedCity();
  const { t } = useLanguage();
  if (!current) return null;
  const { city, state, photo } = current;
  return <>
    <section className="city-banner" aria-label={t("Featured cities")}>
      <Image key={photo.src} className="city-banner-photo" src={photo.src} alt={photo.title}
        fill sizes="100vw" loading="eager" fetchPriority="high" />
      <div className="container city-banner-copy">
        <div aria-live="polite" aria-atomic="true">
          <h1 id="city-title"><Link href={cityPath(state, city)}>{t(city)} <span><Icon name="arrow" size={16} /></span></Link></h1>
          <p>{t("Concerts, exhibitions and more to explore in the city.")}</p>
        </div>
        {cities.length > 1 && <div className="banner-navigation" role="group" aria-label={t("Switch city")}>
          {cities.map((item, index) => <button key={cityPath(item.state, item.city)} type="button"
            className="banner-dot" aria-label={t(item.city)} title={t(item.city)}
            aria-current={position === index ? "true" : undefined} onClick={() => select(index)} />)}
          <button className="banner-next" type="button" onClick={() => select((position + 1) % cities.length)}
            aria-label={t("Next city")} title={t("Next city")}><Icon name="arrow" size={14} /></button>
        </div>}
      </div>
      <PhotoCredit photo={photo} />
    </section>
    <HomeEvents key={cityPath(state, city)} city={city} state={state} />
  </>;
}
