const related = new Map(Object.entries({
  developer: 'develop', developers: 'develop', development: 'develop',
  devlopment: 'develop', devloper: 'develop',
  programmer: 'program', programmers: 'program', programming: 'program',
  installer: 'install', installation: 'install',
}));
const connectors = new Set(['and', 'for', 'the', 'in', 'of', 'with']);
const words = (text: string) => [...new Set(text.normalize('NFKC').toLowerCase().split(/[^\p{L}\p{M}\p{N}+#]+/u)
  .filter(word => word && !connectors.has(word)))];

function distance(left: string, right: string) {
  let previous = Array.from({ length: right.length + 1 }, (_, index) => index);
  let beforePrevious = previous;
  for (let i = 1; i <= left.length; i++) {
    const row = [i];
    for (let j = 1; j <= right.length; j++) {
      const substitution = left[i - 1] === right[j - 1] ? 0 : 1;
      row[j] = Math.min(previous[j - 1] + substitution, previous[j] + 1, row[j - 1] + 1);
      if (i > 1 && j > 1 && left[i - 1] === right[j - 2] && left[i - 2] === right[j - 1]) {
        row[j] = Math.min(row[j], beforePrevious[j - 2] + 1);
      }
    }
    beforePrevious = previous;
    previous = row;
  }
  return previous[right.length];
}

function wordScore(term: string, word: string) {
  if (term === word) return 100;
  if ((related.get(term) ?? term) === (related.get(word) ?? word)) return 75;
  if (term.length >= 2 && word.startsWith(term)) return 60;
  if (term.length >= 3 && word.includes(term)) return 50;
  if (!/^[a-z]{4,40}$/.test(term) || !/^[a-z]{4,40}$/.test(word) || term[0] !== word[0]) return 0;
  const limit = Math.max(term.length, word.length) >= 6 ? 2 : 1;
  if (Math.abs(term.length - word.length) > limit) return 0;
  const edits = distance(term, word);
  return edits <= limit ? 40 - edits * 5 : 0;
}

export function matchKeywords<T>(rows: T[], search: string, text: (row: T) => string): T[] {
  const terms = words(search);
  if (!terms.length) return rows;
  return rows.map(row => {
    const subject = words(text(row));
    let score = 0;
    for (const term of terms) {
      const best = subject.reduce((highest, word) => Math.max(highest, wordScore(term, word)), 0);
      if (!best) return { row, score: 0 };
      score += best;
    }
    return { row, score };
  }).filter(match => match.score > 0).sort((left, right) => right.score - left.score).map(match => match.row);
}
