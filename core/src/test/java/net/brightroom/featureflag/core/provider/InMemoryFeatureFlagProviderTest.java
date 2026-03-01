package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(classes = FeatureFlagProviderTestConfiguration.class)
@TestPropertySource(
    properties = {
      "feature-flags.feature-names.experimental-stage-endpoint=true",
      "feature-flags.feature-names.development-stage-endpoint=false"
    })
class InMemoryFeatureFlagProviderTest {

  FeatureFlagProvider featureFlagProvider;

  @Test
  void undefinedFeaturesAreDisabledByDefault() {
    assertFalse(featureFlagProvider.isFeatureEnabled("undefined-api"));
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
