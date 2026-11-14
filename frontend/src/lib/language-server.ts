import { cookies } from "next/headers";
import { isLocale, languageCookie, translate } from "./i18n";

export async function getLanguage() {
  const saved = (await cookies()).get(languageCookie)?.value;
  const locale = isLocale(saved) ? saved : "ru";
  return { locale, t: (source: string) => translate(locale, source) };
}

export async function pageMetadata(title: string, description: string) {
  const { t } = await getLanguage();
  return { title: t(title), description: t(description) };
}
