import { i18n } from '@lingui/core';
import { detect, fromUrl, fromStorage, fromNavigator } from '@lingui/detect-locale';
import { getDirection, resolveLocale, sourceLocale, type Locale } from './locales';

export * from './locales';

export function detectLocale(): Locale {
  try {
    return resolveLocale(detect(fromUrl('lang'), fromStorage('lang'), fromNavigator()));
  } catch {
    // Sandboxed iframes (CodeSandbox, some embeds) throw SecurityError on
    // localStorage access before the app renders anything at all.
    return resolveLocale(detect(fromUrl('lang'), fromNavigator()));
  }
}

export async function loadCatalog(locale: Locale) {
  try {
    const { messages } = await import(`../locales/${locale}/messages.po`);
    i18n.loadAndActivate({ locale, messages });
  } catch (e) {
    console.error(`Catalog for "${locale}" failed to load, using "${sourceLocale}"`, e);
    const { messages } = await import(`../locales/${sourceLocale}/messages.po`);
    i18n.loadAndActivate({ locale: sourceLocale, messages });
  }
  document.documentElement.lang = i18n.locale;
  document.documentElement.dir = getDirection(i18n.locale);
}

export function saveLocale(locale: Locale) {
  try {
    localStorage.setItem('lang', locale);
  } catch {
    /* sandboxed iframe — the URL param below still persists the choice */
  }
  const url = new URL(window.location.href);
  url.searchParams.set('lang', locale);
  history.replaceState(history.state, '', url);
}

export { i18n };
