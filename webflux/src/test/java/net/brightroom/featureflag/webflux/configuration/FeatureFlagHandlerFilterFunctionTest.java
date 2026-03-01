package net.brightroom.featureflag.webflux.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeatureFlagHandlerFilterFunctionTest {

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final AccessDeniedHandlerFilterResolution resolution =
      mock(AccessDeniedHandlerFilterResolution.class);
  private final FeatureFlagHandlerFilterFunction filterFunction =
      new FeatureFlagHandlerFilterFunction(provider, resolution);

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

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenFeatureEnabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerRequest request = mock(ServerRequest.class);
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenFeatureDisabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(false));
    ServerRequest request = mock(ServerRequest.class);
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }
}
