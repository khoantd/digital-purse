import { format, getTime, formatDistanceToNow, parse, isValid, startOfDay, endOfDay } from 'date-fns';

// Backend TransactionResponseMapper uses Constants.DATE_TIME_FORMAT
const BACKEND_DATE_TIME = 'dd.MM.yyyy HH:mm:ss';
const BACKEND_DATE = 'dd.MM.yyyy';

export function toDate(value) {
  if (value == null || value === '') {
    return null;
  }
  if (value instanceof Date) {
    return isValid(value) ? value : null;
  }
  if (typeof value === 'number') {
    const fromNumber = new Date(value);
    return isValid(fromNumber) ? fromNumber : null;
  }

  const raw = String(value).trim();
  if (!raw) {
    return null;
  }

  // Prefer explicit backend patterns (Firefox rejects dotted dates via Date())
  if (/^\d{2}\.\d{2}\.\d{4} \d{2}:\d{2}:\d{2}$/.test(raw)) {
    const parsed = parse(raw, BACKEND_DATE_TIME, new Date());
    return isValid(parsed) ? parsed : null;
  }
  if (/^\d{2}\.\d{2}\.\d{4}$/.test(raw)) {
    const parsed = parse(raw, BACKEND_DATE, new Date());
    return isValid(parsed) ? parsed : null;
  }

  const native = new Date(raw);
  return isValid(native) ? native : null;
}

/** Parse an HTML date input (YYYY-MM-DD) to start of local day, or null. */
export function parseDateInputStart(value) {
  if (!value) return null;
  const parsed = parse(String(value).trim(), 'yyyy-MM-dd', new Date());
  return isValid(parsed) ? startOfDay(parsed) : null;
}

/** Parse an HTML date input (YYYY-MM-DD) to end of local day, or null. */
export function parseDateInputEnd(value) {
  if (!value) return null;
  const parsed = parse(String(value).trim(), 'yyyy-MM-dd', new Date());
  return isValid(parsed) ? endOfDay(parsed) : null;
}

// ----------------------------------------------------------------------

export function fDate(date, newFormat) {
  const fm = newFormat || 'dd MMM yyyy';
  const parsed = toDate(date);
  return parsed ? format(parsed, fm) : '';
}

export function fDateTime(date, newFormat) {
  const fm = newFormat || 'dd MMM yyyy p';
  const parsed = toDate(date);
  return parsed ? format(parsed, fm) : '';
}

export function fTimestamp(date) {
  const parsed = toDate(date);
  return parsed ? getTime(parsed) : '';
}

export function fToNow(date) {
  const parsed = toDate(date);
  return parsed
    ? formatDistanceToNow(parsed, {
        addSuffix: true,
      })
    : '';
}
