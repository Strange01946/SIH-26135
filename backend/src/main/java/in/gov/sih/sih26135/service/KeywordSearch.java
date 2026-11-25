package in.gov.sih.sih26135.service;

import java.text.Normalizer;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.regex.Pattern;

/** Relevance scoring for descriptive searches. Identifier lookups still use their own rules. */
public final class KeywordSearch {
  private static final Map<String, String> RELATED = Map.ofEntries(
      Map.entry("developer", "develop"), Map.entry("developers", "develop"), Map.entry("development", "develop"),
      Map.entry("devlopment", "develop"), Map.entry("devloper", "develop"),
      Map.entry("programmer", "program"), Map.entry("programmers", "program"), Map.entry("programming", "program"),
      Map.entry("installer", "install"), Map.entry("installation", "install"));
  private static final Set<String> CONNECTORS = Set.of("and", "for", "the", "in", "of", "with");
  private static final Pattern SEPARATORS = Pattern.compile("[^\\p{L}\\p{M}\\p{N}+#]+");
  private static final Pattern TYPABLE_WORD = Pattern.compile("[a-z]{4,40}");

  private final List<String> terms;

  public KeywordSearch(String input) {
    this.terms = words(input);
  }

  public boolean isEmpty() {
    return terms.isEmpty();
  }

  public int score(String text) {
    if (isEmpty()) return 1;
    return score(terms, words(text), true);
  }

  public boolean matchesSkill(String text) {
    return !isEmpty() && score(catalogWords(terms, false), words(text), false) > 0;
  }

  private static int score(List<String> terms, List<String> words, boolean partial) {
    int score = 0;
    for (String term : terms) {
      int best = words.stream().mapToInt(word -> wordScore(term, word, partial)).max().orElse(0);
      if (best == 0) return 0;
      score += best;
    }
    return score;
  }

  public <T> List<T> rank(List<T> rows, Function<T, String> text) {
    if (isEmpty()) return rows;
    return rows.stream().map(row -> new Match<>(row, score(text.apply(row))))
        .filter(match -> match.score() > 0)
        .sorted(Comparator.comparingInt((Match<T> match) -> match.score()).reversed())
        .map(Match::row).toList();
  }

  public <T> T resolve(List<T> options, Function<T, String> name, boolean allowRoleQualifier) {
    if (isEmpty()) return null;
    var input = catalogWords(terms, allowRoleQualifier);
    return uniqueBest(options, option -> {
      var candidate = catalogWords(words(name.apply(option)), allowRoleQualifier);
      if (candidate.size() != input.size()) {
        // A shorter job title may omit a specialization. Require every word and a unique best role.
        if (!allowRoleQualifier || input.size() < 2 || candidate.size() <= input.size()) return 0;
        return input.stream().allMatch(term -> candidate.stream().anyMatch(word -> wordScore(term, word, false) >= 75))
            ? 1 : 0;
      }
      int score = 0;
      for (int i = 0; i < input.size(); i++) {
        int similarity = wordScore(input.get(i), candidate.get(i), false);
        if (similarity == 0) return 0;
        score += similarity;
      }
      return score;
    });
  }

  private static List<String> catalogWords(List<String> words, boolean role) {
    if (!role && words.contains("java") && words.stream().allMatch(Set.of("java", "programming", "language")::contains)) {
      return List.of("java");
    }
    return words.stream().sorted().toList();
  }

  private static <T> T uniqueBest(List<T> options, ToIntFunction<T> scorer) {
    T best = null;
    int bestScore = 0;
    for (T option : options) {
      int score = scorer.applyAsInt(option);
      if (score > bestScore) {
        best = option;
        bestScore = score;
      } else if (score == bestScore) {
        best = null;
      }
    }
    return best;
  }

  private static List<String> words(String text) {
    if (text == null) return List.of();
    return Arrays.stream(SEPARATORS.split(Normalizer.normalize(text, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)))
        .filter(word -> !word.isBlank() && !CONNECTORS.contains(word)).distinct().toList();
  }

  private static int wordScore(String term, String word, boolean partial) {
    if (term.equals(word)) return 100;
    if (RELATED.getOrDefault(term, term).equals(RELATED.getOrDefault(word, word))) return 75;
    if (partial && term.length() >= 2 && word.startsWith(term)) return 60;
    if (partial && term.length() >= 3 && word.contains(term)) return 50;
    if (!TYPABLE_WORD.matcher(term).matches() || !TYPABLE_WORD.matcher(word).matches() || term.charAt(0) != word.charAt(0)) return 0;
    int limit = Math.max(term.length(), word.length()) >= 6 ? 2 : 1;
    if (Math.abs(term.length() - word.length()) > limit) return 0;
    int distance = distance(term, word);
    return distance <= limit ? 40 - distance * 5 : 0;
  }

  private static int distance(String left, String right) {
    int[][] edits = new int[left.length() + 1][right.length() + 1];
    for (int i = 0; i <= left.length(); i++) edits[i][0] = i;
    for (int j = 0; j <= right.length(); j++) edits[0][j] = j;
    for (int i = 1; i <= left.length(); i++) {
      for (int j = 1; j <= right.length(); j++) {
        int substitution = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
        edits[i][j] = Math.min(edits[i - 1][j - 1] + substitution, Math.min(edits[i - 1][j], edits[i][j - 1]) + 1);
        if (i > 1 && j > 1 && left.charAt(i - 1) == right.charAt(j - 2) && left.charAt(i - 2) == right.charAt(j - 1)) {
          edits[i][j] = Math.min(edits[i][j], edits[i - 2][j - 2] + 1);
        }
      }
    }
    return edits[left.length()][right.length()];
  }

  private record Match<T>(T row, int score) {}
}
