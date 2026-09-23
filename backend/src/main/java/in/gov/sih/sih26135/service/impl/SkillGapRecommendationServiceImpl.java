package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.CreateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillGapRecommendationRequest;
import in.gov.sih.sih26135.dto.response.SkillGapRecommendationResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.RefSkillGapActionType;
import in.gov.sih.sih26135.entity.Skill;
import in.gov.sih.sih26135.entity.SkillGap;
import in.gov.sih.sih26135.entity.SkillGapRecommendation;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.SkillGapRecommendationMapper;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.RefSkillGapActionTypeRepository;
import in.gov.sih.sih26135.repository.SkillGapRecommendationRepository;
import in.gov.sih.sih26135.repository.SkillGapRepository;
import in.gov.sih.sih26135.repository.SkillRepository;
import in.gov.sih.sih26135.repository.UserRepository;
import in.gov.sih.sih26135.service.SkillGapRecommendationService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SkillGapRecommendationServiceImpl implements SkillGapRecommendationService {

  private final SkillGapRecommendationRepository recommendationRepository;
  private final SkillGapRepository skillGapRepository;
  private final RefSkillGapActionTypeRepository actionTypeRepository;
  private final CourseRepository courseRepository;
  private final SkillRepository skillRepository;
  private final UserRepository userRepository;
  private final SkillGapRecommendationMapper mapper;

  public SkillGapRecommendationServiceImpl(
      SkillGapRecommendationRepository recommendationRepository,
      SkillGapRepository skillGapRepository,
      RefSkillGapActionTypeRepository actionTypeRepository,
      CourseRepository courseRepository,
      SkillRepository skillRepository,
      UserRepository userRepository,
      SkillGapRecommendationMapper mapper) {
    this.recommendationRepository = recommendationRepository;
    this.skillGapRepository = skillGapRepository;
    this.actionTypeRepository = actionTypeRepository;
    this.courseRepository = courseRepository;
    this.skillRepository = skillRepository;
    this.userRepository = userRepository;
    this.mapper = mapper;
  }

  @Override
  @Transactional
  public SkillGapRecommendationResponse createRecommendation(CreateSkillGapRecommendationRequest request) {
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    if (request.getSkillGapId() == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    SkillGap skillGap = skillGapRepository.findById(request.getSkillGapId())
        .orElseThrow(() -> new ResourceNotFoundException("SkillGap", "id"));

    Integer recommendationNumber = request.getRecommendationNumber();
    if (recommendationNumber == null) {
      List<SkillGapRecommendation> existing = recommendationRepository.findBySkillGapId(skillGap.getId());
      recommendationNumber = existing.stream()
          .mapToInt(SkillGapRecommendation::getRecommendationNumber)
          .max()
          .orElse(0) + 1;
    } else {
      if (recommendationNumber < 1) {
        throw new BadRequestException("Recommendation number must be greater than or equal to 1", "INVALID_RECOMMENDATION_NUMBER");
      }
      if (recommendationRepository.existsBySkillGapIdAndRecommendationNumber(skillGap.getId(), recommendationNumber)) {
        throw new ConflictException("Recommendation number already exists for this skill gap", "RECOMMENDATION_NUMBER_ALREADY_EXISTS");
      }
    }

    if (request.getSkillGapActionTypeId() == null) {
      throw new BadRequestException("Skill gap action type ID is required");
    }
    RefSkillGapActionType actionType = actionTypeRepository.findById(request.getSkillGapActionTypeId())
        .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapActionType", "id"));

    Course recommendedCourse = null;
    if (Boolean.TRUE.equals(actionType.getRequiresCourseFlag())) {
      if (request.getRecommendedCourseId() == null) {
        throw new BadRequestException("Action type requires a recommended course", "RECOMMENDATION_REQUIRES_COURSE");
      }
    }
    if (request.getRecommendedCourseId() != null) {
      recommendedCourse = courseRepository.findById(request.getRecommendedCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "recommendedCourseId"));
      if (recommendedCourse.getDeletedAt() != null) {
        throw new BadRequestException("Recommended course is soft-deleted", "COURSE_SOFT_DELETED");
      }

      Optional<SkillGapRecommendation> dup = recommendationRepository
          .findBySkillGapIdAndSkillGapActionTypeIdAndRecommendedCourseId(
              skillGap.getId(), actionType.getId(), recommendedCourse.getId());
      if (dup.isPresent()) {
        throw new ConflictException("A recommendation with this action type and course already exists for this skill gap",
            "RECOMMENDATION_DUPLICATE_ACTION_COURSE");
      }
    } else {
      List<SkillGapRecommendation> existing = recommendationRepository.findBySkillGapId(skillGap.getId());
      boolean nullCourseDup = existing.stream().anyMatch(e ->
          e.getSkillGapActionType() != null
              && e.getSkillGapActionType().getId().equals(actionType.getId())
              && e.getRecommendedCourse() == null
      );
      if (nullCourseDup) {
        throw new ConflictException(
            "A recommendation with this action type and no course already exists for this skill gap",
            "RECOMMENDATION_DUPLICATE_ACTION_COURSE"
        );
      }
    }

    Skill recommendedSkill = null;
    if (request.getRecommendedSkillId() != null) {
      recommendedSkill = skillRepository.findById(request.getRecommendedSkillId())
          .orElseThrow(() -> new ResourceNotFoundException("Skill", "recommendedSkillId"));
    }

    boolean isAccepted = Boolean.TRUE.equals(request.getIsAcceptedFlag());
    LocalDate acceptedOn = request.getAcceptedOn();
    if (acceptedOn != null && !isAccepted) {
      throw new BadRequestException("Accepted date requires accepted flag to be true", "ACCEPTED_ON_REQUIRES_ACCEPTED_FLAG");
    }
    if (isAccepted && acceptedOn == null) {
      acceptedOn = LocalDate.now();
    }

    if (request.getCreatedByUserId() != null && !userRepository.existsById(request.getCreatedByUserId())) {
      throw new ResourceNotFoundException("User", "createdByUserId");
    }

    SkillGapRecommendation recommendation = new SkillGapRecommendation(
        skillGap,
        recommendationNumber,
        actionType
    );
    recommendation.setRecommendedCourse(recommendedCourse);
    recommendation.setRecommendedSkill(recommendedSkill);
    recommendation.setIsAcceptedFlag(isAccepted);
    recommendation.setAcceptedOn(acceptedOn);
    recommendation.setRemarks(request.getRemarks() != null ? request.getRemarks().trim() : null);
    recommendation.setCreatedByUserId(request.getCreatedByUserId());

    SkillGapRecommendation saved = recommendationRepository.save(recommendation);
    return mapper.toResponse(saved);
  }

  @Override
  @Transactional
  public SkillGapRecommendationResponse updateRecommendation(Long id, UpdateSkillGapRecommendationRequest request) {
    if (id == null) {
      throw new BadRequestException("Recommendation ID is required");
    }
    if (request == null) {
      throw new BadRequestException("Request cannot be null");
    }

    SkillGapRecommendation recommendation = recommendationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "id"));

    RefSkillGapActionType effectiveActionType = recommendation.getSkillGapActionType();
    if (request.getSkillGapActionTypeId() != null) {
      effectiveActionType = actionTypeRepository.findById(request.getSkillGapActionTypeId())
          .orElseThrow(() -> new ResourceNotFoundException("RefSkillGapActionType", "id"));
      recommendation.setSkillGapActionType(effectiveActionType);
    }

    Course effectiveCourse = recommendation.getRecommendedCourse();
    if (request.getRecommendedCourseId() != null) {
      effectiveCourse = courseRepository.findById(request.getRecommendedCourseId())
          .orElseThrow(() -> new ResourceNotFoundException("Course", "recommendedCourseId"));
      if (effectiveCourse.getDeletedAt() != null) {
        throw new BadRequestException("Recommended course is soft-deleted", "COURSE_SOFT_DELETED");
      }
      recommendation.setRecommendedCourse(effectiveCourse);
    }

    if (Boolean.TRUE.equals(effectiveActionType.getRequiresCourseFlag()) && effectiveCourse == null) {
      throw new BadRequestException("Action type requires a recommended course", "RECOMMENDATION_REQUIRES_COURSE");
    }

    if (effectiveCourse != null) {
      Optional<SkillGapRecommendation> dup = recommendationRepository
          .findBySkillGapIdAndSkillGapActionTypeIdAndRecommendedCourseId(
              recommendation.getSkillGap().getId(), effectiveActionType.getId(), effectiveCourse.getId());
      if (dup.isPresent() && !dup.get().getId().equals(recommendation.getId())) {
        throw new ConflictException("A recommendation with this action type and course already exists for this skill gap",
            "RECOMMENDATION_DUPLICATE_ACTION_COURSE");
      }
    } else {
      final Long actionTypeId = effectiveActionType.getId();
      List<SkillGapRecommendation> existing = recommendationRepository
          .findBySkillGapId(recommendation.getSkillGap().getId());
      boolean nullCourseDup = existing.stream().anyMatch(e ->
          !e.getId().equals(recommendation.getId())
              && e.getSkillGapActionType() != null
              && e.getSkillGapActionType().getId().equals(actionTypeId)
              && e.getRecommendedCourse() == null
      );
      if (nullCourseDup) {
        throw new ConflictException(
            "A recommendation with this action type and no course already exists for this skill gap",
            "RECOMMENDATION_DUPLICATE_ACTION_COURSE"
        );
      }
    }

    if (request.getRecommendedSkillId() != null) {
      Skill skill = skillRepository.findById(request.getRecommendedSkillId())
          .orElseThrow(() -> new ResourceNotFoundException("Skill", "recommendedSkillId"));
      recommendation.setRecommendedSkill(skill);
    }

    if (request.getIsAcceptedFlag() != null) {
      recommendation.setIsAcceptedFlag(request.getIsAcceptedFlag());
    }

    if (request.getAcceptedOn() != null) {
      recommendation.setAcceptedOn(request.getAcceptedOn());
    }

    if (recommendation.getAcceptedOn() != null && !Boolean.TRUE.equals(recommendation.getIsAcceptedFlag())) {
      throw new BadRequestException("Accepted date requires accepted flag to be true", "ACCEPTED_ON_REQUIRES_ACCEPTED_FLAG");
    }
    if (Boolean.TRUE.equals(recommendation.getIsAcceptedFlag()) && recommendation.getAcceptedOn() == null) {
      recommendation.setAcceptedOn(LocalDate.now());
    }

    if (request.getCreatedByUserId() != null) {
      if (!userRepository.existsById(request.getCreatedByUserId())) {
        throw new ResourceNotFoundException("User", "createdByUserId");
      }
      recommendation.setCreatedByUserId(request.getCreatedByUserId());
    }

    if (request.getRemarks() != null) {
      recommendation.setRemarks(request.getRemarks().trim());
    }

    SkillGapRecommendation updated = recommendationRepository.save(recommendation);
    return mapper.toResponse(updated);
  }

  @Override
  public SkillGapRecommendationResponse getRecommendationById(Long id) {
    if (id == null) {
      throw new BadRequestException("Recommendation ID is required");
    }
    SkillGapRecommendation entity = recommendationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "id"));
    return mapper.toResponse(entity);
  }

  @Override
  public SkillGapRecommendationResponse getRecommendationBySkillGapAndNumber(Long skillGapId, Integer recommendationNumber) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    if (recommendationNumber == null) {
      throw new BadRequestException("Recommendation number is required");
    }
    SkillGapRecommendation entity = recommendationRepository.findBySkillGapIdAndRecommendationNumber(skillGapId, recommendationNumber)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "skillGapId and recommendationNumber"));
    return mapper.toResponse(entity);
  }

  @Override
  public List<SkillGapRecommendationResponse> getRecommendationsBySkillGap(Long skillGapId) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    return recommendationRepository.findBySkillGapId(skillGapId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapRecommendationResponse> getAcceptedRecommendationsBySkillGap(Long skillGapId) {
    if (skillGapId == null) {
      throw new BadRequestException("Skill gap ID is required");
    }
    return recommendationRepository.findBySkillGapIdAndIsAcceptedFlagTrue(skillGapId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapRecommendationResponse> getRecommendationsByActionType(Long actionTypeId) {
    if (actionTypeId == null) {
      throw new BadRequestException("Action type ID is required");
    }
    return recommendationRepository.findBySkillGapActionTypeId(actionTypeId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapRecommendationResponse> getRecommendationsByCourse(Long courseId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    return recommendationRepository.findByRecommendedCourseId(courseId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  public List<SkillGapRecommendationResponse> getRecommendationsBySkill(Long skillId) {
    if (skillId == null) {
      throw new BadRequestException("Skill ID is required");
    }
    return recommendationRepository.findByRecommendedSkillId(skillId).stream()
        .map(mapper::toResponse)
        .toList();
  }

  @Override
  @Transactional
  public void deleteRecommendation(Long id) {
    if (id == null) {
      throw new BadRequestException("Recommendation ID is required");
    }
    SkillGapRecommendation entity = recommendationRepository.findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("SkillGapRecommendation", "id"));
    recommendationRepository.delete(entity);
  }
}
