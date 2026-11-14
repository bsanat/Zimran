import { translate, type Locale } from "./i18n";

export async function getLanguage() {
  const locale: Locale = "en";
  return { locale, t: (source: string) => translate(locale, source) };
}

export async function pageMetadata(title: string, description: string) {
  return { title, description };
}
