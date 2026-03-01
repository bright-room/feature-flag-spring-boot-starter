package net.brightroom.featureflag.core.resolution;

import static org.assertj.core.api.Assertions.assertThat;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.junit.jupiter.api.Test;

class HtmlResponseBuilderTest {

  @Test
  void buildHtml_containsDoctype() {
    var e = new FeatureFlagAccessDeniedException("my-feature");
    String result = HtmlResponseBuilder.buildHtml(e);
    assertThat(result).contains("<!DOCTYPE html>");
  }

  @Test
  void buildHtml_contains403InBody() {
    var e = new FeatureFlagAccessDeniedException("my-feature");
    String result = HtmlResponseBuilder.buildHtml(e);
    assertThat(result).contains("403");
  }

  @Test
  void buildHtml_containsFeatureName() {
    var e = new FeatureFlagAccessDeniedException("my-feature");
    String result = HtmlResponseBuilder.buildHtml(e);
    assertThat(result).contains("my-feature");
  }

  @Test
  void buildHtml_escapesHtmlCharactersInMessage() {
    var e = new FeatureFlagAccessDeniedException("<script>xss</script>");
    String result = HtmlResponseBuilder.buildHtml(e);
    assertThat(result).doesNotContain("<script>");
    assertThat(result).contains("&lt;script&gt;");
  }
}
