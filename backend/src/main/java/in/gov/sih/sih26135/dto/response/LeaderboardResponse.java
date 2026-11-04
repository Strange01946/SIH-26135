package in.gov.sih.sih26135.dto.response;

import java.util.List;

/**
 * Reusable generic response model for dashboard leaderboard and ranking collections.
 * Supports strongly typed items without raw types or unchecked casts.
 *
 * @param <T> item type of the leaderboard entries
 */
public record LeaderboardResponse<T>(
    String rankingMetric,
    long totalItems,
    List<T> items
) {

  /**
   * Compact constructor enforcing non-null immutable item list.
   */
  public LeaderboardResponse {
    items = items != null ? List.copyOf(items) : List.of();
  }

  /**
   * Factory method deriving {@code totalItems} directly from the item collection.
   *
   * @param rankingMetric description of the ranking metric or criterion
   * @param items list of ranked items
   * @param <T> item type
   * @return initialized LeaderboardResponse instance
   */
  public static <T> LeaderboardResponse<T> of(String rankingMetric, List<T> items) {
    long size = items != null ? items.size() : 0L;
    return new LeaderboardResponse<>(rankingMetric, size, items);
  }
}
