package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Course;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CourseRepository extends JpaRepository<Course, Long> {

  Optional<Course> findByCourseCode(String courseCode);

  boolean existsByCourseCode(String courseCode);

  List<Course> findBySectorId(Long sectorId);

  List<Course> findByIndustryId(Long industryId);

  List<Course> findByDeliveryModeId(Long deliveryModeId);
}
