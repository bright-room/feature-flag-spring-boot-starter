package net.brightroom.featureflag.webflux.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ReactiveFeatureFlagProviderTestConfiguration.class)
class InMemoryReactiveFeatureFlagProviderTest {

  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;

  @Test
  void undefinedFeaturesAreDisabledByDefault() {
    var result = reactiveFeatureFlagProvider.isFeatureEnabled("undefined-api").block();
    assertFalse(result);
  }

  @Test
  void validatePredefinedFeatures() {
    assertTrue(reactiveFeatureFlagProvider.isFeatureEnabled("experimental-stage-endpoint").block());
    assertFalse(reactiveFeatureFlagProvider.isFeatureEnabled("development-stage-endpoint").block());
  }

  @Autowired
  InMemoryReactiveFeatureFlagProviderTest(ReactiveFeatureFlagProvider reactiveFeatureFlagProvider) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
  }
}
