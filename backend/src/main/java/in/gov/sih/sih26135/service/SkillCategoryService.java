package in.gov.sih.sih26135.service;

import in.gov.sih.sih26135.dto.request.CreateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.request.UpdateSkillCategoryRequest;
import in.gov.sih.sih26135.dto.response.SkillCategoryResponse;
import java.util.List;

public interface SkillCategoryService {

  SkillCategoryResponse getById(Long id);

  SkillCategoryResponse getByCode(String categoryCode);

  List<SkillCategoryResponse> getAllSkillCategories();

  List<SkillCategoryResponse> getSkillCategoriesByParentId(Long parentCategoryId);

  SkillCategoryResponse createSkillCategory(CreateSkillCategoryRequest request);

  SkillCategoryResponse updateSkillCategory(Long id, UpdateSkillCategoryRequest request);

  void deleteSkillCategory(Long id);
}
