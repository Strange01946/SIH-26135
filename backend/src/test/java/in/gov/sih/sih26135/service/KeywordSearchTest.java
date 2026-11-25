package in.gov.sih.sih26135.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class KeywordSearchTest {
  @Test
  void ranksFullKeywordsBeforeRelatedWordsAndAllowsTypos() {
    var rows = List.of("Java Backend Development", "Java Developer", "Python Developer", "JavaScript Developer");
    assertThat(new KeywordSearch("java developer").rank(rows, Function.identity()))
        .startsWith("Java Developer", "Java Backend Development");
    assertThat(new KeywordSearch("javvaa").rank(rows, Function.identity()))
        .containsExactly("Java Backend Development", "Java Developer");
    assertThat(new KeywordSearch("java payroll").rank(rows, Function.identity())).isEmpty();
    assertThat(new KeywordSearch("java developer").score("JAVA BACKEND DEVLOPMENT")).isPositive();
  }

  @Test
  void resolvesOnlyOneCatalogSkillOrJobRole() {
    assertThat(new KeywordSearch("javvaa").resolve(List.of("Java", "JavaScript"), Function.identity(), false)).isEqualTo("Java");
    assertThat(new KeywordSearch("Java programming").resolve(List.of("Java", "JavaScript"), Function.identity(), false)).isEqualTo("Java");
    assertThat(new KeywordSearch("java").resolve(List.of("JavaScript"), Function.identity(), false)).isNull();
    assertThat(new KeywordSearch("Java").matchesSkill("JavaScript Basics")).isFalse();
    assertThat(new KeywordSearch("Java programming").matchesSkill("Hands-on Javvaa backend")).isTrue();
    assertThat(new KeywordSearch("java developer").resolve(
        List.of("Java Backend Development", "Java Frontend Development"), Function.identity(), true)).isNull();
    assertThat(new KeywordSearch("java developer").resolve(
        List.of("Java Backend Development", "Python Developer"), Function.identity(), true)).isEqualTo("Java Backend Development");
  }
}
