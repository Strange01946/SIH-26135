package in.gov.sih.sih26135.service.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.gov.sih.sih26135.dto.response.AuditStateChange;
import java.util.List;
import java.util.Map;

/** Encodes server-generated, allowlisted snapshots in the existing immutable audit summary column. */
public final class AuditSnapshot {
  private static final String PREFIX = "@audit-state-v1 ";
  private static final ObjectMapper JSON = new ObjectMapper();

  private AuditSnapshot() {}

  public static boolean isReserved(String summary) {
    return summary != null && summary.stripLeading().startsWith(PREFIX);
  }

  public static String encode(String summary, Map<String, ?> before, Map<String, ?> after, List<String> redactedFields) {
    var value = JSON.createObjectNode();
    value.put("summary", summary);
    value.set("before", JSON.valueToTree(before));
    value.set("after", JSON.valueToTree(after));
    value.set("redactedFields", JSON.valueToTree(redactedFields));
    String record = PREFIX + value;
    if (record.length() > 1000) {
      throw new IllegalStateException("Audit state change exceeds the summary column capacity");
    }
    return record;
  }

  public static StoredSummary decode(String summary) {
    if (summary == null || !summary.startsWith(PREFIX)) {
      return new StoredSummary(summary, null);
    }
    try {
      var value = JSON.readTree(summary.substring(PREFIX.length()));
      var before = value.path("before");
      var after = value.path("after");
      if ((!before.isObject() && !before.isNull()) || (!after.isObject() && !after.isNull())) {
        return new StoredSummary("Audit snapshot unavailable", null);
      }
      List<String> redacted = new java.util.ArrayList<>();
      value.path("redactedFields").forEach(node -> {
        if (node.isTextual()) {
          redacted.add(node.asText());
        }
      });
      return new StoredSummary(value.path("summary").asText("Entity changed"),
          new AuditStateChange(before, after, List.copyOf(redacted)));
    } catch (JsonProcessingException ex) {
      return new StoredSummary("Audit snapshot unavailable", null);
    }
  }

  public record StoredSummary(String text, AuditStateChange change) {}
}
