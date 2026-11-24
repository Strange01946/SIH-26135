package in.gov.sih.sih26135.communication.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CommunicationCenterMaskingTest {
  @Test
  void masksRecipientHintsBeforeTheyReachTheApi() {
    assertThat(CommunicationCenterQueryService.mask("EMAIL", "testcandidate@example.com"))
        .isEqualTo("t***e@example.com");
    assertThat(CommunicationCenterQueryService.mask("EMAIL", "ab@example.com"))
        .isEqualTo("***@example.com");
    assertThat(CommunicationCenterQueryService.mask("SMS", "+91 92847 01123"))
        .isEqualTo("+91-XXXXX-XX123");
    assertThat(CommunicationCenterQueryService.mask("WHATSAPP", "9284701123"))
        .isEqualTo("XXXXX-XX123");
  }

  @Test
  void withholdsUnrecognizedAndIncompleteContacts() {
    assertThat(CommunicationCenterQueryService.mask("EMAIL", "private candidate data")).isNull();
    assertThat(CommunicationCenterQueryService.mask("EMAIL", "one@two@example.com")).isNull();
    assertThat(CommunicationCenterQueryService.mask("SMS", "123")).isNull();
    assertThat(CommunicationCenterQueryService.mask("SMS", "testcandidate@example.com")).isNull();
    assertThat(CommunicationCenterQueryService.mask("WHATSAPP", null)).isNull();
  }
}
