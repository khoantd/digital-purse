/**
 * One-shot helper to emit en/vi namespace JSON files.
 * Run: node scripts/build-locales.mjs
 */
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __dirname = path.dirname(fileURLToPath(import.meta.url));
const outRoot = path.join(__dirname, '../src/locales');

function nest(flat) {
  const root = {};
  for (const [key, value] of Object.entries(flat)) {
    if (key.startsWith('_')) continue;
    const parts = key.split('.');
    let cur = root;
    for (let i = 0; i < parts.length - 1; i += 1) {
      cur[parts[i]] = cur[parts[i]] || {};
      cur = cur[parts[i]];
    }
    cur[parts[parts.length - 1]] = value;
  }
  return root;
}

function writeNs(lang, ns, flat) {
  const dir = path.join(outRoot, lang);
  fs.mkdirSync(dir, { recursive: true });
  fs.writeFileSync(path.join(dir, `${ns}.json`), `${JSON.stringify(nest(flat), null, 2)}\n`);
}

const en = JSON.parse(fs.readFileSync(path.join(__dirname, 'i18n-en-source.json'), 'utf8'));
const vi = JSON.parse(fs.readFileSync(path.join(__dirname, 'i18n-vi-source.json'), 'utf8'));

for (const [ns, flat] of Object.entries(en)) {
  if (ns.startsWith('_')) continue;
  writeNs('en', ns, flat);
}
for (const [ns, flat] of Object.entries(vi)) {
  if (ns.startsWith('_')) continue;
  writeNs('vi', ns, flat);
}

console.log('Wrote locales to', outRoot);
