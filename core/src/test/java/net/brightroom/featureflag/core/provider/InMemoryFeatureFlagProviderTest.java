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
      "feature-flags.features.experimental-stage-endpoint.enabled=true",
      "feature-flags.features.development-stage-endpoint.enabled=false",
      "feature-flags.response.type=json"
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
