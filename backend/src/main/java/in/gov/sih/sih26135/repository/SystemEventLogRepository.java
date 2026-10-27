package in.gov.sih.sih26135.repository;

import in.gov.sih.sih26135.entity.SystemEventLog;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SystemEventLogRepository extends JpaRepository<SystemEventLog, Long> {

  List<SystemEventLog> findBySystemEventCategoryId(Long categoryId);

  List<SystemEventLog> findBySystemEventSeverityId(Long severityId);

  List<SystemEventLog> findByEventCode(String eventCode);

  List<SystemEventLog> findByActorUserId(Long actorUserId);

  List<SystemEventLog> findByEntityType(String entityType);

  List<SystemEventLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

  List<SystemEventLog> findBySourceComponent(String sourceComponent);

  List<SystemEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end);

  Page<SystemEventLog> findBySystemEventCategoryId(Long categoryId, Pageable pageable);

  Page<SystemEventLog> findBySystemEventSeverityId(Long severityId, Pageable pageable);

  Page<SystemEventLog> findByEventCode(String eventCode, Pageable pageable);

  Page<SystemEventLog> findByActorUserId(Long actorUserId, Pageable pageable);

  Page<SystemEventLog> findByEntityType(String entityType, Pageable pageable);

  Page<SystemEventLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);

  Page<SystemEventLog> findBySourceComponent(String sourceComponent, Pageable pageable);

  Page<SystemEventLog> findByOccurredAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);
}
