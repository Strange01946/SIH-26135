package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateCourseRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseRequest;
import in.gov.sih.sih26135.dto.response.CourseResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.Industry;
import in.gov.sih.sih26135.entity.RefDeliveryMode;
import in.gov.sih.sih26135.entity.RefQualificationLevel;
import in.gov.sih.sih26135.entity.Sector;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CourseMapper;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.IndustryRepository;
import in.gov.sih.sih26135.repository.RefDeliveryModeRepository;
import in.gov.sih.sih26135.repository.RefQualificationLevelRepository;
import in.gov.sih.sih26135.repository.SectorRepository;
import in.gov.sih.sih26135.service.CourseService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseServiceImpl implements CourseService {

  private final CourseRepository courseRepository;
  private final SectorRepository sectorRepository;
  private final IndustryRepository industryRepository;
  private final RefQualificationLevelRepository refQualificationLevelRepository;
  private final RefDeliveryModeRepository refDeliveryModeRepository;
  private final CourseMapper courseMapper;

  public CourseServiceImpl(
      CourseRepository courseRepository,
      SectorRepository sectorRepository,
      IndustryRepository industryRepository,
      RefQualificationLevelRepository refQualificationLevelRepository,
      RefDeliveryModeRepository refDeliveryModeRepository,
      CourseMapper courseMapper) {
    this.courseRepository = courseRepository;
    this.sectorRepository = sectorRepository;
    this.industryRepository = industryRepository;
    this.refQualificationLevelRepository = refQualificationLevelRepository;
    this.refDeliveryModeRepository = refDeliveryModeRepository;
    this.courseMapper = courseMapper;
  }

  @Override
  public CourseResponse getById(Long id) {
    Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course", "id"));
    return courseMapper.toResponse(course);
  }

  @Override
  public CourseResponse getByCode(String courseCode) {
    Course course = courseRepository.findByCourseCode(courseCode)
        .orElseThrow(() -> new ResourceNotFoundException("Course", "courseCode"));
    return courseMapper.toResponse(course);
  }

  @Override
  public List<CourseResponse> getAllCourses() {
    return courseRepository.findAll().stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseResponse> getCoursesBySectorId(Long sectorId) {
    return courseRepository.findBySectorId(sectorId).stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseResponse> getCoursesByIndustryId(Long industryId) {
    return courseRepository.findByIndustryId(industryId).stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseResponse> getCoursesByDeliveryModeId(Long deliveryModeId) {
    return courseRepository.findByDeliveryModeId(deliveryModeId).stream()
        .map(courseMapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public CourseResponse createCourse(CreateCourseRequest request) {
    if (request == null) {
      throw new BadRequestException("Course creation request cannot be null");
    }
    if (request.getCourseCode() == null || request.getCourseCode().isBlank()) {
      throw new BadRequestException("Course code is required");
    }
    if (request.getCourseName() == null || request.getCourseName().isBlank()) {
      throw new BadRequestException("Course name is required");
    }
    if (request.getSectorId() == null) {
      throw new BadRequestException("Sector ID is required");
    }
    if (request.getDeliveryModeId() == null) {
      throw new BadRequestException("Delivery mode ID is required");
    }
    if (request.getLifecycleStatusId() == null) {
      throw new BadRequestException("Lifecycle status ID is required");
    }

    String courseCode = request.getCourseCode().trim();
    if (courseRepository.existsByCourseCode(courseCode)) {
      throw new ConflictException("Course code already exists", "COURSE_CODE_ALREADY_EXISTS");
    }

    if (request.getDurationHours() != null && request.getDurationHours() <= 0) {
      throw new BadRequestException("Duration hours must be greater than 0", "INVALID_DURATION_HOURS");
    }

    if (request.getDurationDays() != null && request.getDurationDays() <= 0) {
      throw new BadRequestException("Duration days must be greater than 0", "INVALID_DURATION_DAYS");
    }

    Sector sector = sectorRepository.findById(request.getSectorId())
        .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));

    Industry industry = null;
    if (request.getIndustryId() != null) {
      industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
    }

    RefQualificationLevel qualificationLevel = null;
    if (request.getQualificationLevelId() != null) {
      qualificationLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
    }

    RefDeliveryMode deliveryMode = refDeliveryModeRepository.findById(request.getDeliveryModeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefDeliveryMode", "deliveryModeId"));

    Course course = courseMapper.toEntity(request, sector, industry, qualificationLevel, deliveryMode);
    course.setCourseCode(courseCode);
    course.setCourseName(request.getCourseName().trim());
    if (request.getDescription() != null) {
      course.setDescription(request.getDescription().trim());
    }

    LocalDateTime now = LocalDateTime.now();
    course.setCreatedAt(now);
    course.setUpdatedAt(now);

    Course saved = courseRepository.save(course);
    return courseMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public CourseResponse updateCourse(Long id, UpdateCourseRequest request) {
    if (request == null) {
      throw new BadRequestException("Course update request cannot be null");
    }

    Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course", "id"));

    if (request.getDurationHours() != null) {
      if (request.getDurationHours() <= 0) {
        throw new BadRequestException("Duration hours must be greater than 0", "INVALID_DURATION_HOURS");
      }
      course.setDurationHours(request.getDurationHours());
    }

    if (request.getDurationDays() != null) {
      if (request.getDurationDays() <= 0) {
        throw new BadRequestException("Duration days must be greater than 0", "INVALID_DURATION_DAYS");
      }
      course.setDurationDays(request.getDurationDays());
    }

    if (request.getSectorId() != null) {
      Sector sector = sectorRepository.findById(request.getSectorId())
          .orElseThrow(() -> new ResourceNotFoundException("Sector", "sectorId"));
      course.setSector(sector);
    }

    if (request.getIndustryId() != null) {
      Industry industry = industryRepository.findById(request.getIndustryId())
          .orElseThrow(() -> new ResourceNotFoundException("Industry", "industryId"));
      course.setIndustry(industry);
    }

    if (request.getQualificationLevelId() != null) {
      RefQualificationLevel qualificationLevel = refQualificationLevelRepository.findById(request.getQualificationLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("RefQualificationLevel", "qualificationLevelId"));
      course.setQualificationLevel(qualificationLevel);
    }

    if (request.getDeliveryModeId() != null) {
      RefDeliveryMode deliveryMode = refDeliveryModeRepository.findById(request.getDeliveryModeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefDeliveryMode", "deliveryModeId"));
      course.setDeliveryMode(deliveryMode);
    }

    if (request.getCourseName() != null && !request.getCourseName().isBlank()) {
      course.setCourseName(request.getCourseName().trim());
    }
    if (request.getDescription() != null) {
      course.setDescription(request.getDescription().trim());
    }
    if (request.getLifecycleStatusId() != null) {
      course.setLifecycleStatusId(request.getLifecycleStatusId());
    }

    course.setUpdatedAt(LocalDateTime.now());
    Course saved = courseRepository.save(course);
    return courseMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void deleteCourse(Long id) {
    Course course = courseRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Course", "id"));

    LocalDateTime now = LocalDateTime.now();
    course.setDeletedAt(now);
    course.setUpdatedAt(now);
    courseRepository.save(course);
  }
}
