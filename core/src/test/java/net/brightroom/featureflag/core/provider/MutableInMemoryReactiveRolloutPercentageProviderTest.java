package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class MutableInMemoryReactiveRolloutPercentageProviderTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 50, 100})
  void setRolloutPercentage_storesPercentage_whenValueIsValid(int percentage) {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());

    provider.setRolloutPercentage("feature-a", percentage);

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
