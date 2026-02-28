package net.brightroom.featureflag.webflux.provider;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class InMemoryReactiveFeatureFlagProviderDefaultEnabledTest {

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsUndefined_andDefaultEnabledIsFalse() {
    var provider = new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", true), false);
    assertFalse(provider.isFeatureEnabled("undefined-flag").block());
  }

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsUndefined_andDefaultEnabledIsTrue() {
    var provider = new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", true), true);
    assertTrue(provider.isFeatureEnabled("undefined-flag").block());
  }

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsExplicitlyEnabled_regardlessOfDefaultEnabled() {
    var providerFailClosed = new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", true), false);
    var providerFailOpen = new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", true), true);

    assertTrue(providerFailClosed.isFeatureEnabled("flag-a").block());
    assertTrue(providerFailOpen.isFeatureEnabled("flag-a").block());
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsExplicitlyDisabled_regardlessOfDefaultEnabled() {
    var providerFailClosed =
        new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", false), false);
    var providerFailOpen = new InMemoryReactiveFeatureFlagProvider(Map.of("flag-a", false), true);

    assertFalse(providerFailClosed.isFeatureEnabled("flag-a").block());
    assertFalse(providerFailOpen.isFeatureEnabled("flag-a").block());
  }
}
