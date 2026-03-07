package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MutableInMemoryRolloutPercentageProviderTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 50, 100})
  void setRolloutPercentage_storesPercentage_whenValueIsValid(int percentage) {
    var provider = new MutableInMemoryRolloutPercentageProvider(Map.of());

    provider.setRolloutPercentage("feature-a", percentage);

    assertEquals(OptionalInt.of(percentage), provider.getRolloutPercentage("feature-a"));
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, 101})
  void setRolloutPercentage_throwsIllegalArgumentException_whenValueIsOutOfRange(int percentage) {
    var provider = new MutableInMemoryRolloutPercentageProvider(Map.of());

    assertThrows(
        IllegalArgumentException.class,
        () -> provider.setRolloutPercentage("feature-a", percentage));
  }

  @Test
  void removeRolloutPercentage_removesExistingEntry() {
    var provider = new MutableInMemoryRolloutPercentageProvider(Map.of("feature-a", 50));

    provider.removeRolloutPercentage("feature-a");

    assertEquals(OptionalInt.empty(), provider.getRolloutPercentage("feature-a"));
    assertTrue(provider.getRolloutPercentages().isEmpty());
  }

  @Test
  void removeRolloutPercentage_isNoOp_whenEntryDoesNotExist() {
    var provider = new MutableInMemoryRolloutPercentageProvider(Map.of("feature-a", 50));

    provider.removeRolloutPercentage("nonexistent");

    assertEquals(OptionalInt.of(50), provider.getRolloutPercentage("feature-a"));
    assertEquals(Map.of("feature-a", 50), provider.getRolloutPercentages());
  }
}
