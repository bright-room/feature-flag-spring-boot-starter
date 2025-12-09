package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {FeatureFlagAutoConfiguration.class})
class FeatureFlagAutoConfigurationTest {
  FeatureFlagProperties featureFlagProperties;

  @Test
  void shouldLoadIncludePathPattern() {
    List<String> pathPattern = featureFlagProperties.includePathPattern();

    assertEquals(1, pathPattern.size());
    assertEquals("/**", pathPattern.get(0));
  }

  @Test
  void shouldLoadExcludePathPattern() {
    List<String> pathPattern = featureFlagProperties.excludePathPattern();

    assertEquals(1, pathPattern.size());
    assertEquals("/health", pathPattern.get(0));
  }

  @Test
  void shouldLoadFeatureFlags() {
    Map<String, Boolean> features = featureFlagProperties.features();
    assertTrue(features.containsKey("new-api"));
    assertTrue(features.get("new-api"));

    assertTrue(features.containsKey("beta-feature"));
    assertFalse(features.get("beta-feature"));
  }

  @Test
  void shouldLoadResponse() {
    ResponseProperties response = featureFlagProperties.response();

    assertEquals(400, response.statusCode());

    assertEquals(ResponseType.Json, response.type);

    Map<String, String> body = response.body();
    assertTrue(body.containsKey("error"));
    assertEquals("Feature flag is disabled", body.get("error"));
  }

  @Autowired
  FeatureFlagAutoConfigurationTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
