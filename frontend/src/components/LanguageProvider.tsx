"use client";
import { createContext, useContext } from "react";
import { translate, type Locale } from "@/lib/i18n";

const LanguageContext = createContext<Locale>("ru");

export function LanguageProvider({ locale, children }: { locale: Locale; children: React.ReactNode }) {
  return <LanguageContext.Provider value={locale}>{children}</LanguageContext.Provider>;
}

export function useLanguage() {
  const locale = useContext(LanguageContext);
  return { locale, t: (source: string) => translate(locale, source) };
}
