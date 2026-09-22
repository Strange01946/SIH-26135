package in.gov.sih.sih26135.service.impl;

import in.gov.sih.sih26135.dto.request.AssignCourseProgramRequest;
import in.gov.sih.sih26135.dto.response.CourseProgramResponse;
import in.gov.sih.sih26135.entity.Course;
import in.gov.sih.sih26135.entity.CourseProgram;
import in.gov.sih.sih26135.entity.CourseProgramId;
import in.gov.sih.sih26135.entity.Program;
import in.gov.sih.sih26135.exception.BadRequestException;
import in.gov.sih.sih26135.exception.ConflictException;
import in.gov.sih.sih26135.exception.ResourceNotFoundException;
import in.gov.sih.sih26135.mapper.CourseProgramMapper;
import in.gov.sih.sih26135.repository.CourseProgramRepository;
import in.gov.sih.sih26135.repository.CourseRepository;
import in.gov.sih.sih26135.repository.ProgramRepository;
import in.gov.sih.sih26135.repository.TrainingBatchRepository;
import in.gov.sih.sih26135.service.CourseProgramService;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CourseProgramServiceImpl implements CourseProgramService {

  private final CourseProgramRepository courseProgramRepository;
  private final CourseRepository courseRepository;
  private final ProgramRepository programRepository;
  private final TrainingBatchRepository trainingBatchRepository;
  private final CourseProgramMapper courseProgramMapper;

  public CourseProgramServiceImpl(
      CourseProgramRepository courseProgramRepository,
      CourseRepository courseRepository,
      ProgramRepository programRepository,
      TrainingBatchRepository trainingBatchRepository,
      CourseProgramMapper courseProgramMapper) {
    this.courseProgramRepository = courseProgramRepository;
    this.courseRepository = courseRepository;
    this.programRepository = programRepository;
    this.trainingBatchRepository = trainingBatchRepository;
    this.courseProgramMapper = courseProgramMapper;
  }

  @Override
  @Transactional
  public CourseProgramResponse assignCourseToProgram(AssignCourseProgramRequest request) {
    if (request == null) {
      throw new BadRequestException("Course-program assignment request cannot be null");
    }
    if (request.getCourseId() == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (request.getProgramId() == null) {
      throw new BadRequestException("Program ID is required");
    }

    Course course = courseRepository.findById(request.getCourseId())
        .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));

    Program program = programRepository.findById(request.getProgramId())
        .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));

    if (courseProgramRepository.existsByCourseIdAndProgramId(request.getCourseId(), request.getProgramId())) {
      throw new ConflictException("Course is already assigned to this program", "COURSE_PROGRAM_ALREADY_EXISTS");
    }

    CourseProgram cp = courseProgramMapper.toEntity(request);
    cp.setCreatedAt(LocalDateTime.now());

    CourseProgram saved = courseProgramRepository.save(cp);
    return courseProgramMapper.toResponse(saved, course, program);
  }

  @Override
  @Transactional
  public void removeCourseFromProgram(Long courseId, Long programId) {
    if (courseId == null) {
      throw new BadRequestException("Course ID is required");
    }
    if (programId == null) {
      throw new BadRequestException("Program ID is required");
    }

    CourseProgram cp = courseProgramRepository.findById(new CourseProgramId(courseId, programId))
        .orElseThrow(() -> new ResourceNotFoundException("CourseProgram", "courseId and programId"));

    if (trainingBatchRepository.existsByCourseIdAndProgramId(courseId, programId)) {
      throw new ConflictException(
          "Cannot remove course from program because training batches exist for this combination",
          "COURSE_PROGRAM_IN_USE"
      );
    }

    courseProgramRepository.delete(cp);
  }

  @Override
  public List<CourseProgramResponse> getProgramsForCourse(Long courseId) {
    Course course = courseRepository.findById(courseId)
        .orElseThrow(() -> new ResourceNotFoundException("Course", "courseId"));

    return courseProgramRepository.findByCourseId(courseId).stream()
        .map(cp -> {
          Program program = programRepository.findById(cp.getProgramId()).orElse(null);
          return courseProgramMapper.toResponse(cp, course, program);
        })
        .toList();
  }

  @Override
  public List<CourseProgramResponse> getCoursesForProgram(Long programId) {
    Program program = programRepository.findById(programId)
        .orElseThrow(() -> new ResourceNotFoundException("Program", "programId"));

    return courseProgramRepository.findByProgramId(programId).stream()
        .map(cp -> {
          Course course = courseRepository.findById(cp.getCourseId()).orElse(null);
          return courseProgramMapper.toResponse(cp, course, program);
        })
        .toList();
  }
}
