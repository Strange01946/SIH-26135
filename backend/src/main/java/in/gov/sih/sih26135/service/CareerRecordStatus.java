package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.trainee.TraineeSurvey;
import org.springframework.jdbc.core.JdbcTemplate;

/** Resolves the recorded current outcome while retaining the trainee's original profile declaration. */
public record CareerRecordStatus(long id, String code, String name, String source) {
  public static CareerRecordStatus current(JdbcTemplate jdbc, long traineeId, TraineeSurvey.CareerUpdate report) {
    if (report == null || Boolean.TRUE.equals(jdbc.queryForObject("""
        SELECT EXISTS(SELECT 1 FROM employment_records e
          JOIN ref_employment_info_source src USING(employment_info_source_id)
          JOIN ref_record_verification_status v USING(record_verification_status_id)
          WHERE e.trainee_id=? AND e.deleted_at IS NULL AND e.updated_at>=?
            AND (src.source_code='EMPLOYER_CONFIRMATION'
              OR (src.source_code='TRAINEE_DECLARATION' AND v.status_code='SELF_REPORTED')))
        """, Boolean.class, traineeId, report.submittedAt()))) return resolve(jdbc, traineeId);
    return new CareerRecordStatus(report.statusId(), report.statusCode(), report.status(), "SELF_REPORTED");
  }

  public static CareerRecordStatus resolve(JdbcTemplate jdbc, long traineeId) {
    return jdbc.queryForObject("""
        SELECT r.employment_status_id,r.status_code,r.status_name,work.source
        FROM (
          SELECT CASE WHEN et.is_apprenticeship=1 THEN 'APPRENTICE'
                   WHEN et.is_self_employment=1 THEN 'SELF_EMPLOYED'
                   WHEN et.is_wage_employment=1 THEN 'WAGE_EMPLOYED' END AS status_code,
                 'EMPLOYMENT_RECORD' AS source,1 AS priority,e.start_date,e.employment_id AS fact_id
          FROM employment_records e JOIN ref_engagement_type et ON et.engagement_type_id=e.engagement_type_id
          JOIN ref_employment_spell_status es ON es.employment_spell_status_id=e.employment_spell_status_id
          JOIN ref_employment_info_source src ON src.employment_info_source_id=e.employment_info_source_id
          JOIN ref_record_verification_status v ON v.record_verification_status_id=e.record_verification_status_id
          WHERE e.trainee_id=? AND e.deleted_at IS NULL AND e.is_current=1 AND es.is_active_flag=1
            AND (src.is_self_reported_flag=0 OR v.is_verified_flag=1
              OR (src.source_code='TRAINEE_DECLARATION' AND v.status_code='SELF_REPORTED'))
            AND v.status_code<>'REJECTED'
            AND e.start_date<=CURRENT_DATE AND (e.end_date IS NULL OR e.end_date>=CURRENT_DATE)
          UNION ALL
          SELECT s.status_code,'UNEMPLOYMENT_RECORD',2,u.start_date,u.unemployment_event_id
          FROM trainee_unemployment_events u JOIN ref_employment_status s ON s.employment_status_id=u.labour_status_id
          JOIN ref_employment_info_source src ON src.employment_info_source_id=u.employment_info_source_id
          JOIN ref_record_verification_status v ON v.record_verification_status_id=u.record_verification_status_id
          WHERE u.trainee_id=? AND u.is_current=1 AND u.start_date<=CURRENT_DATE
            AND (u.end_date IS NULL OR u.end_date>=CURRENT_DATE)
            AND (src.is_self_reported_flag=0 OR v.is_verified_flag=1) AND v.status_code<>'REJECTED'
          UNION ALL
          SELECT s.status_code,'PROFILE',3,NULL,t.trainee_id FROM trainees t
          JOIN ref_employment_status s ON s.employment_status_id=t.current_employment_status_id WHERE t.trainee_id=?
        ) work JOIN ref_employment_status r ON r.status_code=work.status_code
        ORDER BY work.priority,work.start_date DESC,work.fact_id DESC LIMIT 1
        """, (rs, row) -> new CareerRecordStatus(rs.getLong(1), rs.getString(2), rs.getString(3), rs.getString(4)),
        traineeId, traineeId, traineeId);
  }
}
