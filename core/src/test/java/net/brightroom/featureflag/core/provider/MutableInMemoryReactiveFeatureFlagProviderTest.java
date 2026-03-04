package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

class MutableInMemoryReactiveFeatureFlagProviderTest {

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsEnabled() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);

    assertTrue(provider.isFeatureEnabled("feature-a").block());
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsDisabled() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", false), true);

    assertFalse(provider.isFeatureEnabled("feature-a").block());
  }

  @Test
  void isFeatureEnabled_returnsDefaultEnabled_false_whenFlagIsUndefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);

    assertFalse(provider.isFeatureEnabled("undefined-flag").block());
  }

  @Test
  void isFeatureEnabled_returnsDefaultEnabled_true_whenFlagIsUndefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);

    assertTrue(provider.isFeatureEnabled("undefined-flag").block());
  }

  @Test
  void getFeatures_returnsSnapshotOfAllFlags() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);

    assertEquals(Map.of("feature-a", true, "feature-b", false), provider.getFeatures().block());
  }

  @Test
  void getFeatures_returnsImmutableCopy_notAffectedBySubsequentMutations() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var snapshot = provider.getFeatures().block();

    provider.setFeatureEnabled("feature-a", false).block();

    assertTrue(snapshot.get("feature-a"), "snapshot must not reflect subsequent mutations");
  }

  @Test
  void setFeatureEnabled_updatesExistingFlag() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);

    provider.setFeatureEnabled("feature-a", false).block();

    assertFalse(provider.isFeatureEnabled("feature-a").block());
  }

  @Test
  void setFeatureEnabled_addsNewFlag() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);

    provider.setFeatureEnabled("new-flag", true).block();

    assertTrue(provider.isFeatureEnabled("new-flag").block());
    assertEquals(Map.of("new-flag", true), provider.getFeatures().block());
  }

  @Test
  void concurrentWrites_doNotCorruptState() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    int threadCount = 100;
    List<String> flags = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      flags.add("flag-" + i);
    }

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String flag : flags) {
        executor.submit(() -> provider.setFeatureEnabled(flag, true).block());
      }
    }

    var features = provider.getFeatures().block();
    assertEquals(threadCount, features.size());
    flags.forEach(flag -> assertTrue(provider.isFeatureEnabled(flag).block()));
  }

  @Test
  void removeFeature_removesExistingFlag_andFallsBackToDefaultEnabled() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);

    provider.removeFeature("feature-a").block();

    assertFalse(provider.isFeatureEnabled("feature-a").block());
    assertTrue(provider.getFeatures().block().isEmpty());
  }

  @Test
  void removeFeature_fallsBackToDefaultEnabled_true_whenDefaultEnabledIsTrue() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", false), true);

    provider.removeFeature("feature-a").block();

    assertTrue(provider.isFeatureEnabled("feature-a").block());
  }

  @Test
  void removeFeature_isNoOp_whenFlagDoesNotExist() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);

    provider.removeFeature("nonexistent").block();

    assertEquals(Map.of("feature-a", true), provider.getFeatures().block());
  }
}
