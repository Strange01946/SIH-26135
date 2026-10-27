package in.gov.sih.sih26135.response;

import in.gov.sih.sih26135.exception.BadRequestException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Utility for parsing, validating, and building Spring Data {@link Pageable} instances.
 * Enforces pagination limits, parameter bounds, sorting direction, and sortable field allowlists.
 */
public final class PaginationUtils {

  public static final int DEFAULT_PAGE = 0;
  public static final int DEFAULT_SIZE = 20;
  public static final int MIN_PAGE = 0;
  public static final int MIN_SIZE = 1;
  public static final int MAX_SIZE = 100;
  public static final String DEFAULT_DIRECTION = "ASC";

  // Pre-configured sort field mappings for the 7 core domain entities
  public static final Map<String, String> TRAINEE_SORT_FIELDS = Map.of(
      "id", "id",
      "registrationnumber", "registrationNumber",
      "firstname", "firstName",
      "lastname", "lastName",
      "dateofbirth", "dateOfBirth",
      "email", "email",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> EMPLOYER_SORT_FIELDS = Map.of(
      "id", "id",
      "code", "employerCode",
      "employercode", "employerCode",
      "name", "employerName",
      "employername", "employerName",
      "registrationnumber", "registrationNumber",
      "gstin", "gstin",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> COURSE_SORT_FIELDS = Map.of(
      "id", "id",
      "code", "courseCode",
      "coursecode", "courseCode",
      "name", "courseName",
      "coursename", "courseName",
      "durationhours", "durationHours",
      "durationdays", "durationDays",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> TRAINING_CENTER_SORT_FIELDS = Map.of(
      "id", "id",
      "code", "centerCode",
      "centercode", "centerCode",
      "name", "centerName",
      "centername", "centerName",
      "operationalcapacity", "operationalCapacity",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> TRAINING_PROVIDER_SORT_FIELDS = Map.of(
      "id", "id",
      "code", "providerCode",
      "providercode", "providerCode",
      "name", "providerName",
      "providername", "providerName",
      "registrationnumber", "registrationNumber",
      "rating", "rating",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> TRAINING_BATCH_SORT_FIELDS = Map.of(
      "id", "id",
      "code", "batchCode",
      "batchcode", "batchCode",
      "startdate", "startDate",
      "enddate", "endDate",
      "capacity", "capacity",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  public static final Map<String, String> TRAINING_ENROLLMENT_SORT_FIELDS = Map.of(
      "id", "id",
      "enrollmentnumber", "enrollmentNumber",
      "enrollmentdate", "enrollmentDate",
      "startdate", "startDate",
      "expectedcompletiondate", "expectedCompletionDate",
      "actualcompletiondate", "actualCompletionDate",
      "createdat", "createdAt",
      "updatedat", "updatedAt"
  );

  private PaginationUtils() {
    // Utility class
  }

  /**
   * Validates pagination parameters and builds a Pageable instance with stable sorting.
   *
   * @param page zero-based page index
   * @param size number of records per page
   * @param sort field to sort on (optional, uses default if null/blank)
   * @param direction sorting direction ("ASC" or "DESC", case-insensitive)
   * @param allowedSortFields map of allowed lowercase sort query keys to entity property names
   * @param defaultSortProperty default property to sort by when sort parameter is omitted
   * @return validated {@link Pageable}
   * @throws BadRequestException if any parameter is out of bounds or unsupported
   */
  public static Pageable buildPageable(
      int page,
      int size,
      String sort,
      String direction,
      Map<String, String> allowedSortFields,
      String defaultSortProperty) {

    if (page < MIN_PAGE) {
      throw new BadRequestException("Page index must not be less than zero", "INVALID_PAGE");
    }

    if (size < MIN_SIZE) {
      throw new BadRequestException("Page size must be at least 1", "INVALID_PAGE_SIZE");
    }

    if (size > MAX_SIZE) {
      throw new BadRequestException("Page size must not exceed " + MAX_SIZE, "INVALID_PAGE_SIZE");
    }

    Sort.Direction sortDirection = parseDirection(direction);

    String resolvedProperty = defaultSortProperty;
    if (sort != null && !sort.isBlank()) {
      String normalizedKey = sort.trim().toLowerCase();
      if (!allowedSortFields.containsKey(normalizedKey)) {
        throw new BadRequestException(
            "Unsupported sort field '" + sort + "'. Supported fields: " + getCleanAllowedFields(allowedSortFields),
            "INVALID_SORT_FIELD"
        );
      }
      resolvedProperty = allowedSortFields.get(normalizedKey);
    }

    Sort primarySort = Sort.by(sortDirection, resolvedProperty);
    // Add stable tie-breaker on primary key "id" if not already sorting by "id"
    Sort finalSort = "id".equals(resolvedProperty)
        ? primarySort
        : primarySort.and(Sort.by(Sort.Direction.ASC, "id"));

    return PageRequest.of(page, size, finalSort);
  }

  private static Sort.Direction parseDirection(String direction) {
    if (direction == null || direction.isBlank()) {
      return Sort.Direction.ASC;
    }
    String normalized = direction.trim().toUpperCase();
    if ("ASC".equals(normalized)) {
      return Sort.Direction.ASC;
    }
    if ("DESC".equals(normalized)) {
      return Sort.Direction.DESC;
    }
    throw new BadRequestException(
        "Invalid sort direction '" + direction + "'. Must be ASC or DESC",
        "INVALID_SORT_DIRECTION"
    );
  }

  private static Set<String> getCleanAllowedFields(Map<String, String> allowedSortFields) {
    return Collections.unmodifiableSet(allowedSortFields.keySet());
  }
}
