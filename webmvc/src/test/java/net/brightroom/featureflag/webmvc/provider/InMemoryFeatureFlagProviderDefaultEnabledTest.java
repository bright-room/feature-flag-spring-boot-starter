package net.brightroom.featureflag.webmvc.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryFeatureFlagProviderDefaultEnabledTest {

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsUndefined_andDefaultEnabledIsFalse() {
    var provider = new InMemoryFeatureFlagProvider(Map.of("flag-a", true), false);
    assertFalse(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsUndefined_andDefaultEnabledIsTrue() {
    var provider = new InMemoryFeatureFlagProvider(Map.of("flag-a", true), true);
    assertTrue(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsExplicitlyEnabled_regardlessOfDefaultEnabled() {
    var providerFailClosed = new InMemoryFeatureFlagProvider(Map.of("flag-a", true), false);
    var providerFailOpen = new InMemoryFeatureFlagProvider(Map.of("flag-a", true), true);

    assertTrue(providerFailClosed.isFeatureEnabled("flag-a"));
    assertTrue(providerFailOpen.isFeatureEnabled("flag-a"));
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsExplicitlyDisabled_regardlessOfDefaultEnabled() {
    var providerFailClosed = new InMemoryFeatureFlagProvider(Map.of("flag-a", false), false);
    var providerFailOpen = new InMemoryFeatureFlagProvider(Map.of("flag-a", false), true);

    assertFalse(providerFailClosed.isFeatureEnabled("flag-a"));
    assertFalse(providerFailOpen.isFeatureEnabled("flag-a"));
  }
}
