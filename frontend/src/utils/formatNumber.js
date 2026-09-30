// App money is Vietnamese đồng (VND). Display locale follows active i18n language.

import i18n from '../i18n';
import { localeForLang } from '../i18n/storage';

export const APP_CURRENCY = 'VND';

export function getNumberLocale() {
  return localeForLang(i18n.language);
}

function vndFormatter() {
  return new Intl.NumberFormat(getNumberLocale(), {
    style: 'currency',
    currency: APP_CURRENCY,
    maximumFractionDigits: 0,
  });
}

function compactFormatter() {
  return new Intl.NumberFormat(getNumberLocale(), {
    notation: 'compact',
    maximumFractionDigits: 1,
  });
}

// ----------------------------------------------------------------------

export function fNumber(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '0';
  }
  return new Intl.NumberFormat(getNumberLocale()).format(n);
}

export function fCurrency(number) {
  const n = Number(number);
  return vndFormatter().format(Number.isFinite(n) ? n : 0);
}

export function fPercent(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '';
  }
  return new Intl.NumberFormat(getNumberLocale(), {
    style: 'percent',
    maximumFractionDigits: 1,
  }).format(n / 100);
}

export function fShortenNumber(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '0';
  }
  return compactFormatter().format(n);
}

export function fData(number) {
  const n = Number(number);
  if (!Number.isFinite(n) || n === 0) {
    return '';
  }
  return new Intl.NumberFormat(getNumberLocale(), {
    style: 'unit',
    unit: 'byte',
    unitDisplay: 'narrow',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(n);
}
