package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.Organization;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface OrganizationRepository extends JpaRepository<Organization, Long> {

  Optional<Organization> findByOrganizationCode(String organizationCode);

  boolean existsByOrganizationCode(String organizationCode);
}
