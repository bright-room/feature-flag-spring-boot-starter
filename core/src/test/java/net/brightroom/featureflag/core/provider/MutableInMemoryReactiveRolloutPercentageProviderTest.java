package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MutableInMemoryReactiveRolloutPercentageProviderTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 50, 100})
  void setRolloutPercentage_storesPercentage_whenValueIsValid(int percentage) {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());

    provider.setRolloutPercentage("feature-a", percentage).block();

    assertEquals(percentage, provider.getRolloutPercentage("feature-a").block());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 101})
  void setRolloutPercentage_throwsIllegalArgumentException_whenValueIsOutOfRange(int percentage) {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());

    assertThrows(
        IllegalArgumentException.class,
        () -> provider.setRolloutPercentage("feature-a", percentage));
  }

  @Test
  void getRolloutPercentage_returnsPercentage_whenExists() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    assertEquals(50, provider.getRolloutPercentage("feature-a").block());
  }

  @Test
  void getRolloutPercentage_returnsEmpty_whenNotExists() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());

    assertNull(provider.getRolloutPercentage("nonexistent").block());
  }

  @Test
  void setRolloutPercentage_addsNewEntry() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());

    provider.setRolloutPercentage("feature-a", 75).block();

    assertEquals(75, provider.getRolloutPercentage("feature-a").block());
    assertEquals(Map.of("feature-a", 75), provider.getRolloutPercentages().block());
  }

  @Test
  void setRolloutPercentage_updatesExistingEntry() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    provider.setRolloutPercentage("feature-a", 80).block();

    assertEquals(80, provider.getRolloutPercentage("feature-a").block());
  }

  @Test
  void getRolloutPercentages_returnsSnapshotOfAllPercentages() {
    var provider =
        new MutableInMemoryReactiveRolloutPercentageProvider(
            Map.of("feature-a", 50, "feature-b", 100));

    var percentages = provider.getRolloutPercentages().block();

    assertEquals(Map.of("feature-a", 50, "feature-b", 100), percentages);
  }

  @Test
  void getRolloutPercentages_returnsImmutableCopy_notAffectedBySubsequentMutations() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));
    var snapshot = provider.getRolloutPercentages().block();

    provider.setRolloutPercentage("feature-a", 80).block();

    assertEquals(50, snapshot.get("feature-a"), "snapshot must not reflect subsequent mutations");
  }

  @Test
  void concurrentSetRolloutPercentage_doesNotCorruptState() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());
    int threadCount = 100;
    List<String> features = new ArrayList<>();
    for (int i = 0; i < threadCount; i++) {
      features.add("feature-" + i);
    }

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (String feature : features) {
        executor.submit(() -> provider.setRolloutPercentage(feature, 50).block());
      }
    }

    assertEquals(threadCount, provider.getRolloutPercentages().block().size());
    features.forEach(feature -> assertEquals(50, provider.getRolloutPercentage(feature).block()));
  }

  @Test
  void removeRolloutPercentage_removesExistingEntry() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    Boolean removed = provider.removeRolloutPercentage("feature-a").block();

    assertTrue(removed);
    assertNull(provider.getRolloutPercentage("feature-a").block());
    assertTrue(provider.getRolloutPercentages().block().isEmpty());
  }

  @Test
  void removeRolloutPercentage_isNoOp_whenEntryDoesNotExist() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    Boolean removed = provider.removeRolloutPercentage("nonexistent").block();

    assertFalse(removed);
    assertEquals(50, provider.getRolloutPercentage("feature-a").block());
    assertEquals(Map.of("feature-a", 50), provider.getRolloutPercentages().block());
  }
}
