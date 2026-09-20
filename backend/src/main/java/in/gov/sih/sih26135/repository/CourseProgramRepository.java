package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.CourseProgram;
import in.gov.sih.sih26135.entity.CourseProgramId;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseProgramRepository extends JpaRepository<CourseProgram, CourseProgramId> {

  List<CourseProgram> findByCourseId(Long courseId);

  List<CourseProgram> findByProgramId(Long programId);

  boolean existsByCourseIdAndProgramId(Long courseId, Long programId);
}
