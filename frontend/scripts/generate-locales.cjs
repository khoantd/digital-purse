/**
 * Writes nested namespace JSON under src/locales/{en,vi}/.
 * Run: node scripts/generate-locales.cjs
 */
const fs = require('fs');
const path = require('path');

const { en, vi } = require('./i18n-locale-data.cjs');

const outRoot = path.join(__dirname, '../src/locales');

function writeLang(lang, bundles) {
  const dir = path.join(outRoot, lang);
  fs.mkdirSync(dir, { recursive: true });
  for (const [ns, content] of Object.entries(bundles)) {
    fs.writeFileSync(path.join(dir, `${ns}.json`), `${JSON.stringify(content, null, 2)}\n`);
  }
}

writeLang('en', en);
writeLang('vi', vi);
console.log('Wrote locales to', outRoot);
