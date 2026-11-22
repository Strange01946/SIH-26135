package in.gov.sih.sih26135.dto.registration;

import java.util.List;

public class RegistrationReferenceDataResponse {

  public static class ReferenceItem {
    private Long id;
    private String code;
    private String name;
    private Long parentId;

    public ReferenceItem() {
    }

    public ReferenceItem(Long id, String code, String name) {
      this.id = id;
      this.code = code;
      this.name = name;
    }

    public ReferenceItem(Long id, String code, String name, Long parentId) {
      this.id = id;
      this.code = code;
      this.name = name;
      this.parentId = parentId;
    }

    public Long getId() {
      return id;
    }

    public void setId(Long id) {
      this.id = id;
    }

    public String getCode() {
      return code;
    }

    public void setCode(String code) {
      this.code = code;
    }

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public Long getParentId() {
      return parentId;
    }

    public void setParentId(Long parentId) {
      this.parentId = parentId;
    }
  }

  private List<ReferenceItem> genders;
  private List<ReferenceItem> states;
  private List<ReferenceItem> districts;
  private List<ReferenceItem> organizationTypes;
  private List<ReferenceItem> employmentStatuses;

  public RegistrationReferenceDataResponse() {
  }

  public List<ReferenceItem> getGenders() {
    return genders;
  }

  public void setGenders(List<ReferenceItem> genders) {
    this.genders = genders;
  }

  public List<ReferenceItem> getStates() {
    return states;
  }

  public void setStates(List<ReferenceItem> states) {
    this.states = states;
  }

  public List<ReferenceItem> getDistricts() {
    return districts;
  }

  public void setDistricts(List<ReferenceItem> districts) {
    this.districts = districts;
  }

  public List<ReferenceItem> getOrganizationTypes() {
    return organizationTypes;
  }

  public void setOrganizationTypes(List<ReferenceItem> organizationTypes) {
    this.organizationTypes = organizationTypes;
  }

  public List<ReferenceItem> getEmploymentStatuses() {
    return employmentStatuses;
  }

  public void setEmploymentStatuses(List<ReferenceItem> employmentStatuses) {
    this.employmentStatuses = employmentStatuses;
  }
}
