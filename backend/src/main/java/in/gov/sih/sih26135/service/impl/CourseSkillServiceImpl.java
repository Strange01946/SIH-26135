package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignCourseSkillRequest;
import in.gov.sih.sih26135.dto.request.UpdateCourseSkillRequest;
import in.gov.sih.sih26135.dto.response.CourseSkillResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.CourseSkill;
import in.gov.sih.sih26135.entity.CourseSkillId;
import in.gov.sih.sih26135.entity.RefSkillImportance;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillLevel;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CourseSkillMapper;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.CourseSkillRepository;
import in.gov.sih.sih26135.repository.RefSkillImportanceRepository;
import in.gov.sih.sih26135.repository.SkillLevelRepository;
import in.gov.sih.sih26135.repository.SkillRepository;
import in.gov.sih.sih26135.service.CourseSkillService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseSkillServiceImpl implements CourseSkillService {

  private final CourseSkillRepository courseSkillRepository;
  private final CourseRepository courseRepository;
  private final SkillRepository skillRepository;
  private final SkillLevelRepository skillLevelRepository;
  private final RefSkillImportanceRepository refSkillImportanceRepository;
  private final CourseSkillMapper courseSkillMapper;

  public CourseSkillServiceImpl(
      CourseSkillRepository courseSkillRepository,
      CourseRepository courseRepository,
      SkillRepository skillRepository,
      SkillLevelRepository skillLevelRepository,
      RefSkillImportanceRepository refSkillImportanceRepository,
      CourseSkillMapper courseSkillMapper) {
    this.courseSkillRepository = courseSkillRepository;
    this.courseRepository = courseRepository;
    this.skillRepository = skillRepository;
    this.skillLevelRepository = skillLevelRepository;
    this.refSkillImportanceRepository = refSkillImportanceRepository;
    this.courseSkillMapper = courseSkillMapper;
  }

  @Override
  @Transactional
  public CourseSkillResponse assignSkillToCourse(AssignCourseSkillRequest request) {
    if (request == null) {
      throw new BadRequestException("Course skill assignment request cannot be null");
    }
    if (request.getCourseId() == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (request.getSkillId() == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (request.getTaughtSkillLevelId() == null) {
      throw new BadRequestException("Taught skill level ID is required");
    }
    if (request.getSkillImportanceId() == null) {
      throw new BadRequestException("Skill importance ID is required");
    }

    if (courseSkillRepository.existsByCourseIdAndSkillId(request.getCourseId(), request.getSkillId())) {
      throw new ConflictException("Course skill mapping already exists", "COURSE_SKILL_ALREADY_EXISTS");
    }

    Course course = courseRepository.findById(request.getCourseId())
        .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));

    Skill skill = skillRepository.findById(request.getSkillId())
        .orElseThrow(() -> new ResourceNotFoundException("Skill", "skillId"));

    SkillLevel taughtSkillLevel = skillLevelRepository.findById(request.getTaughtSkillLevelId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "taughtSkillLevelId"));

    RefSkillImportance skillImportance = refSkillImportanceRepository.findById(request.getSkillImportanceId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "skillImportanceId"));

    CourseSkill entity = courseSkillMapper.toEntity(request, course, skill, taughtSkillLevel, skillImportance);
    LocalDateTime now = LocalDateTime.now();
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);

    CourseSkill saved = courseSkillRepository.save(entity);
    return courseSkillMapper.toResponse(saved, course, skill, taughtSkillLevel, skillImportance);
  }

  @Override
  @Transactional
  public CourseSkillResponse updateCourseSkill(Long courseId, Long skillId, UpdateCourseSkillRequest request) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Course skill update request cannot be null");
    }

    CourseSkill courseSkill = courseSkillRepository.findById(new CourseSkillId(courseId, skillId))
        .orElseThrow(() -> new ResourceNotFoundException("CourseSkill", "courseId and skillId"));

    if (request.getTaughtSkillLevelId() != null) {
      SkillLevel level = skillLevelRepository.findById(request.getTaughtSkillLevelId())
          .orElseThrow(() -> new ResourceNotFoundException("SkillLevel", "taughtSkillLevelId"));
      courseSkill.setTaughtSkillLevel(level);
    }

    if (request.getSkillImportanceId() != null) {
      RefSkillImportance importance = refSkillImportanceRepository.findById(request.getSkillImportanceId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillImportance", "skillImportanceId"));
      courseSkill.setSkillImportance(importance);
    }

    if (request.getIsCoreSkill() != null) {
      courseSkill.setIsCoreSkill(request.getIsCoreSkill());
    }

    courseSkill.setUpdatedAt(LocalDateTime.now());
    CourseSkill saved = courseSkillRepository.save(courseSkill);
    return courseSkillMapper.toResponse(saved);
  }

  @Override
  @Transactional
  public void removeSkillFromCourse(Long courseId, Long skillId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }

    CourseSkill courseSkill = courseSkillRepository.findById(new CourseSkillId(courseId, skillId))
        .orElseThrow(() -> new ResourceNotFoundException("CourseSkill", "courseId and skillId"));

    courseSkillRepository.delete(courseSkill);
  }

  @Override
  public List<CourseSkillResponse> getSkillsForCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return courseSkillRepository.findByCourseId(courseId).stream()
        .map(courseSkillMapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseSkillResponse> getCoreSkillsForCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return courseSkillRepository.findByCourseIdAndIsCoreSkillTrue(courseId).stream()
        .map(courseSkillMapper::toResponse)
        .toList();
  }

  @Override
  public List<CourseSkillResponse> getCoursesForSkill(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return courseSkillRepository.findBySkillId(skillId).stream()
        .map(courseSkillMapper::toResponse)
        .toList();
  }
}
