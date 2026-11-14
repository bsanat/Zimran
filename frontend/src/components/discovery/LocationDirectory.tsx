"use client";

import Link from "next/link";
import { useState } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { getStateByCode, isSupportedCity, US_STATES } from "@/lib/locations";
import { UnitedStatesMap } from "./UnitedStatesMap";

export function LocationDirectory() {
  const { t } = useLanguage();
  const [stateCode, setStateCode] = useState("NY");
  const [city, setCity] = useState("");
  const state = getStateByCode(stateCode) ?? US_STATES[0];
  const cities = city ? state.cities.filter(name => name === city) : state.cities;

  function selectState(code: string) {
    setStateCode(code);
    setCity("");
  }

  return <section id="locations" className="container location-directory" aria-labelledby="locations-title">
    <div className="section-heading"><div><span className="eyebrow">{t("BROWSE BY STATE & CITY")}</span><h2 id="locations-title">{t("Find your corner of the country.")}</h2></div></div>
    <p className="location-scope-note">{t("Your next stop starts here. Choose one of all 50 states on the map or with the filters below. Event discovery currently supports New York.")}</p>
    <div className="location-controls">
      <div><label className="field-label" htmlFor="directory-state">{t("Choose a state")}</label><div className="select-control"><select id="directory-state" value={stateCode} onChange={event => selectState(event.target.value)}>{US_STATES.map(item => <option key={item.code} value={item.code}>{t(item.name)}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
      <div><label className="field-label" htmlFor="directory-city">{t("City")}</label><div className="select-control"><select id="directory-city" value={city} onChange={event => setCity(event.target.value)}><option value="">{t("All cities")}</option>{state.cities.map(name => <option key={name} value={name}>{t(name)}</option>)}</select><Icon name="chevron" size={18} className="select-chevron" /></div></div>
    </div>
    <UnitedStatesMap selectedCode={stateCode} onSelect={selectState} />
    <div className="location-state-summary" aria-live="polite"><h3>{t(state.name)}</h3><span>{state.code}</span></div>
    <div className="city-directory-grid" aria-live="polite">
      {cities.map(city => isSupportedCity(city, state.code) ? <Link className="city-directory-card city-live" href={`/explore?state=${state.code}&city=${encodeURIComponent(city)}`} key={city}><Icon name="pin" size={22} /><div><strong>{t(city)}</strong><span>{t(state.name)}</span></div><span>{t("Explore events")} <Icon name="arrow" size={16} /></span></Link> : <div className="city-directory-card city-coming-soon" key={city}><Icon name="pin" size={22} /><div><strong>{t(city)}</strong><span>{t(state.name)}</span></div><span>{t("Coming soon")}</span></div>)}
    </div>
    <p className="location-scope-note">{t("More cities will join as event coverage grows. Cities marked “Coming soon” aren’t available for event searches yet.")}</p>
  </section>;
}
