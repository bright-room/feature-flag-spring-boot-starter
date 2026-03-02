package net.brightroom.featureflag.core.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class InMemoryMutableFeatureFlagProviderTest {

  @Mock ApplicationEventPublisher eventPublisher;

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsExplicitlyEnabled() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", true), false, eventPublisher);

    assertTrue(provider.isFeatureEnabled("flag-a"));
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsExplicitlyDisabled() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", false), false, eventPublisher);

    assertFalse(provider.isFeatureEnabled("flag-a"));
  }

  @Test
  void isFeatureEnabled_returnsFalse_whenFlagIsUndefined_andDefaultEnabledIsFalse() {
    var provider = new InMemoryMutableFeatureFlagProvider(Map.of(), false, eventPublisher);

    assertFalse(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void isFeatureEnabled_returnsTrue_whenFlagIsUndefined_andDefaultEnabledIsTrue() {
    var provider = new InMemoryMutableFeatureFlagProvider(Map.of(), true, eventPublisher);

    assertTrue(provider.isFeatureEnabled("undefined-flag"));
  }

  @Test
  void enable_updatesFeatureStateToTrue() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", false), false, eventPublisher);

    provider.enable("flag-a");

    assertTrue(provider.isFeatureEnabled("flag-a"));
  }

  @Test
  void enable_publishesChangedEventWithEnabledTrue() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", false), false, eventPublisher);

    provider.enable("flag-a");

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("flag-a", captor.getValue().featureName());
    assertTrue(captor.getValue().enabled());
  }

  @Test
  void disable_updatesFeatureStateToFalse() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", true), false, eventPublisher);

    provider.disable("flag-a");

    assertFalse(provider.isFeatureEnabled("flag-a"));
  }

  @Test
  void disable_publishesChangedEventWithEnabledFalse() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", true), false, eventPublisher);

    provider.disable("flag-a");

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("flag-a", captor.getValue().featureName());
    assertFalse(captor.getValue().enabled());
  }

  @Test
  void getAllFeatures_returnsAllConfiguredFeatures() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(
            Map.of("flag-a", true, "flag-b", false), false, eventPublisher);

    var features = provider.getAllFeatures();

    assertEquals(Map.of("flag-a", true, "flag-b", false), features);
  }

  @Test
  void getAllFeatures_reflectsRuntimeChanges() {
    var provider =
        new InMemoryMutableFeatureFlagProvider(Map.of("flag-a", true), false, eventPublisher);

    provider.disable("flag-a");

    assertEquals(Map.of("flag-a", false), provider.getAllFeatures());
  }
}
