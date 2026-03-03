package net.brightroom.featureflag.actuator.endpoint;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class FeatureFlagEndpointTest {

  @Mock ApplicationEventPublisher eventPublisher;

  @Test
  void features_returnsAllFlagsAndDefaultEnabled() {
    var provider =
        new MutableInMemoryFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.features();

    assertEquals(Map.of("feature-a", true, "feature-b", false), response.features());
    assertFalse(response.defaultEnabled());
  }

  @Test
  void updateFeature_updatesExistingFlagAndReturnsUpdatedState() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false);

    assertFalse(response.features().get("feature-a"));
  }

  @Test
  void updateFeature_publishesFeatureFlagChangedEvent() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    endpoint.updateFeature("feature-a", false);

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("feature-a", captor.getValue().featureName());
    assertFalse(captor.getValue().enabled());
  }

  @Test
  void updateFeature_addsNewFlagNotPreviouslyDefined() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("new-flag", true);

    assertTrue(response.features().get("new-flag"));
  }

  @Test
  void features_returnsDefaultEnabled_true_whenConfigured() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), true);
    var endpoint = new FeatureFlagEndpoint(provider, true, eventPublisher);

    var response = endpoint.features();

    assertTrue(response.defaultEnabled());
  }

  @Test
  void updateFeature_responseReflectsAllFlagsIncludingUnchanged() {
    var provider =
        new MutableInMemoryFeatureFlagProvider(Map.of("feature-a", true, "feature-b", true), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false);

    List<String> keys = new ArrayList<>(response.features().keySet());
    assertEquals(2, keys.size());
    assertFalse(response.features().get("feature-a"));
    assertTrue(response.features().get("feature-b"));
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsNull() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature(null, true))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsEmpty() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("", true))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsBlank() {
    var provider = new MutableInMemoryFeatureFlagProvider(Map.of(), false);
    var endpoint = new FeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("   ", true))
        .withMessageContaining("featureName must not be null or blank");
  }
}
