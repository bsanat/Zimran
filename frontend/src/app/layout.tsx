import type { Metadata } from "next";
import { LanguageProvider } from "@/components/LanguageProvider";
import { Header } from "@/components/layout/Header";
import { Footer } from "@/components/layout/Footer";
import { getLanguage, pageMetadata } from "@/lib/language-server";
import "./globals.css";
import "./pages.css";

export async function generateMetadata(): Promise<Metadata> {
  const { title, description } = await pageMetadata("Outly USA — Your city. Your kind of day.", "Discover your kind of New York. Choose your interests, date, and budget for personal recommendations from real events. No account needed.");
  return { title: { default: title, template: "%s | Outly USA" }, description };
}

export default async function RootLayout({ children }: { children: React.ReactNode }) {
  const { locale, t } = await getLanguage();
  return <html lang={locale} data-scroll-behavior="smooth"><body><LanguageProvider locale={locale}><a href="#main-content" className="skip-link">{t("Skip to content")}</a><Header /><main id="main-content">{children}</main><Footer /></LanguageProvider></body></html>;
}
