package net.brightroom.featureflag.actuator.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import reactor.core.publisher.Mono;

@ExtendWith(MockitoExtension.class)
class ReactiveFeatureFlagEndpointTest {

  @Mock ApplicationEventPublisher eventPublisher;

  @Test
  void features_returnsAllFlagsAndDefaultEnabled() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features())
        .extracting(FeatureFlagEndpointResponse::featureName, FeatureFlagEndpointResponse::enabled)
        .containsExactlyInAnyOrder(tuple("feature-a", true), tuple("feature-b", false));
    assertFalse(response.defaultEnabled());
  }

  @Test
  void updateFeature_updatesExistingFlagAndReturnsUpdatedState() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false);

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("feature-a"))
        .extracting(FeatureFlagEndpointResponse::enabled)
        .containsExactly(false);
  }

  @Test
  void updateFeature_publishesFeatureFlagChangedEvent() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    endpoint.updateFeature("feature-a", false);

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("feature-a", captor.getValue().featureName());
    assertFalse(captor.getValue().enabled());
  }

  @Test
  void updateFeature_addsNewFlagNotPreviouslyDefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("new-flag", true);

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("new-flag"))
        .extracting(FeatureFlagEndpointResponse::enabled)
        .containsExactly(true);
  }

  @Test
  void features_returnsDefaultEnabled_true_whenConfigured() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, true, eventPublisher);

    var response = endpoint.features();

    assertTrue(response.defaultEnabled());
  }

  @Test
  void updateFeature_responseReflectsAllFlagsIncludingUnchanged() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", true), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false);

    assertEquals(2, response.features().size());
    assertThat(response.features())
        .extracting(FeatureFlagEndpointResponse::featureName, FeatureFlagEndpointResponse::enabled)
        .containsExactlyInAnyOrder(tuple("feature-a", false), tuple("feature-b", true));
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsNull() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature(null, true))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsEmpty() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("", true))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsBlank() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("   ", true))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void feature_returnsEnabledFlag() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals("feature-a", response.featureName());
    assertTrue(response.enabled());
  }

  @Test
  void feature_returnsDisabledFlag() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", false), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals("feature-a", response.featureName());
    assertFalse(response.enabled());
  }

  @Test
  void feature_returnsDefaultEnabled_whenFlagIsUndefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.feature("undefined-flag");

    assertEquals("undefined-flag", response.featureName());
    assertFalse(response.enabled());
  }

  @Test
  void feature_returnsDefaultEnabled_true_whenFlagIsUndefined_andDefaultEnabledIsTrue() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, true, eventPublisher);

    var response = endpoint.feature("undefined-flag");

    assertEquals("undefined-flag", response.featureName());
    assertTrue(response.enabled());
  }

  @Test
  void features_returnsEmptyList_whenProviderReturnsMonoEmpty() {
    var provider = org.mockito.Mockito.mock(MutableReactiveFeatureFlagProvider.class);
    when(provider.getFeatures()).thenReturn(Mono.empty());
    var endpoint = new ReactiveFeatureFlagEndpoint(provider, false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features()).isEmpty();
    assertFalse(response.defaultEnabled());
  }
}
