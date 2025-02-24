package net.brightroom.featureflag.configuration;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import net.brightroom.featureflag.Application;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = {Application.class})
class FeatureFlagAutoConfigurationTest {
  FeatureFlagProperties featureFlagProperties;

  @Test
  void providerFunctionsProperly() {
    Map<String, Boolean> features = featureFlagProperties.features();
    assertTrue(features.containsKey("new-api"));
    assertTrue(features.get("new-api"));

    assertTrue(features.containsKey("beta-feature"));
    assertFalse(features.get("beta-feature"));
  }

  @Autowired
  FeatureFlagAutoConfigurationTest(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
