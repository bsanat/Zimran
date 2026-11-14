"use client";

import Link from "next/link";
import { useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { catalogRegions, filterCatalogEvents } from "@/lib/catalog";
import { cityPath } from "@/lib/city-path";
import { UnitedStatesMap } from "./UnitedStatesMap";

export function LocationDirectory() {
  const { t } = useLanguage();
  const [stateCode, setStateCode] = useState("NY");
  const [city, setCity] = useState("");
  const state = catalogRegions.find(state => state.code === stateCode) ?? catalogRegions[0];
  const cities = city ? state.cities.filter(name => name === city) : state.cities;

  function selectState(code: string) {
    setStateCode(code);
    setCity("");
  }

  return <section id="locations" className="container location-directory" aria-labelledby="locations-title">
    <div className="section-heading"><div><span className="eyebrow">{t("BROWSE BY STATE & CITY")}</span><h2 id="locations-title">{t("Find your corner of the country.")}</h2></div></div>
    <p className="location-scope-note">{t("Choose any state on the map, then open a city to see its verified listings.")}</p>
    <div className="location-controls">
      <div><label className="field-label" htmlFor="directory-state">{t("Choose a state")}</label><div className="select-control"><select id="directory-state" value={stateCode} onChange={event => selectState(event.target.value)}>{catalogRegions.map(item => <option key={item.code} value={item.code}>{t(item.name)} ({filterCatalogEvents({ state: item.code }).length})</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label className="field-label" htmlFor="directory-city">{t("City")}</label><div className="select-control"><select id="directory-city" value={city} onChange={event => setCity(event.target.value)}><option value="">{t("All cities")}</option>{state.cities.map(name => <option key={name} value={name}>{t(name)}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
    </div>
    <UnitedStatesMap selectedCode={stateCode} onSelect={selectState} />
    <div className="location-state-summary" aria-live="polite"><h3>{t(state.name)}</h3><span>{state.code}</span></div>
    <div className="city-directory-grid" aria-live="polite">
      {cities.map(city => <Link className="city-directory-card city-live" href={cityPath(state.code, city)} key={city}><Icon name="pin" size={22} /><div><strong>{t(city)}</strong><span>{t(state.name)}</span></div><span>{t("Events: {count}").replace("{count}", String(filterCatalogEvents({ state: state.code, city }).length))} <Icon name="arrow" size={16} /></span></Link>)}
    </div>
    <p className="location-scope-note">{t("Counts show the checked listings in this catalog. More events may be available on the official websites.")}</p>
  </section>;
}
