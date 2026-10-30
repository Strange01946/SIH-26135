package in.gov.sih.sih26135.response;

import in.gov.sih.sih26135.exception.BadRequestException;

/**
 * Utility for sanitizing, validating, and escaping search terms and filter parameters.
 * Protects against SQL wildcard abuse ({@code %}, {@code _}), ReDoS risks, and computational denial of service.
 */
public final class SearchSanitizationUtils {

  public static final int MAX_SEARCH_TERM_LENGTH = 100;
  public static final String ESCAPE_CHAR = "\\";

  private SearchSanitizationUtils() {
    // Utility class
  }

  /**
   * Escapes SQL wildcard characters ({@code %}, {@code _}) and escape characters ({@code \})
   * for safe usage in parameterized SQL LIKE queries.
   *
   * @param term the input search term
   * @return escaped search term, or null if input was null
   */
  public static String escapeSqlWildcards(String term) {
    if (term == null) {
      return null;
    }
    // Must escape the backslash first so subsequent escape prefixes are not re-escaped
    return term.replace("\\", "\\\\")
        .replace("%", "\\%")
        .replace("_", "\\_");
  }

  /**
   * Validates search term length and escapes SQL wildcard characters.
   *
   * @param term the input search term
   * @return escaped search term, or null if input is null or blank
   * @throws BadRequestException if term exceeds {@link #MAX_SEARCH_TERM_LENGTH} characters
   */
  public static String validateAndEscapeSearchTerm(String term) {
    if (term == null || term.isBlank()) {
      return null;
    }
    String trimmed = term.trim();
    if (trimmed.length() > MAX_SEARCH_TERM_LENGTH) {
      throw new BadRequestException(
          "Search term must not exceed " + MAX_SEARCH_TERM_LENGTH + " characters",
          "INVALID_SEARCH_TERM"
      );
    }
    return escapeSqlWildcards(trimmed);
  }

  /**
   * Builds a safe SQL LIKE pattern matching entries that contain the given term ({@code %term%}).
   *
   * @param term the search term
   * @return formatted pattern string, or null if term is null or blank
   * @throws BadRequestException if term exceeds {@link #MAX_SEARCH_TERM_LENGTH} characters
   */
  public static String toContainsPattern(String term) {
    String escaped = validateAndEscapeSearchTerm(term);
    return escaped != null ? "%" + escaped + "%" : null;
  }

  /**
   * Builds a safe SQL LIKE pattern matching entries that start with the given term ({@code term%}).
   *
   * @param term the search term
   * @return formatted pattern string, or null if term is null or blank
   * @throws BadRequestException if term exceeds {@link #MAX_SEARCH_TERM_LENGTH} characters
   */
  public static String toStartsWithPattern(String term) {
    String escaped = validateAndEscapeSearchTerm(term);
    return escaped != null ? escaped + "%" : null;
  }

  /**
   * Builds a safe SQL LIKE pattern matching entries that end with the given term ({@code %term}).
   *
   * @param term the search term
   * @return formatted pattern string, or null if term is null or blank
   * @throws BadRequestException if term exceeds {@link #MAX_SEARCH_TERM_LENGTH} characters
   */
  public static String toEndsWithPattern(String term) {
    String escaped = validateAndEscapeSearchTerm(term);
    return escaped != null ? "%" + escaped : null;
  }

  /**
   * Checks whether the given string contains unescaped SQL wildcard characters ({@code %}, {@code _})
   * or escape characters ({@code \}).
   *
   * @param term the string to check
   * @return true if wildcards or escape characters are present, false otherwise
   */
  public static boolean containsSqlWildcards(String term) {
    if (term == null) {
      return false;
    }
    return term.indexOf('%') >= 0 || term.indexOf('_') >= 0 || term.indexOf('\\') >= 0;
  }

  /**
   * Validates whether a search term is safe to use as a literal without modification.
   *
   * @param term the search term
   * @return true if non-null, within length bounds, and free of wildcards
   */
  public static boolean isSafeLiteralSearchTerm(String term) {
    if (term == null) {
      return false;
    }
    String trimmed = term.trim();
    return !trimmed.isEmpty() && trimmed.length() <= MAX_SEARCH_TERM_LENGTH && !containsSqlWildcards(trimmed);
  }
}
