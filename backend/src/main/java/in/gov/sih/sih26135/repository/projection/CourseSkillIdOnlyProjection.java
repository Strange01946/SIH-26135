package in.gov.sih.sih26135.repository.projection;

/**
 * Spring Data JPA interface projection to retrieve only the {@code skillId}
 * for skills associated with a course, avoiding full {@code CourseSkill} entity loading.
 */
public interface CourseSkillIdOnlyProjection {

  Long getSkillId();
}
