package in.gov.sih.sih26135.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;

public record AuditStateChange(JsonNode before, JsonNode after, List<String> redactedFields) {}
