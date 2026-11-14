"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useTransition } from "react";
import { Icon } from "@/components/Icon";
import { useLanguage } from "@/components/LanguageProvider";
import { useDemo } from "@/components/DemoProvider";
import { isLocale, languages } from "@/lib/i18n";
import { useFeaturedCity } from "@/components/discovery/FeaturedCities";
import { cityPath } from "@/lib/city-path";
import { setLanguage } from "@/app/language-action";

export function Header() {
  const pathname = usePathname();
  const { locale, t } = useLanguage();
  const { unreadCount } = useDemo();
  const { city: featured } = useFeaturedCity();
  const onCityPage = pathname.startsWith("/cities/");
  const [pending, startTransition] = useTransition();
  return <header className="site-header">
    <div className="container header-inner">
      <Link href="/" className="brand" aria-label={t("Outly USA home")}>outly<span className="brand-dot">.</span><span className="usa-label">USA</span></Link>
      <nav className="main-nav" aria-label={t("Main navigation")}>
        <Link href={onCityPage || !featured ? "/#locations" : cityPath(featured.state, featured.city)} className="header-city"><Icon name="pin" size={18} />{t(onCityPage || !featured ? "Choose another city" : featured.city)}</Link>
        <Link href="/#interests-title" className="header-categories"><Icon name="grid" size={17} />{t("Categories")}</Link>
      </nav>
      <form className="header-search" action="/explore" role="search"><label className="sr-only" htmlFor="site-search">{t("Search events and experiences")}</label><button type="submit" aria-label={t("Search events")}><Icon name="search" size={18} /></button><input id="site-search" type="search" name="q" maxLength={100} placeholder={t("What do you want to do?")} autoComplete="off" /></form>
      <label className="language-picker" title={t("Interface language")}>
        <Icon name="globe" size={17} /><span aria-hidden="true">{locale.toUpperCase()}</span><Icon name="chevron" size={12} />
        <select aria-label={t("Interface language")} value={locale} disabled={pending} onChange={event => {
          const value = event.target.value;
          if (isLocale(value)) startTransition(() => setLanguage(value));
        }}>{Object.entries(languages).map(([code, name]) => <option key={code} value={code} lang={code}>{name}</option>)}</select>
      </label>
      <Link href="/planner" className="button button-primary header-plan" aria-label={t("Plan My Day")} aria-current={pathname.startsWith("/planner") ? "page" : undefined}><Icon name="spark" size={17} /><span>{t("Plan My Day")}</span></Link>
    </div>
    <nav className="product-nav" aria-label={t("Plan and connect")}><div className="container">
      {[{ href: "/explore", label: "Explore events", icon: "grid" }, { href: "/assistant", label: "AI Assistant", icon: "spark" }, { href: "/buddy", label: "Find a Buddy", icon: "people" }, { href: "/pair/new", label: "Plan together", icon: "wellness" }, { href: "/notifications", label: "Notifications", icon: "bell" }].map(item => <Link key={item.href} href={item.href} aria-current={pathname.startsWith(item.href === "/pair/new" ? "/pair/" : item.href) ? "page" : undefined}><Icon name={item.icon} size={15} />{t(item.label)}{item.href === "/notifications" && unreadCount > 0 && <span className="member-count" aria-label={`${t("Unread notifications")}: ${unreadCount}`}>{unreadCount}</span>}</Link>)}
      <Link href="/premium" className="product-nav-premium" aria-current={pathname === "/premium" ? "page" : undefined}><Icon name="spark" size={15} />Premium</Link>
    </div></nav>
  </header>;
}
