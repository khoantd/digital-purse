// App money is Vietnamese đồng (VND). Display uses vi-VN locale; no fractional digits.

export const APP_CURRENCY = 'VND';
export const APP_LOCALE = 'vi-VN';

const vndFormatter = new Intl.NumberFormat(APP_LOCALE, {
  style: 'currency',
  currency: APP_CURRENCY,
  maximumFractionDigits: 0,
});

const compactFormatter = new Intl.NumberFormat(APP_LOCALE, {
  notation: 'compact',
  maximumFractionDigits: 1,
});

// ----------------------------------------------------------------------

export function fNumber(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '0';
  }
  return new Intl.NumberFormat(APP_LOCALE).format(n);
}

export function fCurrency(number) {
  const n = Number(number);
  return vndFormatter.format(Number.isFinite(n) ? n : 0);
}

export function fPercent(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '';
  }
  return new Intl.NumberFormat(APP_LOCALE, {
    style: 'percent',
    maximumFractionDigits: 1,
  }).format(n / 100);
}

export function fShortenNumber(number) {
  const n = Number(number);
  if (!Number.isFinite(n)) {
    return '0';
  }
  return compactFormatter.format(n);
}

export function fData(number) {
  const n = Number(number);
  if (!Number.isFinite(n) || n === 0) {
    return '';
  }
  return new Intl.NumberFormat(APP_LOCALE, {
    style: 'unit',
    unit: 'byte',
    unitDisplay: 'narrow',
    notation: 'compact',
    maximumFractionDigits: 1,
  }).format(n);
}
