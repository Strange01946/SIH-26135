package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.CourseSkill;
import in.gov.sih.sih26135.entity.CourseSkillId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseSkillRepository extends JpaRepository<CourseSkill, CourseSkillId> {

  List<CourseSkill> findByCourseId(Long courseId);

  List<CourseSkill> findBySkillId(Long skillId);

  boolean existsByCourseIdAndSkillId(Long courseId, Long skillId);

  List<CourseSkill> findByCourseIdAndIsCoreSkillTrue(Long courseId);

  List<CourseSkill> findByTaughtSkillLevelId(Long taughtSkillLevelId);

  List<CourseSkill> findBySkillImportanceId(Long skillImportanceId);
}
