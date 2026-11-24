package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.venue.TrainingRequirementDto.*;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.service.ActiveGeography;
import in.gov.sih.sih26135.service.AuditLogService;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class TrainingRequirementService {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
  private static final String REQUIREMENT_SELECT = """
      SELECT r.*, s.state_name, d.district_name, p.provider_name, c.center_name, c.address_line1,
        b.booking_id, b.booking_status
      FROM training_requirements r
      JOIN states s ON s.state_id=r.state_id JOIN districts d ON d.district_id=r.district_id
      LEFT JOIN training_centers c ON c.center_id=r.center_id
      LEFT JOIN training_providers p ON p.provider_id=r.selected_provider_id
      LEFT JOIN venue_bookings b ON b.booking_id=(
        SELECT MAX(bp.booking_id) FROM venue_booking_training_plans bp
        WHERE bp.requirement_id=r.requirement_id AND r.root_booking_id IS NOT NULL
          AND (bp.booking_id=r.root_booking_id OR EXISTS (
            SELECT 1 FROM venue_booking_sessions vs WHERE vs.booking_id=bp.booking_id AND vs.reserved_at IS NOT NULL)))
      """;
  private static final String APPLICATION_SELECT = """
      SELECT a.*, r.title, p.provider_name
      FROM training_requirement_applications a JOIN training_requirements r USING (requirement_id)
      JOIN training_providers p ON p.provider_id=a.provider_id
      """;
  private static final RowMapper<Requirement> REQUIREMENT_ROW = (rs, row) -> new Requirement(
      rs.getLong("requirement_id"), rs.getString("title"), rs.getString("skills_to_teach"), rs.getString("description"),
      rs.getLong("state_id"), rs.getString("state_name"), rs.getLong("district_id"), rs.getString("district_name"), rs.getInt("seats"),
      nullableLong(rs, "center_id"), rs.getString("center_name"), rs.getString("address_line1"),
      rs.getObject("earliest_date", LocalDate.class), rs.getObject("latest_date", LocalDate.class),
      rs.getObject("daily_start_time", LocalTime.class), rs.getObject("daily_end_time", LocalTime.class),
      rs.getString("requirement_status"), accepting(rs), nullableLong(rs, "selected_provider_id"), rs.getString("provider_name"),
      rs.getObject("published_at", LocalDateTime.class), rs.getObject("selected_at", LocalDateTime.class),
      nullableLong(rs, "booking_id"), rs.getString("booking_status"));
  private static final RowMapper<Application> APPLICATION_ROW = (rs, row) -> new Application(
      rs.getLong("application_id"), rs.getLong("requirement_id"), rs.getString("title"),
      rs.getLong("provider_id"), rs.getString("provider_name"), rs.getString("application_status"),
      rs.getObject("start_date", LocalDate.class), rs.getObject("end_date", LocalDate.class),
      rs.getObject("daily_start_time", LocalTime.class), rs.getObject("daily_end_time", LocalTime.class),
      rs.getString("delivery_notes"), rs.getObject("submitted_at", LocalDateTime.class));

  private record LockedRequirement(long state, long district, String status, Long centerId, Long bookingId) {}

  private final JdbcTemplate jdbc;
  private final TrainingWorkflowAccess access;
  private final AuditLogService audits;
  private final VenueBookingService venues;

  public TrainingRequirementService(JdbcTemplate jdbc, TrainingWorkflowAccess access, AuditLogService audits,
      VenueBookingService venues) {
    this.jdbc = jdbc;
    this.access = access;
    this.audits = audits;
    this.venues = venues;
  }

  public List<Requirement> governmentList(int page, int size) {
    var scope = access.reviewerScope();
    StringBuilder sql = new StringBuilder(REQUIREMENT_SELECT).append(" WHERE 1=1");
    List<Object> args = new ArrayList<>();
    if (scope.stateId() != null) {
      sql.append(" AND r.state_id=?");
      args.add(scope.stateId());
    }
    if (scope.districtId() != null) {
      sql.append(" AND r.district_id=?");
      args.add(scope.districtId());
    }
    sql.append(" ORDER BY r.requirement_id DESC LIMIT ? OFFSET ?");
    args.add(size);
    args.add((long) page * size);
    return jdbc.query(sql.toString(), REQUIREMENT_ROW, args.toArray());
  }

  public RequirementDetails governmentDetails(long id) {
    var scope = access.reviewerScope();
    Requirement requirement = find(id);
    requireLocation(scope, requirement.stateId(), requirement.districtId());
    return new RequirementDetails(requirement, jdbc.query(APPLICATION_SELECT
        + " WHERE a.requirement_id=? ORDER BY a.application_id", APPLICATION_ROW, id), null);
  }

  @Transactional
  public Requirement publish(Publish request) {
    var scope = access.reviewerScope();
    requireLocation(scope, request.stateId(), request.districtId());
    ActiveGeography.validate(jdbc, request.stateId(), request.districtId());
    validateDates(request.earliestDate(), request.latestDate(), request.dailyStartTime(), request.dailyEndTime());
    venues.lockGovernmentCentre(request.centerId(), request.stateId(), request.districtId(), request.seats());
    long id = insert("""
        INSERT INTO training_requirements(title,skills_to_teach,description,state_id,district_id,center_id,
          seats,earliest_date,latest_date,daily_start_time,daily_end_time,published_by)
        VALUES (?,?,?,?,?,?,?,?,?,?,?,?)
        """, request.title().trim(), request.skillsToTeach().trim(), text(request.description()),
        request.stateId(), request.districtId(), request.centerId(), request.seats(), request.earliestDate(),
        request.latestDate(), request.dailyStartTime(), request.dailyEndTime(), access.actor().getId());
    audits.recordStateChange("CREATE", "TrainingRequirement", id, "Training requirement published", null,
        Map.of("status", "OPEN", "centerId", request.centerId()), List.of());
    return find(id);
  }

  public List<Requirement> openRequirements(int page, int size, Long state, Long district) {
    long providerId = access.providerScope().trainingProviderId();
    if (!access.eligibleProvider(providerId)) return List.of();
    return jdbc.query(REQUIREMENT_SELECT + """
        WHERE r.requirement_status='OPEN' AND r.center_id IS NOT NULL AND r.latest_date>=?
          AND (? IS NULL OR r.state_id=?) AND (? IS NULL OR r.district_id=?)
        ORDER BY r.earliest_date,r.requirement_id DESC LIMIT ? OFFSET ?
        """, REQUIREMENT_ROW, today(), state, state, district, district, size, (long) page * size);
  }

  public List<Application> myApplications(int page, int size) {
    return jdbc.query(APPLICATION_SELECT + " WHERE a.provider_id=? ORDER BY a.application_id DESC LIMIT ? OFFSET ?",
        APPLICATION_ROW, access.providerScope().trainingProviderId(), size, (long) page * size);
  }

  public RequirementDetails providerDetails(long id) {
    long providerId = access.providerScope().trainingProviderId();
    Requirement requirement = jdbc.query(REQUIREMENT_SELECT + """
        WHERE r.requirement_id=? AND ((r.requirement_status='OPEN' AND r.center_id IS NOT NULL AND r.latest_date>=?)
          OR r.selected_provider_id=?)
        """, REQUIREMENT_ROW, id, today(), providerId).stream().findFirst().orElseThrow(TrainingRequirementService::missing);
    if (requirement.acceptingApplications() && !access.eligibleProvider(providerId)) throw missing();
    return new RequirementDetails(requirement, List.of(), myApplication(id, providerId));
  }

  @Transactional
  public Application apply(long id, Apply request) {
    long providerId = access.providerScope().trainingProviderId();
    LockedRequirement locked = lock(id);
    if (!"OPEN".equals(locked.status())) throw missing();
    Requirement requirement = find(id);
    if (!requirement.acceptingApplications()) throw missing();
    requireEligibleProvider(providerId);
    validateDates(request.startDate(), request.endDate(), request.dailyStartTime(), request.dailyEndTime());
    validateProposal(request.startDate(), request.endDate(), request.dailyStartTime());
    if (request.startDate().isBefore(requirement.earliestDate()) || request.endDate().isAfter(requirement.latestDate())
        || request.dailyStartTime().isBefore(requirement.dailyStartTime()) || request.dailyEndTime().isAfter(requirement.dailyEndTime())) {
      throw new BadRequestException("Propose dates and times within the published training window.", "TRAINING_OFFER_OUTSIDE_WINDOW");
    }
    long applicationId;
    try {
      applicationId = insert("""
          INSERT INTO training_requirement_applications(requirement_id,provider_id,start_date,end_date,
            daily_start_time,daily_end_time,delivery_notes,submitted_by) VALUES (?,?,?,?,?,?,?,?)
          """, id, providerId, request.startDate(), request.endDate(), request.dailyStartTime(), request.dailyEndTime(),
          text(request.deliveryNotes()), access.actor().getId());
    } catch (DuplicateKeyException ex) {
      throw new ConflictException("Your organization has already applied.", "TRAINING_APPLICATION_EXISTS");
    }
    audits.recordStateChange("CREATE", "TrainingRequirementApplication", applicationId, "Provider applied",
        null, Map.of("status", "APPLIED", "requirementId", id), List.of());
    return myApplication(id, providerId);
  }

  @Transactional
  public Requirement select(long id, long applicationId) {
    var scope = access.reviewerScope();
    LockedRequirement locked = lock(id);
    requireLocation(scope, locked.state(), locked.district());
    if (!"OPEN".equals(locked.status())) {
      throw new ConflictException("A provider has already been selected.", "TRAINING_REQUIREMENT_AWARDED");
    }
    Requirement requirement = find(id);
    if (locked.centerId() == null) {
      throw new ConflictException("Assign a training center before selecting a provider.", "TRAINING_CENTRE_REQUIRED");
    }
    Application chosen = jdbc.query(APPLICATION_SELECT + " WHERE a.requirement_id=? AND a.application_id=?",
        APPLICATION_ROW, id, applicationId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("TrainingRequirementApplication", "id"));
    long bookingId = reserve(requirement, chosen);
    jdbc.update("""
        UPDATE training_requirement_applications
        SET application_status=CASE WHEN application_id=? THEN 'SELECTED' ELSE 'NOT_SELECTED' END
        WHERE requirement_id=? AND application_status='APPLIED'
        """, applicationId, id);
    jdbc.update("""
        UPDATE training_requirements SET requirement_status='AWARDED',selected_provider_id=?,selected_by=?,selected_at=?,root_booking_id=?
        WHERE requirement_id=?
        """, chosen.providerId(), access.actor().getId(), LocalDateTime.now(INDIA).withNano(0), bookingId, id);
    audits.recordStateChange("STATUS_CHANGE", "TrainingRequirement", id, "Provider selected",
        Map.of("status", "OPEN"), Map.of("status", "AWARDED", "providerId", chosen.providerId(), "bookingId", bookingId), List.of());
    return find(id);
  }

  @Transactional
  public Requirement setCentre(long id, long centerId) {
    var scope = access.reviewerScope();
    LockedRequirement locked = lock(id);
    requireLocation(scope, locked.state(), locked.district());
    if (locked.centerId() != null || locked.bookingId() != null) {
      throw new ConflictException("This requirement already has a training center or booking.", "TRAINING_CENTRE_ALREADY_SET");
    }
    Requirement requirement = find(id);
    venues.lockGovernmentCentre(centerId, locked.state(), locked.district(), requirement.seats());
    jdbc.update("UPDATE training_requirements SET center_id=? WHERE requirement_id=?", centerId, id);
    if ("AWARDED".equals(locked.status())) {
      Application chosen = myApplication(id, requirement.selectedProviderId());
      if (chosen == null || !"SELECTED".equals(chosen.status())) throw missing();
      long bookingId = reserve(find(id), chosen);
      jdbc.update("UPDATE training_requirements SET root_booking_id=? WHERE requirement_id=?", bookingId, id);
    }
    audits.recordStateChange("UPDATE", "TrainingRequirement", id, "Training center assigned", null,
        Map.of("centerId", centerId), List.of());
    return find(id);
  }

  private long reserve(Requirement requirement, Application application) {
    validateProposal(application.startDate(), application.endDate(), application.dailyStartTime());
    requireEligibleProvider(application.providerId());
    return venues.reserveGovernmentTraining(requirement, application);
  }

  private Requirement find(long id) {
    return jdbc.query(REQUIREMENT_SELECT + " WHERE r.requirement_id=?", REQUIREMENT_ROW, id).stream()
        .findFirst().orElseThrow(TrainingRequirementService::missing);
  }

  private LockedRequirement lock(long id) {
    return jdbc.query("""
        SELECT state_id,district_id,requirement_status,center_id,root_booking_id
        FROM training_requirements WHERE requirement_id=? FOR UPDATE
        """, (rs, row) -> new LockedRequirement(rs.getLong(1), rs.getLong(2), rs.getString(3),
            nullableLong(rs, "center_id"), nullableLong(rs, "root_booking_id")), id).stream()
        .findFirst().orElseThrow(TrainingRequirementService::missing);
  }

  private Application myApplication(long id, long providerId) {
    return jdbc.query(APPLICATION_SELECT + " WHERE a.requirement_id=? AND a.provider_id=?",
        APPLICATION_ROW, id, providerId).stream().findFirst().orElse(null);
  }

  private static void requireLocation(TrainingTenantScopeSupport.CallerScope scope, long state, long district) {
    TrainingTenantScopeSupport.validateCenterOwnership(scope, null, state, district);
  }

  private void requireEligibleProvider(long providerId) {
    if (!access.eligibleProvider(providerId)) {
      throw new AccessDeniedException("An eligible training provider is required");
    }
  }

  private static void validateDates(LocalDate from, LocalDate to, LocalTime start, LocalTime end) {
    if (from.isBefore(today()) || to.isBefore(from) || !end.isAfter(start) || !to.atTime(end).isAfter(LocalDateTime.now(INDIA))
        || start.getNano() != 0 || end.getNano() != 0) {
      throw new BadRequestException("Enter a current or future date range and an increasing daily time window.",
          "INVALID_TRAINING_WINDOW");
    }
  }

  private static boolean accepting(ResultSet rs) throws SQLException {
    return "OPEN".equals(rs.getString("requirement_status")) && nullableLong(rs, "center_id") != null
        && !rs.getObject("latest_date", LocalDate.class).isBefore(today());
  }

  private static void validateProposal(LocalDate from, LocalDate to, LocalTime start) {
    if (!from.atTime(start).isAfter(LocalDateTime.now(INDIA))) {
      throw new ConflictException("The proposed training window has passed.", "TRAINING_WINDOW_EXPIRED");
    }
    if (ChronoUnit.DAYS.between(from, to) >= 366) {
      throw new BadRequestException("Propose at most 366 daily sessions.", "TRAINING_PROPOSAL_TOO_LONG");
    }
  }

  private static ResourceNotFoundException missing() {
    return new ResourceNotFoundException("TrainingRequirement", "id");
  }

  private static LocalDate today() { return LocalDate.now(INDIA); }

  private static String text(String value) { return value == null || value.isBlank() ? null : value.trim(); }

  private static Long nullableLong(ResultSet rs, String column) throws SQLException {
    Number value = (Number) rs.getObject(column);
    return value == null ? null : value.longValue();
  }

  private long insert(String sql, Object... values) {
    GeneratedKeyHolder key = new GeneratedKeyHolder();
    jdbc.update(connection -> {
      var statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
      for (int i = 0; i < values.length; i++) statement.setObject(i + 1, values[i]);
      return statement;
    }, key);
    return Objects.requireNonNull(key.getKey(), "Insert did not return an identity").longValue();
  }
}
