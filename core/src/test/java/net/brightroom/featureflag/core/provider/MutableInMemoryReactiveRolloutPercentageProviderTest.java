package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import org.junit.jupiter.api.Test;

class MutableInMemoryReactiveRolloutPercentageProviderTest {

  @Test
  void removeRolloutPercentage_removesExistingEntry() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    provider.removeRolloutPercentage("feature-a");

    assertNull(provider.getRolloutPercentage("feature-a").block());
    assertTrue(provider.getRolloutPercentages().block().isEmpty());
  }

  @Test
  void removeRolloutPercentage_isNoOp_whenEntryDoesNotExist() {
    var provider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));

    provider.removeRolloutPercentage("nonexistent");

    assertEquals(50, provider.getRolloutPercentage("feature-a").block());
    assertEquals(Map.of("feature-a", 50), provider.getRolloutPercentages().block());
  }
}
