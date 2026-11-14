import { FeaturedCitiesProvider } from "@/components/discovery/FeaturedCities";
import { filterCatalogEvents } from "@/lib/catalog";
import { cityPhoto } from "@/lib/city-photos";
import { topEventCities } from "@/lib/discovery";
import { EventOrderProvider } from "@/components/events/EventLink";
import type { Metadata } from "next";
import { LanguageProvider } from "@/components/LanguageProvider";
import { DemoProvider } from "@/components/DemoProvider";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { getLanguage, pageMetadata } from "@/lib/language-server";
import "./globals.css";
import "./pages.css";
import "./social.css";
import "./assistant.css";
import "./membership.css";

export async function generateMetadata(): Promise<Metadata> {
  const { title, description } = await pageMetadata("Outly USA — Your city. Your kind of day.", "Discover your kind of New York. Choose your interests, date, and budget for personal recommendations from real events. No account needed.");
  return { title: { default: title, template: "%s | Outly USA" }, description };
}

export default async function RootLayout({ children, eventPanel }: { children: React.ReactNode; eventPanel: React.ReactNode }) {
  const { locale, t } = await getLanguage();
  const cities = topEventCities(filterCatalogEvents()).map(({ state, city }) => ({ state, city, photo: cityPhoto(state, city) }));
  return <html lang={locale} data-scroll-behavior="smooth"><body><LanguageProvider locale={locale}><DemoProvider><a href="#main-content" className="skip-link">{t("Skip to content")}</a><EventOrderProvider><FeaturedCitiesProvider cities={cities}><Header /><main id="main-content">{children}</main><Footer />{eventPanel}</FeaturedCitiesProvider></EventOrderProvider></DemoProvider></LanguageProvider></body></html>;
}
