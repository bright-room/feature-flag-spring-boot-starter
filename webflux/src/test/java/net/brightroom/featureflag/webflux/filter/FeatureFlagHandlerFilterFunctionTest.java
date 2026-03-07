package net.brightroom.featureflag.webflux.filter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeatureFlagHandlerFilterFunctionTest {

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final AccessDeniedHandlerFilterResolution resolution =
      mock(AccessDeniedHandlerFilterResolution.class);
  private final ReactiveFeatureFlagContextResolver contextResolver =
      mock(ReactiveFeatureFlagContextResolver.class);
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider =
      mock(ReactiveRolloutPercentageProvider.class);
  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator =
      mock(ReactiveFeatureFlagConditionEvaluator.class);
  private final ReactiveScheduleProvider reactiveScheduleProvider =
      mock(ReactiveScheduleProvider.class, invocation -> Mono.empty());
  private final FeatureFlagHandlerFilterFunction filterFunction =
      new FeatureFlagHandlerFilterFunction(
          provider,
          resolution,
          new DefaultReactiveRolloutStrategy(),
          contextResolver,
          rolloutPercentageProvider,
          conditionEvaluator,
          reactiveScheduleProvider,
          Clock.systemDefaultZone());

  // Filter function with mocked rollout strategy for rollout-specific tests
  private final ReactiveRolloutStrategy rolloutStrategy = mock(ReactiveRolloutStrategy.class);
  private final FeatureFlagHandlerFilterFunction filterFunctionWithRollout =
      new FeatureFlagHandlerFilterFunction(
          provider,
          resolution,
          rolloutStrategy,
          contextResolver,
          rolloutPercentageProvider,
          conditionEvaluator,
          reactiveScheduleProvider,
          Clock.systemDefaultZone());

  @Test
  void of_throwsIllegalArgumentException_whenFeatureNameIsNull() {
    assertThatThrownBy(() -> filterFunction.of(null))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null or blank");
  }

  @Test
  void of_throwsIllegalArgumentException_whenFeatureNameIsEmpty() {
    assertThatThrownBy(() -> filterFunction.of(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("null or blank");
  }

  @Test
  void of_throwsIllegalArgumentException_whenRolloutIsNegative() {
    assertThatThrownBy(() -> filterFunction.of("my-feature", -1))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void of_throwsIllegalArgumentException_whenRolloutIsOver100() {
    assertThatThrownBy(() -> filterFunction.of("my-feature", 101))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenFeatureEnabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());
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

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenRolloutCheckPasses() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(Mono.just(true));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenRolloutCheckFails() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(Mono.just(false));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  private void stubRequestForConditionVariables(ServerHttpRequest httpRequest) {
    when(httpRequest.getHeaders()).thenReturn(new HttpHeaders());
    when(httpRequest.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
    when(httpRequest.getCookies()).thenReturn(new LinkedMultiValueMap<>());
    org.springframework.http.server.RequestPath path =
        mock(org.springframework.http.server.RequestPath.class);
    when(path.value()).thenReturn("/functional/condition/header");
    when(httpRequest.getPath()).thenReturn(path);
    when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(httpRequest.getRemoteAddress()).thenReturn(null);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenConditionPasses() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any()))
        .thenReturn(Mono.just(true));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunction.of("my-feature", "headers['X-Beta'] != null");
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenConditionFails() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any()))
        .thenReturn(Mono.just(false));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunction.of("my-feature", "headers['X-Beta'] != null");
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_skipsConditionCheck_whenConditionIsEmpty() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());

    ServerRequest request = mock(ServerRequest.class);
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verifyNoInteractions(conditionEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_evaluatesConditionBeforeRollout() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any()))
        .thenReturn(Mono.just(false));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    // rollout = 50, but condition fails first — rollout check must not be reached
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", "headers['X-Beta'] != null", 50);
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(rolloutPercentageProvider);
    verifyNoInteractions(contextResolver);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenContextIsEmpty() {
    // fail-open: when context is not available, rollout check is skipped
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature")).thenReturn(Mono.empty());

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);

    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }
}
