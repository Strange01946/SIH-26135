package in.gov.sih.sih26135.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import org.springframework.data.domain.Page;

/**
 * Standardized pagination wrapper for API responses.
 * Enveloped within {@link ApiResponse} as {@code ApiResponse<PagedResponse<T>>}.
 *
 * @param <T> data transfer object type of the page content
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagedResponse<T> {

  private final List<T> content;
  private final int page;
  private final int size;
  private final long totalElements;
  private final int totalPages;
  private final boolean first;
  private final boolean last;
  private final boolean hasNext;
  private final boolean hasPrevious;

  @JsonCreator
  public PagedResponse(
      @JsonProperty("content") List<T> content,
      @JsonProperty("page") int page,
      @JsonProperty("size") int size,
      @JsonProperty("totalElements") long totalElements,
      @JsonProperty("totalPages") int totalPages,
      @JsonProperty("first") boolean first,
      @JsonProperty("last") boolean last,
      @JsonProperty("hasNext") boolean hasNext,
      @JsonProperty("hasPrevious") boolean hasPrevious) {
    this.content = content != null ? Collections.unmodifiableList(content) : Collections.emptyList();
    this.page = page;
    this.size = size;
    this.totalElements = totalElements;
    this.totalPages = totalPages;
    this.first = first;
    this.last = last;
    this.hasNext = hasNext;
    this.hasPrevious = hasPrevious;
  }

  /**
   * Constructs a PagedResponse from a Spring Data {@link Page}.
   *
   * @param springPage the Spring Data Page
   * @param <T> content type
   * @return populated PagedResponse
   */
  public static <T> PagedResponse<T> of(Page<T> springPage) {
    if (springPage == null) {
      return empty();
    }
    return new PagedResponse<>(
        springPage.getContent(),
        springPage.getNumber(),
        springPage.getSize(),
        springPage.getTotalElements(),
        springPage.getTotalPages(),
        springPage.isFirst(),
        springPage.isLast(),
        springPage.hasNext(),
        springPage.hasPrevious()
    );
  }

  /**
   * Constructs a PagedResponse from a Spring Data {@link Page} of entities and a mapper function.
   *
   * @param springPage the entity Page
   * @param mapper function to map entity to response DTO
   * @param <E> entity type
   * @param <T> DTO response type
   * @return populated PagedResponse
   */
  public static <E, T> PagedResponse<T> from(Page<E> springPage, Function<E, T> mapper) {
    if (springPage == null) {
      return empty();
    }
    List<T> mapped = springPage.getContent().stream()
        .map(mapper)
        .toList();
    return new PagedResponse<>(
        mapped,
        springPage.getNumber(),
        springPage.getSize(),
        springPage.getTotalElements(),
        springPage.getTotalPages(),
        springPage.isFirst(),
        springPage.isLast(),
        springPage.hasNext(),
        springPage.hasPrevious()
    );
  }

  /**
   * Returns an empty PagedResponse.
   *
   * @param <T> content type
   * @return empty PagedResponse
   */
  public static <T> PagedResponse<T> empty() {
    return new PagedResponse<>(Collections.emptyList(), 0, 0, 0L, 0, true, true, false, false);
  }

  @JsonProperty("content")
  public List<T> getContent() {
    return content;
  }

  @JsonProperty("page")
  public int getPage() {
    return page;
  }

  @JsonProperty("size")
  public int getSize() {
    return size;
  }

  @JsonProperty("totalElements")
  public long getTotalElements() {
    return totalElements;
  }

  @JsonProperty("totalPages")
  public int getTotalPages() {
    return totalPages;
  }

  @JsonProperty("first")
  public boolean isFirst() {
    return first;
  }

  @JsonProperty("last")
  public boolean isLast() {
    return last;
  }

  @JsonProperty("hasNext")
  public boolean isHasNext() {
    return hasNext;
  }

  @JsonProperty("hasPrevious")
  public boolean isHasPrevious() {
    return hasPrevious;
  }
}
