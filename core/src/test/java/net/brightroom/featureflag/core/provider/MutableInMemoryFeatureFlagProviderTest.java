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

class MutableInMemoryFeatureFlagProviderTest {

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsEnabled() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true), false);

    assertTrue(provider.isFeatureEnabled("feature-a"));
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsDisabled() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", false), true);

    assertFalse(provider.isFeatureEnabled("feature-a"));
  }

  @Test
  void isFeatureEnabled_returnsDefaultEnabled_false_whenFlagIsUndefined() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);

    assertFalse(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void isFeatureEnabled_returnsDefaultEnabled_true_whenFlagIsUndefined() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), true);

    assertTrue(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void getFeatures_returnsSnapshotOfAllFlags() {
    var provider =
        new MutableInMemoryFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);

    var features = provider.getFeatures();

    assertEquals(Map.of("feature-a", true, "feature-b", false), features);
  }

  @Test
  void getFeatures_returnsImmutableCopy_notAffectedBySubsequentMutations() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true), false);
    var snapshot = provider.getFeatures();

    provider.setFeatureEnabled("feature-a", false);

    assertTrue(snapshot.get("feature-a"), "snapshot must not reflect subsequent mutations");
  }

  @Test
  void setFeatureEnabled_updatesExistingFlag() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true), false);

    provider.setFeatureEnabled("feature-a", false);

    assertFalse(provider.isFeatureEnabled("feature-a"));
  }

  @Test
  void setFeatureEnabled_addsNewFlag() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);

    provider.setFeatureEnabled("new-flag", true);

    assertTrue(provider.isFeatureEnabled("new-flag"));
    assertEquals(Map.of("new-flag", true), provider.getFeatures());
  }

  @Test
  void concurrentWrites_doNotCorruptState() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    int threadCount = 100;
    List<String> flags = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      flags.add("flag-" + i);
    }

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String flag : flags) {
        executor.submit(() -> provider.setFeatureEnabled(flag, true));
      }
    }

    assertEquals(threadCount, provider.getFeatures().size());
    flags.forEach(flag -> assertTrue(provider.isFeatureEnabled(flag)));
  }
}
