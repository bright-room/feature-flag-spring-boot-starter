package net.brightroom.featureflag.core.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = {FeatureFlagAutoConfiguration.class})
@TestPropertySource(
    properties = {
      "spring.config.additional-location=classpath:/application-multiple-properties.yaml"
    })
class FeatureFlagAutoConfigurationMultiplePropertiesTest {

  FeatureFlagProperties featureFlagProperties;

  @Test
  void shouldLoadMultipleIncludePathPatterns() {
    List<String> pathPattern = featureFlagProperties.pathPatterns().includes();
    assertEquals(2, pathPattern.size());
    assertTrue(pathPattern.contains("/api/**"));
    assertTrue(pathPattern.contains("/web/**"));
  }

  @Test
  void shouldLoadMultipleExcludePathPatterns() {
    List<String> pathPattern = featureFlagProperties.pathPatterns().excludes();
    assertEquals(2, pathPattern.size());
    assertTrue(pathPattern.contains("/internal/**"));
    assertTrue(pathPattern.contains("/health/**"));
  }

  @Test
  void shouldLoadMultipleFeatureFlags() {
    Map<String, Boolean> features = featureFlagProperties.featureNames();
    assertEquals(2, features.size());
    assertTrue(features.get("f1"));
    assertFalse(features.get("f2"));
  }

  @Test
  void shouldLoadResponseType() {
    ResponseProperties response = featureFlagProperties.response();
    assertEquals(ResponseType.PLAIN_TEXT, response.type());
  }

  @Autowired
  FeatureFlagAutoConfigurationMultiplePropertiesTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
