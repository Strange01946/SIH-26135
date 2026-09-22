package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.response.AttendanceStatusResponse;
import java.util.List;

public interface AttendanceStatusReferenceService {

  List<AttendanceStatusResponse> getAllAttendanceStatuses();

  AttendanceStatusResponse getById(Long id);

  AttendanceStatusResponse getByCode(String statusCode);
}
