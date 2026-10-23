export const locales = ['en', 'uk', 'es-ES'] as const;
export type Locale = (typeof locales)[number];
export const sourceLocale: Locale = 'en';

const RTL_LOCALES = new Set(['ar', 'he', 'fa', 'ur']);
export const getDirection = (locale: string): 'ltr' | 'rtl' =>
  RTL_LOCALES.has(locale.split('-')[0]) ? 'rtl' : 'ltr';

// "Deutsch", not "German" — each language name rendered in its own language
export const localeDisplayName = (locale: string) =>
  new Intl.DisplayNames([locale], { type: 'language' }).of(locale) ?? locale;

// `null` is in the signature on purpose: detect() and headers.get() both return it
export function resolveLocale(candidate: string | null | undefined): Locale {
  if (!candidate) return sourceLocale;
  if ((locales as readonly string[]).includes(candidate)) return candidate as Locale;
  const base = candidate.split('-')[0]; // es-MX → es
  return (locales as readonly string[]).includes(base) ? (base as Locale) : sourceLocale;
}
