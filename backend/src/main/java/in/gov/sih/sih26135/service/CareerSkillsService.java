package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.career.CareerSkills.*;
import in.gov.sih.sih26135.entity.Trainee;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.repository.TraineeRepository;
import in.gov.sih.sih26135.security.WorkflowSecurityScopeValidator;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Comparator;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CareerSkillsService {
  private static final ZoneId INDIA = ZoneId.of("Asia/Kolkata");
  private static final Set<String> GENERAL_JOB_WORDS = Set.of("trainee", "junior", "senior", "worker", "operator", "technician",
      "specialist", "apprentice", "engineer", "assistant", "associate", "officer", "manager", "developer", "developers",
      "development", "and", "for", "the");
  private static final String ACTIVE_ROLES = "SELECT job_role_id,job_role_name FROM job_roles WHERE deleted_at IS NULL AND lifecycle_status_id=1";
  private static final String ACTIVE_SKILLS = "SELECT skill_id,skill_name FROM skills WHERE deleted_at IS NULL AND lifecycle_status_id=1";
  private static final String UPCOMING = """
      SELECT b.batch_id,b.batch_code,b.course_id,COALESCE(r.title,c.course_name) course_name,
             COALESCE(plan.skills_to_teach,c.description,c.course_name) skills_to_teach,
             p.provider_name,tc.center_name,d.district_name,st.state_name,b.start_date,b.end_date,bs.status_code,
             (SELECT MIN(s.starts_at) FROM venue_booking_sessions s WHERE s.booking_id=v.booking_id
                  AND s.reserved_at IS NOT NULL AND s.released_at IS NULL) first_session,
             CAST(b.capacity AS SIGNED) - CAST((SELECT COUNT(*) FROM training_enrollments e
                  WHERE e.batch_id=b.batch_id AND e.deleted_at IS NULL) AS SIGNED) available,
             i.decision, EXISTS(SELECT 1 FROM training_enrollments e WHERE e.trainee_id=?
                  AND e.batch_id=b.batch_id AND e.deleted_at IS NULL) enrolled
      FROM training_batches b
      JOIN ref_batch_status bs USING (batch_status_id)
      JOIN courses c USING (course_id)
      JOIN training_providers p ON p.provider_id=b.provider_id
      JOIN training_centers tc ON tc.center_id=b.center_id
      JOIN districts d ON d.district_id=tc.district_id
      JOIN states st ON st.state_id=tc.state_id
      LEFT JOIN venue_bookings v ON v.booking_id=(SELECT MAX(vb.booking_id) FROM venue_bookings vb
                                                    WHERE vb.batch_id=b.batch_id AND vb.booking_status='APPROVED')
      LEFT JOIN venue_booking_training_plans plan ON plan.booking_id=v.booking_id
      LEFT JOIN training_requirements r ON r.requirement_id=plan.requirement_id
      LEFT JOIN trainee_training_interests i ON i.batch_id=b.batch_id AND i.trainee_id=?
      WHERE b.deleted_at IS NULL AND p.deleted_at IS NULL AND tc.deleted_at IS NULL AND c.deleted_at IS NULL
        AND bs.status_code IN ('PLANNED','OPEN') AND b.start_date>=?
        AND (v.booking_id IS NOT NULL OR (bs.status_code='OPEN'
             AND NOT EXISTS(SELECT 1 FROM venue_bookings vb WHERE vb.batch_id=b.batch_id)))
        AND (? IS NULL OR b.batch_id=?)
      HAVING (first_session IS NULL OR first_session>?)
      ORDER BY b.start_date,first_session,b.batch_id
      """;

  private final JdbcTemplate jdbc;
  private final WorkflowSecurityScopeValidator scope;
  private final TraineeRepository trainees;
  private final TraineeConsentService consent;
  private final AssessorWorkbenchService assessor;

  public CareerSkillsService(JdbcTemplate jdbc, WorkflowSecurityScopeValidator scope, TraineeRepository trainees,
                             TraineeConsentService consent, AssessorWorkbenchService assessor) {
    this.jdbc = jdbc;
    this.scope = scope;
    this.trainees = trainees;
    this.consent = consent;
    this.assessor = assessor;
  }

  public Reference reference() {
    return new Reference(options(ACTIVE_ROLES + " ORDER BY job_role_name,job_role_id"),
        options(ACTIVE_SKILLS + " ORDER BY skill_name,skill_id"),
        options("SELECT skill_level_id,level_name FROM skill_levels ORDER BY level_rank"));
  }

  public Profile ownProfile() {
    return profile(ownTrainee().getId());
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Profile save(Save request) {
    Long actorId = scope.resolveAuthenticatedUserId();
    long traineeId = trainees.findLockedByUserId(actorId).filter(t -> t.getDeletedAt() == null).map(Trainee::getId)
        .orElseThrow(() -> new ResourceNotFoundException("No trainee profile is linked to this account."));
    long revision = goal(traineeId).revision();
    if (revision != request.revision()) {
      throw new ConflictException("This profile changed. Reload your skills before saving again.", "CAREER_SKILLS_CHANGED");
    }
    Long roleId = request.desiredRoleId();
    String roleTitle = cleanText(request.desiredJobTitle());
    if (roleId != null) {
      roleTitle = requireOption(ACTIVE_ROLES + " AND job_role_id=?", roleId, "Choose an active job role.").name();
    } else if (roleTitle != null) {
      var matching = jdbc.query(ACTIVE_ROLES + " AND job_role_name=? ORDER BY job_role_id LIMIT 2",
          (rs, row) -> new Option(rs.getLong(1), rs.getString(2)), roleTitle);
      if (matching.size() == 1) {
        roleId = matching.get(0).id();
        roleTitle = matching.get(0).name();
      }
    }
    Set<String> names = new HashSet<>();
    Set<Long> skillIds = new HashSet<>();
    List<Object[]> replacements = new ArrayList<>();
    for (SkillInput input : request.skills()) {
      String name = cleanText(input.name());
      if (name == null || !names.add(key(name))) {
        throw new BadRequestException("Enter each skill once.", "DUPLICATE_CAREER_SKILL");
      }
      var match = jdbc.query(ACTIVE_SKILLS + " AND skill_name=?", (rs, row) -> new Option(rs.getLong(1), rs.getString(2)), name)
          .stream().findFirst().orElse(null);
      if (match != null && !skillIds.add(match.id())) {
        throw new BadRequestException("Enter each skill once.", "DUPLICATE_CAREER_SKILL");
      }
      if (input.levelId() != null) requireLevel(input.levelId());
      replacements.add(new Object[]{traineeId, match == null ? name : match.name(), match == null ? null : match.id(), input.levelId()});
    }
    jdbc.update("""
        INSERT INTO trainee_career_goals(trainee_id,desired_job_role_id,desired_job_title,revision) VALUES (?,?,?,?)
        ON DUPLICATE KEY UPDATE desired_job_role_id=?,desired_job_title=?,revision=?
        """, traineeId, roleId, roleTitle, revision + 1, roleId, roleTitle, revision + 1);
    jdbc.update("DELETE FROM trainee_skills WHERE trainee_id=?", traineeId);
    jdbc.batchUpdate("INSERT INTO trainee_skills(trainee_id,skill_name,skill_id,skill_level_id) VALUES (?,?,?,?)", replacements);
    return profile(traineeId);
  }

  public Profile governmentProfile(long traineeId) {
    scope.validateTraineeScope(traineeId);
    requireProcessingConsent(traineeId);
    return profile(traineeId);
  }

  public Profile assessmentProfile(long assessmentId, long traineeId) {
    requireRosterCandidate(assessmentId, traineeId);
    requireProcessingConsent(traineeId);
    return profile(traineeId);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public Profile recordEvidence(long assessmentId, long traineeId, Observe input) {
    if (Boolean.TRUE.equals(jdbc.queryForObject(
        "SELECT EXISTS(SELECT 1 FROM batch_assessment_publications WHERE assessment_id=?)", Boolean.class, assessmentId))) {
      throw new in.gov.sih.sih26135.exception.ConflictException("Batch skills are awarded when results are published.", "AUTOMATIC_BATCH_SKILLS");
    }
    requireRosterCandidate(assessmentId, traineeId);
    trainees.findLockedById(traineeId).filter(t -> t.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("Trainee", "id"));
    requireProcessingConsent(traineeId);
    requireOption(ACTIVE_SKILLS + " AND skill_id=?", input.skillId(), "Choose an active catalog skill.");
    requireLevel(input.levelId());
    Long actorId = scope.resolveAuthenticatedUserId();
    jdbc.update("""
        INSERT INTO trainee_skill_evidence(assessment_id,trainee_id,skill_id,skill_level_id,assessed_by)
        VALUES (?,?,?,?,?) ON DUPLICATE KEY UPDATE skill_level_id=?,assessed_by=?,observed_at=CURRENT_TIMESTAMP(6)
        """, assessmentId, traineeId, input.skillId(), input.levelId(), actorId, input.levelId(), actorId);
    return profile(traineeId);
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void saveRequirement(long roleId, RoleRequirement input) {
    requireOption(ACTIVE_ROLES + " AND job_role_id=?", roleId, "Choose an active job role.");
    requireOption(ACTIVE_SKILLS + " AND skill_id=?", input.skillId(), "Choose an active catalog skill.");
    requireLevel(input.levelId());
    jdbc.update("""
        INSERT INTO job_role_skills(job_role_id,skill_id,required_skill_level_id,skill_importance_id)
        SELECT ?,?,?,skill_importance_id FROM ref_skill_importance WHERE importance_code='MANDATORY'
        ON DUPLICATE KEY UPDATE required_skill_level_id=?,skill_importance_id=VALUES(skill_importance_id)
        """, roleId, input.skillId(), input.levelId(), input.levelId());
  }

  public List<Training> ownTraining(String search) {
    Trainee trainee = ownTrainee();
    long traineeId = trainee.getId();
    var profile = profile(traineeId);
    Map<Long, String> gaps = new LinkedHashMap<>();
    profile.requirements().stream().filter(Requirement::needsTraining).forEach(g -> gaps.put(g.skillId(), g.skill()));
    jdbc.query("""
        SELECT s.skill_id,s.skill_name FROM skill_gaps g JOIN skills s USING (skill_id)
        JOIN ref_skill_gap_status gs USING (skill_gap_status_id)
        WHERE g.trainee_id=? AND g.is_current=1 AND gs.status_code IN ('OPEN','IN_REMEDIATION') AND s.deleted_at IS NULL
        """, (rs, row) -> new Option(rs.getLong(1), rs.getString(2)), traineeId)
        .forEach(g -> gaps.putIfAbsent(g.id(), g.name()));
    String skillIds = gaps.keySet().stream().map(String::valueOf).collect(Collectors.joining(",", "[", "]"));
    var courseMatches = gaps.isEmpty() ? Map.<Long, List<String>>of() : jdbc.query("""
        SELECT cs.course_id,cs.skill_id FROM course_skills cs
        JOIN JSON_TABLE(?, '$[*]' COLUMNS(skill_id BIGINT PATH '$')) selected USING (skill_id)
        UNION
        SELECT r.recommended_course_id,g.skill_id FROM skill_gap_recommendations r JOIN skill_gaps g USING (skill_gap_id)
        WHERE g.trainee_id=? AND g.is_current=1 AND r.recommended_course_id IS NOT NULL AND r.rejected_on IS NULL
        """, (rs, row) -> new CourseMatch(rs.getLong(1), rs.getLong(2)), skillIds, traineeId).stream()
        .filter(match -> gaps.containsKey(match.skillId()))
        .collect(Collectors.groupingBy(CourseMatch::courseId, Collectors.mapping(match -> gaps.get(match.skillId()), Collectors.toList())));
    var keywords = new KeywordSearch(search);
    var jobGoal = new KeywordSearch(profile.requirements().isEmpty() && profile.desiredJobTitle() != null
        ? java.util.Arrays.stream(profile.desiredJobTitle().toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{M}\\p{N}+#]+"))
            .filter(word -> word.length() >= 2 && !GENERAL_JOB_WORDS.contains(word)).collect(Collectors.joining(" "))
        : null);
    List<Training> matches = new ArrayList<>();
    for (Scheduled candidate : upcoming(traineeId, null)) {
      Training item = candidate.training();
      String subject = item.topic() + " " + item.skillsToTeach();
      Set<String> matchesSkills = new java.util.LinkedHashSet<>(courseMatches.getOrDefault(candidate.courseId(), List.of()));
      gaps.values().stream().filter(gap -> new KeywordSearch(gap).matchesSkill(subject)).forEach(matchesSkills::add);
      boolean chosen = item.choice() != Choice.UNDECIDED;
      boolean relatedToJobGoal = jobGoal.matchesSkill(subject);
      if (keywords.isEmpty() && matchesSkills.isEmpty() && !chosen && !relatedToJobGoal) continue;
      matches.add(new Training(item.batchId(), item.code(), item.topic(), item.skillsToTeach(), item.provider(), item.venue(),
          item.district(), item.state(), item.startsOn(), item.endsOn(), item.firstSession(), item.status(), item.seatsAvailable(),
          item.choice(), List.copyOf(matchesSkills), item.enrolled(), relatedToJobGoal));
    }
    var ordered = matches.stream().sorted(Comparator.comparingInt((Training t) -> t.matchedSkills().size()).reversed()
        .thenComparing(Training::startsOn).thenComparingLong(Training::batchId)).toList();
    return keywords.rank(ordered, t -> String.join(" ", t.topic(), t.skillsToTeach(), t.code(), t.district(), t.state(), t.provider()))
        .stream().limit(30).toList();
  }

  @Transactional(isolation = Isolation.READ_COMMITTED)
  public void saveInterest(long batchId, Interest input) {
    long traineeId = ownTrainee().getId();
    if (input.choice() == Choice.UNDECIDED) {
      jdbc.update("DELETE FROM trainee_training_interests WHERE trainee_id=? AND batch_id=?", traineeId, batchId);
      return;
    }
    var candidate = upcoming(traineeId, batchId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("This training is no longer upcoming."));
    if (candidate.training().enrolled()) {
      throw new ConflictException("You already have an enrollment for this batch.", "TRAINING_ALREADY_ENROLLED");
    }
    jdbc.update("""
        INSERT INTO trainee_training_interests(trainee_id,batch_id,decision) VALUES (?,?,?)
        ON DUPLICATE KEY UPDATE decision=?
        """, traineeId, batchId, input.choice().name(), input.choice().name());
  }

  public InterestCount providerInterest(long batchId) {
    Long providerId = scope.resolveAuthenticatedUser().map(user -> user.getTrainingProviderId())
        .orElseThrow(() -> new AccessDeniedException("Training provider assignment is required."));
    return jdbc.query("""
        SELECT COUNT(i.trainee_id) FROM training_batches b LEFT JOIN trainee_training_interests i
          ON i.batch_id=b.batch_id AND i.decision='INTERESTED'
        WHERE b.batch_id=? AND b.provider_id=? AND b.deleted_at IS NULL GROUP BY b.batch_id
        """, (rs, row) -> new InterestCount(rs.getLong(1)), batchId, providerId).stream().findFirst()
        .orElseThrow(() -> new ResourceNotFoundException("Training batch for this provider was not found."));
  }

  private Profile profile(long traineeId) {
    var goal = goal(traineeId);
    if (goal.roleId() == null && goal.title() != null) {
      var role = new KeywordSearch(goal.title()).resolve(options(ACTIVE_ROLES), Option::name, true);
      if (role != null) goal = new Goal(goal.revision(), role.id(), goal.title());
    }
    var skills = jdbc.query("""
        SELECT p.skill_name,COALESCE(p.skill_id,catalog.skill_id),p.skill_level_id,l.level_name,l.level_rank
        FROM trainee_skills p LEFT JOIN skill_levels l USING (skill_level_id)
        LEFT JOIN skills catalog ON p.skill_id IS NULL AND catalog.skill_name=p.skill_name
          AND catalog.deleted_at IS NULL AND catalog.lifecycle_status_id=1
        WHERE p.trainee_id=? ORDER BY p.skill_name
        """, (rs, row) -> new Skill(rs.getString(1), rs.getObject(2, Long.class), rs.getObject(3, Long.class),
            rs.getString(4), rs.getObject(5, Integer.class)), traineeId);
    var evidence = jdbc.query("""
        WITH observations AS (
          SELECT e.skill_id,e.skill_level_id,e.observed_at,'ASSESSMENT' source,e.assessment_id row_id
          FROM trainee_skill_evidence e JOIN assessments a USING (assessment_id)
          WHERE e.trainee_id=? AND a.deleted_at IS NULL
          UNION ALL
          SELECT g.skill_id,g.observed_skill_level_id,g.updated_at,'SKILL_GAP_ASSESSMENT',g.skill_gap_id
          FROM skill_gaps g WHERE g.trainee_id=? AND g.is_current=1 AND g.observed_skill_level_id IS NOT NULL
        ), ranked AS (
          SELECT *, ROW_NUMBER() OVER (PARTITION BY skill_id ORDER BY observed_at DESC,source,row_id DESC) position
          FROM observations
        )
        SELECT s.skill_id,s.skill_name,l.level_name,l.level_rank,r.observed_at,r.source
        FROM ranked r JOIN skills s USING (skill_id) JOIN skill_levels l USING (skill_level_id)
        WHERE r.position=1 AND s.deleted_at IS NULL ORDER BY s.skill_name
        """, (rs, row) -> new Evidence(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getInt(4),
            rs.getObject(5, LocalDateTime.class), rs.getString(6)), traineeId, traineeId);
    if (skills.stream().anyMatch(skill -> skill.skillId() == null)) {
      var catalog = options(ACTIVE_SKILLS);
      skills = skills.stream().map(skill -> resolveReportedSkill(skill, catalog)).toList();
    }
    var required = requirements(goal, skills, evidence);
    return new Profile(goal.revision(), goal.roleId(), goal.title(), skills, evidence, required);
  }

  private static Skill resolveReportedSkill(Skill skill, List<Option> catalog) {
    if (skill.skillId() != null) return skill;
    var option = new KeywordSearch(skill.name()).resolve(catalog, Option::name, false);
    return option == null ? skill : new Skill(skill.name(), option.id(), skill.levelId(), skill.level(), skill.rank());
  }

  private List<Requirement> requirements(Goal goal, List<Skill> skills, List<Evidence> evidence) {
    if (goal.roleId() == null && goal.title() == null) return List.of();
    var assessed = evidence.stream().collect(Collectors.toMap(Evidence::skillId, value -> value));
    var reported = skills.stream().filter(s -> s.skillId() != null).collect(Collectors.toMap(Skill::skillId, value -> value,
        (first, next) -> Comparator.comparing(Skill::rank, Comparator.nullsLast(Integer::compareTo)).compare(first, next) <= 0 ? first : next));
    return jdbc.query("""
        WITH demand AS (
          SELECT r.skill_id,l.level_rank,0 priority FROM job_role_skills r JOIN skill_levels l ON l.skill_level_id=r.required_skill_level_id
          JOIN job_roles j USING (job_role_id) WHERE r.job_role_id=? AND j.deleted_at IS NULL AND j.lifecycle_status_id=1
          UNION ALL
          SELECT ps.skill_id,l.level_rank,1 FROM job_posting_skills ps JOIN job_postings p USING (job_posting_id)
          JOIN job_roles j USING (job_role_id) JOIN skill_levels l ON l.skill_level_id=ps.required_skill_level_id
          JOIN ref_job_posting_status status USING (job_posting_status_id)
          WHERE p.deleted_at IS NULL AND j.deleted_at IS NULL AND j.lifecycle_status_id=1
            AND status.status_code='OPEN' AND p.posted_date<=CURRENT_DATE
            AND (p.closing_date IS NULL OR p.closing_date>=CURRENT_DATE)
            AND (p.job_role_id=? OR (? IS NULL AND (j.job_role_name=? OR p.posting_title=?)))
        ), ranked AS (
          SELECT *, ROW_NUMBER() OVER(PARTITION BY skill_id ORDER BY priority,level_rank DESC) position FROM demand
        )
        SELECT s.skill_id,s.skill_name,l.level_name,r.level_rank
        FROM ranked r JOIN skills s ON s.skill_id=r.skill_id JOIN skill_levels l ON l.level_rank=r.level_rank
        WHERE r.position=1 AND s.deleted_at IS NULL AND s.lifecycle_status_id=1 ORDER BY s.skill_name
        """, (rs, row) -> {
      long skillId = rs.getLong(1);
      int required = rs.getInt(4);
      var observation = assessed.get(skillId);
      var skill = reported.get(skillId);
      Integer rank = observation != null ? Integer.valueOf(observation.rank()) : skill == null ? null : skill.rank();
      String level = observation != null ? observation.level() : skill == null ? null : skill.level();
      String source = observation != null ? observation.source() : skill == null ? "UNRECORDED" : "SELF_REPORTED";
      return new Requirement(skillId, rs.getString(2), rs.getString(3), required, level, rank, source, rank == null || rank < required);
    }, goal.roleId(), goal.roleId(), goal.roleId(), goal.title(), goal.title());
  }

  private List<Scheduled> upcoming(long traineeId, Long batchId) {
    LocalDateTime now = LocalDateTime.now(INDIA);
    return jdbc.query(UPCOMING, (rs, row) -> {
      String decision = rs.getString("decision");
      return new Scheduled(rs.getLong("course_id"), new Training(rs.getLong("batch_id"), rs.getString("batch_code"),
          rs.getString("course_name"), rs.getString("skills_to_teach"), rs.getString("provider_name"), rs.getString("center_name"),
          rs.getString("district_name"), rs.getString("state_name"), rs.getObject("start_date", LocalDate.class),
          rs.getObject("end_date", LocalDate.class), rs.getObject("first_session", LocalDateTime.class), rs.getString("status_code"),
          Math.max(0, rs.getLong("available")), decision == null ? Choice.UNDECIDED : Choice.valueOf(decision), List.of(),
          rs.getBoolean("enrolled"), false));
    }, traineeId, traineeId, now.toLocalDate(), batchId, batchId, now);
  }

  private Goal goal(long traineeId) {
    return jdbc.query("""
        SELECT revision,COALESCE(desired_job_role_id,(SELECT CASE WHEN COUNT(*)=1 THEN MIN(job_role_id) END
               FROM job_roles r WHERE r.job_role_name=g.desired_job_title AND r.deleted_at IS NULL AND r.lifecycle_status_id=1)),
               desired_job_title FROM trainee_career_goals g WHERE trainee_id=?
        """, (rs, row) -> new Goal(rs.getLong(1), rs.getObject(2, Long.class), rs.getString(3)), traineeId)
        .stream().findFirst().orElse(new Goal(0, null, null));
  }

  private Trainee ownTrainee() {
    return scope.resolveAuthenticatedTrainee().filter(t -> t.getDeletedAt() == null)
        .orElseThrow(() -> new ResourceNotFoundException("No trainee profile is linked to this account."));
  }

  private void requireProcessingConsent(long traineeId) {
    if (!consent.hasActiveConsent(traineeId, "DATA_PROCESSING")) {
      throw new AccessDeniedException("The trainee needs active core data processing consent to share this profile with staff.");
    }
  }

  private void requireRosterCandidate(long assessmentId, long traineeId) {
    if (assessor.getAssessmentRoster(assessmentId).candidates().stream().noneMatch(candidate -> candidate.traineeId().equals(traineeId))) {
      throw new ResourceNotFoundException("Candidate is not on this assessment roster.");
    }
  }

  private List<Option> options(String query) {
    return jdbc.query(query, (rs, row) -> new Option(rs.getLong(1), rs.getString(2)));
  }

  private Option requireOption(String query, long id, String message) {
    return jdbc.query(query, (rs, row) -> new Option(rs.getLong(1), rs.getString(2)), id).stream().findFirst()
        .orElseThrow(() -> new BadRequestException(message, "INVALID_CAREER_REFERENCE"));
  }

  private void requireLevel(long id) {
    requireOption("SELECT skill_level_id,level_name FROM skill_levels WHERE skill_level_id=?", id, "Choose a valid skill level.");
  }

  private static String cleanText(String value) {
    if (value == null || value.isBlank()) return null;
    return Normalizer.normalize(value, Normalizer.Form.NFC).replaceAll("(?U)\\s+", " ").strip();
  }

  private static String key(String value) {
    return Normalizer.normalize(value.toLowerCase(Locale.ROOT), Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
  }

  private record Goal(long revision, Long roleId, String title) {}
  private record CourseMatch(long courseId, long skillId) {}
  private record Scheduled(long courseId, Training training) {}
}
