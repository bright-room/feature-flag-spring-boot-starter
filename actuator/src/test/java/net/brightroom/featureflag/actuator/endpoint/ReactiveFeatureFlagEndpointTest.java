package net.brightroom.featureflag.actuator.endpoint;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.groups.Tuple.tuple;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import net.brightroom.featureflag.core.event.FeatureFlagChangedEvent;
import net.brightroom.featureflag.core.event.FeatureFlagRemovedEvent;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveRolloutPercentageProvider;
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

  private MutableInMemoryReactiveRolloutPercentageProvider emptyRolloutProvider() {
    return new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());
  }

  @Test
  void features_returnsAllFlagsAndDefaultEnabled() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features())
        .extracting(FeatureFlagEndpointResponse::featureName, FeatureFlagEndpointResponse::enabled)
        .containsExactlyInAnyOrder(tuple("feature-a", true), tuple("feature-b", false));
    assertFalse(response.defaultEnabled());
  }

  @Test
  void updateFeature_updatesExistingFlagAndReturnsUpdatedState() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false, null);

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("feature-a"))
        .extracting(FeatureFlagEndpointResponse::enabled)
        .containsExactly(false);
  }

  @Test
  void updateFeature_publishesFeatureFlagChangedEvent() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.updateFeature("feature-a", false, null);

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("feature-a", captor.getValue().featureName());
    assertFalse(captor.getValue().enabled());
  }

  @Test
  void updateFeature_addsNewFlagNotPreviouslyDefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.updateFeature("new-flag", true, null);

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("new-flag"))
        .extracting(FeatureFlagEndpointResponse::enabled)
        .containsExactly(true);
  }

  @Test
  void features_returnsDefaultEnabled_true_whenConfigured() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), true, eventPublisher);

    var response = endpoint.features();

    assertTrue(response.defaultEnabled());
  }

  @Test
  void updateFeature_responseReflectsAllFlagsIncludingUnchanged() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", false, null);

    assertEquals(2, response.features().size());
    assertThat(response.features())
        .extracting(FeatureFlagEndpointResponse::featureName, FeatureFlagEndpointResponse::enabled)
        .containsExactlyInAnyOrder(tuple("feature-a", false), tuple("feature-b", true));
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsNull() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature(null, true, null))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsEmpty() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("", true, null))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenFeatureNameIsBlank() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("   ", true, null))
        .withMessageContaining("featureName must not be null or blank");
  }

  @Test
  void feature_returnsEnabledFlag() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals("feature-a", response.featureName());
    assertTrue(response.enabled());
  }

  @Test
  void feature_returnsDisabledFlag() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", false), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals("feature-a", response.featureName());
    assertFalse(response.enabled());
  }

  @Test
  void feature_returnsDefaultEnabled_whenFlagIsUndefined() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.feature("undefined-flag");

    assertEquals("undefined-flag", response.featureName());
    assertFalse(response.enabled());
  }

  @Test
  void feature_returnsDefaultEnabled_true_whenFlagIsUndefined_andDefaultEnabledIsTrue() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), true);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), true, eventPublisher);

    var response = endpoint.feature("undefined-flag");

    assertEquals("undefined-flag", response.featureName());
    assertTrue(response.enabled());
  }

  @Test
  void features_returnsEmptyList_whenProviderReturnsMonoEmpty() {
    var provider = mock(MutableReactiveFeatureFlagProvider.class);
    when(provider.getFeatures()).thenReturn(Mono.empty());
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features()).isEmpty();
    assertFalse(response.defaultEnabled());
  }

  @Test
  void features_returnsRolloutPercentagesFromProvider() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var rolloutProvider =
        new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, rolloutProvider, false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("feature-a"))
        .extracting(FeatureFlagEndpointResponse::rollout)
        .containsExactly(50);
  }

  @Test
  void features_returnsDefaultRollout100_whenNotConfigured() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.features();

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("feature-a"))
        .extracting(FeatureFlagEndpointResponse::rollout)
        .containsExactly(100);
  }

  @Test
  void feature_returnsRolloutPercentageFromProvider() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var rolloutProvider =
        new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 75));
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, rolloutProvider, false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals(75, response.rollout());
  }

  @Test
  void feature_returnsDefaultRollout100_whenNotConfigured() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    var response = endpoint.feature("feature-a");

    assertEquals(100, response.rollout());
  }

  @Test
  void updateFeature_updatesRolloutPercentage() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var rolloutProvider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, rolloutProvider, false, eventPublisher);

    var response = endpoint.updateFeature("feature-a", true, 50);

    assertThat(response.features())
        .filteredOn(f -> f.featureName().equals("feature-a"))
        .extracting(FeatureFlagEndpointResponse::rollout)
        .containsExactly(50);
  }

  @Test
  void updateFeature_publishesEventWithRolloutPercentage() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.updateFeature("feature-a", true, 60);

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertTrue(captor.getValue().enabled());
    assertEquals(60, captor.getValue().rolloutPercentage());
  }

  @Test
  void updateFeature_publishesEventWithNullRollout_whenRolloutNotSpecified() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.updateFeature("feature-a", true, null);

    var captor = ArgumentCaptor.forClass(FeatureFlagChangedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertNull(captor.getValue().rolloutPercentage());
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenRolloutIsNegative() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("feature-a", true, -1))
        .withMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void updateFeature_throwsIllegalArgumentException_whenRolloutExceeds100() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatIllegalArgumentException()
        .isThrownBy(() -> endpoint.updateFeature("feature-a", true, 101))
        .withMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void updateFeature_acceptsBoundaryRolloutValues() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var rolloutProvider = new MutableInMemoryReactiveRolloutPercentageProvider(Map.of());
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, rolloutProvider, false, eventPublisher);

    assertThatNoException().isThrownBy(() -> endpoint.updateFeature("feature-a", true, 0));
    assertThatNoException().isThrownBy(() -> endpoint.updateFeature("feature-a", true, 100));
  }

  @Test
  void deleteFeature_removesFlagFromProvider() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.deleteFeature("feature-a");

    assertFalse(provider.isFeatureEnabled("feature-a").block());
    assertTrue(provider.getFeatures().block().isEmpty());
  }

  @Test
  void deleteFeature_publishesFeatureFlagRemovedEvent() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.deleteFeature("feature-a");

    var captor = ArgumentCaptor.forClass(FeatureFlagRemovedEvent.class);
    verify(eventPublisher).publishEvent(captor.capture());
    assertEquals("feature-a", captor.getValue().featureName());
  }

  @Test
  void deleteFeature_removesRolloutPercentage() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of("feature-a", true), false);
    var rolloutProvider =
        new MutableInMemoryReactiveRolloutPercentageProvider(Map.of("feature-a", 50));
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, rolloutProvider, false, eventPublisher);

    endpoint.deleteFeature("feature-a");

    assertTrue(rolloutProvider.getRolloutPercentages().block().isEmpty());
  }

  @Test
  void deleteFeature_thenFeatures_excludesDeletedFlag() {
    var provider =
        new MutableInMemoryReactiveFeatureFlagProvider(
            Map.of("feature-a", true, "feature-b", false), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    endpoint.deleteFeature("feature-a");

    var response = endpoint.features();
    assertThat(response.features())
        .extracting(FeatureFlagEndpointResponse::featureName)
        .containsExactly("feature-b");
  }

  @Test
  void deleteFeature_isIdempotent_whenFlagDoesNotExist() {
    var provider = new MutableInMemoryReactiveFeatureFlagProvider(Map.of(), false);
    var endpoint =
        new ReactiveFeatureFlagEndpoint(provider, emptyRolloutProvider(), false, eventPublisher);

    assertThatNoException().isThrownBy(() -> endpoint.deleteFeature("nonexistent"));
  }
}
