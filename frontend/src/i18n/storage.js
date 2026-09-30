export const LANG_STORAGE_KEY = 'ewallet.lang';
export const SUPPORTED_LANGS = ['vi', 'en'];
export const DEFAULT_LANG = 'vi';

export function readStoredLang() {
  try {
    const stored = localStorage.getItem(LANG_STORAGE_KEY);
    if (SUPPORTED_LANGS.includes(stored)) {
      return stored;
    }
  } catch {
    // ignore
  }
  return DEFAULT_LANG;
}

export function writeStoredLang(lang) {
  try {
    localStorage.setItem(LANG_STORAGE_KEY, lang);
  } catch {
    // ignore
  }
}

export function localeForLang(lang) {
  return lang === 'en' ? 'en-US' : 'vi-VN';
}

export function displayCodeForLang(lang) {
  return lang === 'en' ? 'EN' : 'VN';
}
