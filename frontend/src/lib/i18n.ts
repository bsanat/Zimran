/** The interface is English only; UI strings are written in English in the components. */
export type Locale = "en";

/** Kept so components can wrap UI strings in one place; returns the English source unchanged. */
export function translate(_locale: Locale, source: string): string {
  return source;
}
