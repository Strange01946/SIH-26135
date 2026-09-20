package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.AuditLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

  List<AuditLog> findByActorUserId(Long actorUserId);

  List<AuditLog> findByAuditActionId(Long auditActionId);

  List<AuditLog> findByEntityType(String entityType);

  List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

  List<AuditLog> findByCorrelationId(String correlationId);

  List<AuditLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);
}
