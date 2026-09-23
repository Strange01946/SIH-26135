package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.request.UpdateFollowupCampaignRequest;
import in.gov.sih.sih26135.dto.response.FollowupCampaignResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.FollowupCampaign;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.FollowupCampaignMapper;
import in.gov.sih.sih26135.repository.CourseProgramRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.FollowupCampaignRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.FollowupCampaignService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class FollowupCampaignServiceImpl implements FollowupCampaignService {

  private final FollowupCampaignRepository followupCampaignRepository;
  private final ProgramRepository programRepository;
  private final CourseRepository courseRepository;
  private final CourseProgramRepository courseProgramRepository;
  private final UserRepository userRepository;
  private final FollowupCampaignMapper followupCampaignMapper;

  public FollowupCampaignServiceImpl(
      FollowupCampaignRepository followupCampaignRepository,
      ProgramRepository programRepository,
      CourseRepository courseRepository,
      CourseProgramRepository courseProgramRepository,
      UserRepository userRepository,
      FollowupCampaignMapper followupCampaignMapper) {
    this.followupCampaignRepository = followupCampaignRepository;
    this.programRepository = programRepository;
    this.courseRepository = courseRepository;
    this.courseProgramRepository = courseProgramRepository;
    this.userRepository = userRepository;
    this.followupCampaignMapper = followupCampaignMapper;
  }

  @Override
  @Transactional
  public FollowupCampaignResponse createFollowupCampaign(CreateFollowupCampaignRequest request) {
    if (request == null) {
      throw new BadRequestException("Followup campaign creation request cannot be null");
    }
    if (request.getCampaignCode() == null || request.getCampaignCode().isBlank()) {
      throw new BadRequestException("Campaign code is required");
    }
    if (request.getCampaignName() == null || request.getCampaignName().isBlank()) {
      throw new BadRequestException("Campaign name is required");
    }
    if (request.getFollowupTypeId() == null) {
      throw new BadRequestException("Followup type ID is required");
    }
    if (request.getScheduledStartDate() == null) {
      throw new BadRequestException("Scheduled start date is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String code = request.getCampaignCode().trim();
    if (followupCampaignRepository.existsByCampaignCode(code)) {
      throw new ConflictException("Campaign code already exists", "CAMPAIGN_CODE_ALREADY_EXISTS");
    }

    if (request.getScheduledEndDate() != null && request.getScheduledEndDate().isBefore(request.getScheduledStartDate())) {
      throw new BadRequestException("Scheduled end date cannot be before scheduled start date", "INVALID_DATE_RANGE");
    }

    Program program = null;
    if (request.getProgramId() != null) {
      program = programRepository.findById(request.getProgramId())
          .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));
    }

    Course course = null;
    if (request.getCourseId() != null) {
      course = courseRepository.findById(request.getCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));
    }

    if (program != null && course != null) {
      if (!courseProgramRepository.existsByCourseIdAndProgramId(course.getId(), program.getId())) {
        throw new BadRequestException("Course is not mapped to the specified program", "COURSE_PROGRAM_MISMATCH");
      }
    }

    if (request.getCreatedByUserId() != null) {
      if (!userRepository.existsById(request.getCreatedByUserId())) {
        throw new ResourceNotFoundException("User", "createdByUserId");
      }
    }

    FollowupCampaign campaign = followupCampaignMapper.toEntity(request, program, course);
    campaign.setCampaignCode(code);
    campaign.setCampaignName(request.getCampaignName().trim());

    LocalDateTime now = LocalDateTime.now();
    campaign.setCreatedAt(now);
    campaign.setUpdatedAt(now);

    FollowupCampaign saved = followupCampaignRepository.save(campaign);
    return followupCampaignMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public FollowupCampaignResponse updateFollowupCampaign(Long id, UpdateFollowupCampaignRequest request) {
    if (id == null) {
      throw new BadRequestException("Campaign ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Update request cannot be null");
    }

    FollowupCampaign campaign = followupCampaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "id"));

    if (campaign.getDeletedAt() != null) {
      throw new BadRequestException("Cannot update soft-deleted followup campaign", "CAMPAIGN_ALREADY_DELETED");
    }

    if (request.getCampaignName() != null && !request.getCampaignName().isBlank()) {
      campaign.setCampaignName(request.getCampaignName().trim());
    }

    if (request.getFollowupTypeId() != null) {
      campaign.setFollowupTypeId(request.getFollowupTypeId());
    }

    if (request.getSurveyId() != null) {
      campaign.setSurveyId(request.getSurveyId());
    }

    LocalDate effectiveStart = request.getScheduledStartDate() != null
        ? request.getScheduledStartDate() : campaign.getScheduledStartDate();
    LocalDate effectiveEnd = request.getScheduledEndDate() != null
        ? request.getScheduledEndDate() : campaign.getScheduledEndDate();

    if (effectiveEnd != null && effectiveEnd.isBefore(effectiveStart)) {
      throw new BadRequestException("Scheduled end date cannot be before scheduled start date", "INVALID_DATE_RANGE");
    }

    if (request.getScheduledStartDate() != null) {
      campaign.setScheduledStartDate(request.getScheduledStartDate());
    }
    if (request.getScheduledEndDate() != null) {
      campaign.setScheduledEndDate(request.getScheduledEndDate());
    }

    Long effectiveProgramId = request.getProgramId() != null
        ? request.getProgramId() : (campaign.getProgram() != null ? campaign.getProgram().getId() : null);
    Long effectiveCourseId = request.getCourseId() != null
        ? request.getCourseId() : (campaign.getCourse() != null ? campaign.getCourse().getId() : null);

    if (request.getProgramId() != null) {
      Program prog = programRepository.findById(request.getProgramId())
          .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));
      campaign.setProgram(prog);
    }

    if (request.getCourseId() != null) {
      Course crs = courseRepository.findById(request.getCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));
      campaign.setCourse(crs);
    }

    if (effectiveProgramId != null && effectiveCourseId != null) {
      if (!courseProgramRepository.existsByCourseIdAndProgramId(effectiveCourseId, effectiveProgramId)) {
        throw new BadRequestException("Course is not mapped to the specified program", "COURSE_PROGRAM_MISMATCH");
      }
    }

    if (request.getLifecycleStatusId() != null) {
      campaign.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    campaign.setUpdatedAt(LocalDateTime.now());
    FollowupCampaign updated = followupCampaignRepository.save(campaign);
    return followupCampaignMapper.toResponse(updated);
  }

  @Override
  public FollowupCampaignResponse getFollowupCampaignById(Long id) {
    if (id == null) {
      throw new BadRequestException("Campaign ID is required");
    }
    FollowupCampaign campaign = followupCampaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "id"));
    return followupCampaignMapper.toResponse(campaign);
  }

  @Override
  public FollowupCampaignResponse getFollowupCampaignByCode(String campaignCode) {
    if (campaignCode == null || campaignCode.isBlank()) {
      throw new BadRequestException("Campaign code is required");
    }
    FollowupCampaign campaign = followupCampaignRepository.findByCampaignCode(campaignCode.trim())
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "campaignCode"));
    return followupCampaignMapper.toResponse(campaign);
  }

  @Override
  public List<FollowupCampaignResponse> getAllActiveFollowupCampaigns() {
    return followupCampaignRepository.findByDeletedAtIsNull().stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsByFollowupType(Long followupTypeId) {
    if (followupTypeId == null) {
      throw new BadRequestException("Followup type ID is required");
    }
    return followupCampaignRepository.findByFollowupTypeId(followupTypeId).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsBySurvey(Long surveyId) {
    if (surveyId == null) {
      throw new BadRequestException("Survey ID is required");
    }
    return followupCampaignRepository.findBySurveyId(surveyId).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsByProgram(Long programId) {
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }
    return followupCampaignRepository.findByProgramId(programId).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsByCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return followupCampaignRepository.findByCourseId(courseId).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsByLifecycleStatus(Long lifecycleStatusId) {
    if (lifecycleStatusId == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }
    return followupCampaignRepository.findByLifecycleStatusId(lifecycleStatusId).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  public List<FollowupCampaignResponse> getFollowupCampaignsByScheduledStartDate(LocalDate scheduledStartDate) {
    if (scheduledStartDate == null) {
      throw new BadRequestException("Scheduled start date is required");
    }
    return followupCampaignRepository.findByScheduledStartDate(scheduledStartDate).stream()
        .map(followupCampaignMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteFollowupCampaign(Long id) {
    if (id == null) {
      throw new BadRequestException("Campaign ID is required");
    }
    FollowupCampaign campaign = followupCampaignRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("FollowupCampaign", "id"));

    if (campaign.getDeletedAt() != null) {
      throw new BadRequestException("Followup campaign is already deleted", "CAMPAIGN_ALREADY_DELETED");
    }

    LocalDateTime now = LocalDateTime.now();
    campaign.setDeletedAt(now);
    campaign.setUpdatedAt(now);
    followupCampaignRepository.save(campaign);
  }
}
