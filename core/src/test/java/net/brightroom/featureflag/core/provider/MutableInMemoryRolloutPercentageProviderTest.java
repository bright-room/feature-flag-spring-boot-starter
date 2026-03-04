package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

class MutableInMemoryRolloutPercentageProviderTest {

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
