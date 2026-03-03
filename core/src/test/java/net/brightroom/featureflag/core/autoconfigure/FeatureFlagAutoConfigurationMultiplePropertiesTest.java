package net.brightroom.featureflag.core.autoconfigure;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Map;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.properties.ResponseProperties;
import net.brightroom.featureflag.core.properties.ResponseType;
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
  void shouldLoadMultipleFeatureFlags() {
    Map<String, Boolean> features = featureFlagProperties.featureNames();
    assertEquals(2, features.size());
    assertTrue(features.get("f1"));
    assertFalse(features.get("f2"));
  }

  @Test
  void shouldLoadDefaultEnabled() {
    assertTrue(featureFlagProperties.defaultEnabled());
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
