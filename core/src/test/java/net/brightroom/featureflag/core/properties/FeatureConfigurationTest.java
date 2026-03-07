package net.brightroom.featureflag.core.properties;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class FeatureConfigurationTest {

  @ParameterizedTest
  @ValueSource(ints = {0, 1, 50, 99, 100})
  void setRollout_shouldAcceptValidValues(int rollout) {
    FeatureConfiguration config = new FeatureConfiguration();
    assertDoesNotThrow(() -> config.setRollout(rollout));
    assertEquals(rollout, config.rollout());
  }

  @ParameterizedTest
  @ValueSource(ints = {-1, -100, 101, 200})
  void setRollout_shouldRejectOutOfRangeValues(int rollout) {
    FeatureConfiguration config = new FeatureConfiguration();
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> config.setRollout(rollout));
    assertTrue(ex.getMessage().contains(String.valueOf(rollout)));
  }

  @Test
  void setRollout_errorMessageShouldContainInvalidValue() {
    FeatureConfiguration config = new FeatureConfiguration();
    IllegalArgumentException ex =
        assertThrows(IllegalArgumentException.class, () -> config.setRollout(101));
    assertEquals("rollout must be between 0 and 100, but was: 101", ex.getMessage());
  }
}
