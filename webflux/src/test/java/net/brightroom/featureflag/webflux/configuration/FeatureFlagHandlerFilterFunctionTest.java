package net.brightroom.featureflag.webflux.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Test;

class FeatureFlagHandlerFilterFunctionTest {

  private final FeatureFlagHandlerFilterFunction filterFunction =
      new FeatureFlagHandlerFilterFunction(
          mock(ReactiveFeatureFlagProvider.class), mock(AccessDeniedHandlerFilterResolution.class));

  @Test
  void of_throwsIllegalArgumentException_whenFeatureNameIsNull() {
    assertThatThrownBy(() -> filterFunction.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null or empty");
  }

  @Test
  void of_throwsIllegalArgumentException_whenFeatureNameIsEmpty() {
    assertThatThrownBy(() -> filterFunction.of(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null or empty");
  }
}
