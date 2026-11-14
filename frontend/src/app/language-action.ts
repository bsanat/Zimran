"use server";
import { cookies } from "next/headers";
import { isLocale, languageCookie, type Locale } from "@/lib/i18n";

export async function setLanguage(locale: Locale) {
  if (!isLocale(locale)) throw new Error("Unsupported language");
  (await cookies()).set(languageCookie, locale, { path: "/", maxAge: 365 * 86400, sameSite: "lax", httpOnly: true });
}
