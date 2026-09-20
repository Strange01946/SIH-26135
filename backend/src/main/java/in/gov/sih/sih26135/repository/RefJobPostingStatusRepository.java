package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.RefJobPostingStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RefJobPostingStatusRepository extends JpaRepository<RefJobPostingStatus, Long> {

  Optional<RefJobPostingStatus> findByStatusCode(String statusCode);

  boolean existsByStatusCode(String statusCode);

  List<RefJobPostingStatus> findByIsOpenFlag(Boolean isOpenFlag);
}
