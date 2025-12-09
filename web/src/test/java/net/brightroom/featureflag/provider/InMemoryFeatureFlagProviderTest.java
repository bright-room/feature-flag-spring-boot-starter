package net.brightroom.featureflag.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.brightroom.featureflag.configuration.FeatureFlagMvcTestAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = FeatureFlagMvcTestAutoConfiguration.class)
class InMemoryFeatureFlagProviderTest {

  FeatureFlagProvider featureFlagProvider;

  @Test
  void undefinedFeaturesAreNotManagedByFeatureFlag() {
    assertTrue(featureFlagProvider.isFeatureEnabled("undefined-api"));
  }

  @Test
  void validatePredefinedFunctions() {
    assertTrue(featureFlagProvider.isFeatureEnabled("experimental-stage-endpoint"));
    assertFalse(featureFlagProvider.isFeatureEnabled("development-stage-endpoint"));
  }

  @Autowired
  InMemoryFeatureFlagProviderTest(FeatureFlagProvider featureFlagProvider) {
    this.featureFlagProvider = featureFlagProvider;
  }
}
