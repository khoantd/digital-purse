import i18n from 'i18next';
import { initReactI18next } from 'react-i18next';
import { readStoredLang, writeStoredLang } from './storage';

import enCommon from '../locales/en/common.json';
import enNav from '../locales/en/nav.json';
import enPageTitles from '../locales/en/pageTitles.json';
import enAuth from '../locales/en/auth.json';
import enDashboard from '../locales/en/dashboard.json';
import enWallets from '../locales/en/wallets.json';
import enTransfers from '../locales/en/transfers.json';
import enTransactions from '../locales/en/transactions.json';
import enCustomers from '../locales/en/customers.json';
import enApprovals from '../locales/en/approvals.json';
import enActivity from '../locales/en/activity.json';
import enSettings from '../locales/en/settings.json';
import enHeader from '../locales/en/header.json';
import enLanguage from '../locales/en/language.json';

import viCommon from '../locales/vi/common.json';
import viNav from '../locales/vi/nav.json';
import viPageTitles from '../locales/vi/pageTitles.json';
import viAuth from '../locales/vi/auth.json';
import viDashboard from '../locales/vi/dashboard.json';
import viWallets from '../locales/vi/wallets.json';
import viTransfers from '../locales/vi/transfers.json';
import viTransactions from '../locales/vi/transactions.json';
import viCustomers from '../locales/vi/customers.json';
import viApprovals from '../locales/vi/approvals.json';
import viActivity from '../locales/vi/activity.json';
import viSettings from '../locales/vi/settings.json';
import viHeader from '../locales/vi/header.json';
import viLanguage from '../locales/vi/language.json';

const resources = {
  en: {
    common: enCommon,
    nav: enNav,
    pageTitles: enPageTitles,
    auth: enAuth,
    dashboard: enDashboard,
    wallets: enWallets,
    transfers: enTransfers,
    transactions: enTransactions,
    customers: enCustomers,
    approvals: enApprovals,
    activity: enActivity,
    settings: enSettings,
    header: enHeader,
    language: enLanguage,
  },
  vi: {
    common: viCommon,
    nav: viNav,
    pageTitles: viPageTitles,
    auth: viAuth,
    dashboard: viDashboard,
    wallets: viWallets,
    transfers: viTransfers,
    transactions: viTransactions,
    customers: viCustomers,
    approvals: viApprovals,
    activity: viActivity,
    settings: viSettings,
    header: viHeader,
    language: viLanguage,
  },
};

const NS = [
  'common',
  'nav',
  'pageTitles',
  'auth',
  'dashboard',
  'wallets',
  'transfers',
  'transactions',
  'customers',
  'approvals',
  'activity',
  'settings',
  'header',
  'language',
];

const initialLang = readStoredLang();

i18n.use(initReactI18next).init({
  resources,
  lng: initialLang,
  fallbackLng: 'vi',
  defaultNS: 'common',
  ns: NS,
  interpolation: {
    escapeValue: false,
  },
});

document.documentElement.lang = initialLang;

i18n.on('languageChanged', (lng) => {
  writeStoredLang(lng);
  document.documentElement.lang = lng;
});

export default i18n;
