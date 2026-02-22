import type { ConundrumEntry } from "../logic/gameRules";

let dictionaryCache: Set<string> | null = null;
let conundrumCache: ConundrumEntry[] | null = null;

const ALLOWED_SINGLE_LETTER_WORDS = new Set(["a", "i"]);
const EXCLUDED_NAME_WORDS = new Set([
  "aaron", "abigail", "adam", "adrian", "ahmed", "albert", "alec", "alexa", "alexander", "alfred",
  "ali", "alice", "alison", "amanda", "amber", "amelia", "amy", "andrea", "andrew", "angela",
  "anna", "anthony", "arthur", "ashley", "audrey", "austin", "barbara", "benjamin", "bethany", "betty",
  "beverly", "blake", "bradley", "brandon", "brian", "brianna", "brittany", "caleb", "cameron", "carla",
  "carmen", "carol", "caroline", "carter", "catherine", "charles", "charlotte", "cheryl", "chloe", "christian",
  "christina", "christine", "christopher", "cindy", "claire", "cole", "colin", "connor", "courtney", "crystal",
  "daniel", "danielle", "david", "deborah", "denise", "dennis", "derek", "diana", "donald", "donna",
  "dylan", "edward", "elijah", "elizabeth", "ella", "emily", "emma", "eric", "ethan", "eugene",
  "evelyn", "frances", "frank", "gabriel", "gary", "george", "gerald", "gloria", "grace", "gregory",
  "hannah", "harold", "heather", "helen", "henry", "isabella", "jacob", "jacqueline", "james", "jamie",
  "janet", "janice", "jason", "jean", "jeffrey", "jenna", "jennifer", "jeremy", "jerry", "jesse",
  "jessica", "joan", "joe", "john", "johnny", "jonathan", "jordan", "joseph", "joshua", "joyce",
  "judith", "judy", "julia", "julie", "justin", "karen", "katherine", "kathleen", "kathryn", "kayla",
  "keith", "kelly", "kenneth", "kevin", "kimberly", "kyle", "larry", "laura", "lauren", "linda",
  "lisa", "logan", "lori", "madison", "maria", "marie", "marilyn", "markus", "martha", "mary",
  "mason", "matthew", "megan", "melanie", "melissa", "michael", "michelle", "morgan", "nancy", "natalie",
  "nathan", "nicholas", "nicole", "noah", "olivia", "pamela", "patricia", "patrick", "paul", "peter",
  "philip", "rachel", "ralph", "raymond", "rebecca", "richard", "robert", "roger", "ronald", "rosemary",
  "russell", "ruth", "ryan", "samantha", "samuel", "sandra", "sara", "sarah", "scott", "sean",
  "sharon", "shirley", "sophia", "stephanie", "stephen", "steven", "susan", "tammy", "taylor", "teresa",
  "terry", "theresa", "thomas", "timothy", "tyler", "victoria", "vincent", "virginia", "walter", "wayne",
  "william"
]);

const isAllowedDictionaryEntry = (line: string): boolean => {
  if (!line) return false;
  if (!/^[a-z]+$/.test(line)) return false;
  if (line.length === 1) return ALLOWED_SINGLE_LETTER_WORDS.has(line);
  return !EXCLUDED_NAME_WORDS.has(line);
};

export const loadDictionary = async (): Promise<Set<string>> => {
  if (dictionaryCache) return dictionaryCache;

  const response = await fetch("/data/dictionary_common_10k.txt");
  if (!response.ok) {
    throw new Error("Failed to load dictionary dataset.");
  }

  const text = await response.text();
  dictionaryCache = new Set(
    text
      .split(/\r?\n/)
      .map((line) => line.trim().toLowerCase())
      .filter(isAllowedDictionaryEntry)
  );

  return dictionaryCache;
};

export const loadConundrums = async (): Promise<ConundrumEntry[]> => {
  if (conundrumCache) return conundrumCache;

  const response = await fetch("/data/conundrums.json");
  if (!response.ok) {
    throw new Error("Failed to load conundrum dataset.");
  }

  const parsed = (await response.json()) as ConundrumEntry[];
  conundrumCache = parsed;
  return conundrumCache;
};
