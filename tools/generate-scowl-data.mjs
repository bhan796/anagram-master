import fs from 'node:fs/promises';
import path from 'node:path';

const root = process.cwd();
const tmpDir = path.join(root, '.tmp_scowl');
const raw70Path = path.join(tmpDir, 'scowl_us_70_raw.txt');
const raw60Path = path.join(tmpDir, 'scowl_us_60_raw.txt');
const loaderPath = path.join(root, 'web-app', 'src', 'logic', 'loaders.ts');

const parseSet = (source, constName) => {
  const regex = new RegExp(`const ${constName} = new Set\\(\\[(.*?)\\]\\);`, 's');
  const match = source.match(regex);
  if (!match) {
    throw new Error(`Could not parse ${constName} from loaders.ts`);
  }
  return new Set(Array.from(match[1].matchAll(/"([a-z]+)"/g), (m) => m[1]));
};

const scrambleWord = (word) => {
  const chars = word.toUpperCase().split('');
  if (chars.length <= 1) return word.toUpperCase();
  let candidate = chars.join('');
  for (let attempt = 0; attempt < 12; attempt += 1) {
    const shuffled = [...chars];
    for (let i = shuffled.length - 1; i > 0; i -= 1) {
      const j = Math.floor(Math.random() * (i + 1));
      [shuffled[i], shuffled[j]] = [shuffled[j], shuffled[i]];
    }
    candidate = shuffled.join('');
    if (candidate !== word.toUpperCase()) return candidate;
  }
  return word.slice(1).toUpperCase() + word[0].toUpperCase();
};

const normalizeAndFilter = (raw, allowedSingleLetterWords, excludedNameWords) => {
  const out = new Set();
  for (const line of raw.split(/\r?\n/)) {
    const word = line.trim();
    if (!word) continue;
    if (!/^[a-z]+$/.test(word)) continue;
    if (word.length === 1) {
      if (!allowedSingleLetterWords.has(word)) continue;
    } else if (excludedNameWords.has(word)) {
      continue;
    }
    out.add(word);
  }
  return Array.from(out).sort((a, b) => a.localeCompare(b));
};

const writeTextTargets = async (relativeTargets, content) => {
  await Promise.all(
    relativeTargets.map(async (relativeTarget) => {
      const outPath = path.join(root, relativeTarget);
      await fs.mkdir(path.dirname(outPath), { recursive: true });
      await fs.writeFile(outPath, content, 'utf8');
    })
  );
};

const writeJsonTargets = async (relativeTargets, data) => {
  const json = `${JSON.stringify(data, null, 2)}\n`;
  await Promise.all(
    relativeTargets.map(async (relativeTarget) => {
      const outPath = path.join(root, relativeTarget);
      await fs.mkdir(path.dirname(outPath), { recursive: true });
      await fs.writeFile(outPath, json, 'utf8');
    })
  );
};

const main = async () => {
  const [raw70, raw60, loaderSource] = await Promise.all([
    fs.readFile(raw70Path, 'utf8'),
    fs.readFile(raw60Path, 'utf8'),
    fs.readFile(loaderPath, 'utf8')
  ]);

  const allowedSingleLetterWords = parseSet(loaderSource, 'ALLOWED_SINGLE_LETTER_WORDS');
  const excludedNameWords = parseSet(loaderSource, 'EXCLUDED_NAME_WORDS');

  const dictionaryWords70 = normalizeAndFilter(raw70, allowedSingleLetterWords, excludedNameWords);
  const sourceWords60 = normalizeAndFilter(raw60, allowedSingleLetterWords, excludedNameWords);

  const conundrumWords = sourceWords60.filter((word) => word.length === 9);
  const conundrums = conundrumWords.map((answer, index) => ({
    id: String(index + 1),
    scrambled: scrambleWord(answer),
    answer
  }));

  const dictionaryContent = `${dictionaryWords70.join('\n')}\n`;

  await writeTextTargets(
    [
      'shared/data/dictionary_common_10k.txt',
      'server/data/dictionary_common_10k.txt',
      'web-app/public/data/dictionary_common_10k.txt',
      'android-app/app/src/main/assets/data/dictionary_common_10k.txt',
      'ios-app/AnagramArena/Resources/Data/dictionary_common_10k.txt'
    ],
    dictionaryContent
  );

  await writeJsonTargets(
    [
      'shared/data/conundrums.json',
      'server/data/conundrums.json',
      'web-app/public/data/conundrums.json',
      'android-app/app/src/main/assets/data/conundrums.json',
      'ios-app/AnagramArena/Resources/Data/conundrums.json'
    ],
    conundrums
  );

  console.log(`SCOWL size-70 dictionary words written: ${dictionaryWords70.length}`);
  console.log(`SCOWL size-60 conundrum candidates used: ${conundrumWords.length}`);
};

main().catch((error) => {
  console.error(error);
  process.exit(1);
});
