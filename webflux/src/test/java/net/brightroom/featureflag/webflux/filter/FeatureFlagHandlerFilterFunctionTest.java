package net.brightroom.featureflag.webflux.filter;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.ReactiveConditionEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveEnabledEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveFeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.evaluation.ReactiveRolloutEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveScheduleEvaluationStep;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
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
      mock(ReactiveFeatureFlagContextResolver.class, invocation -> Mono.empty());
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider =
      mock(ReactiveRolloutPercentageProvider.class, invocation -> Mono.empty());
  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator =
      mock(ReactiveFeatureFlagConditionEvaluator.class);
  private final ReactiveScheduleProvider reactiveScheduleProvider =
      mock(ReactiveScheduleProvider.class, invocation -> Mono.empty());
  private final ReactiveRolloutStrategy rolloutStrategy = mock(ReactiveRolloutStrategy.class);

  private ReactiveFeatureFlagEvaluationPipeline buildPipeline(ReactiveRolloutStrategy strategy) {
    return new ReactiveFeatureFlagEvaluationPipeline(
        List.of(
            new ReactiveEnabledEvaluationStep(provider),
            new ReactiveScheduleEvaluationStep(reactiveScheduleProvider, Clock.systemDefaultZone()),
            new ReactiveConditionEvaluationStep(conditionEvaluator),
            new ReactiveRolloutEvaluationStep(strategy)));
  }

  private final FeatureFlagHandlerFilterFunction filterFunction =
      new FeatureFlagHandlerFilterFunction(
          buildPipeline(new DefaultReactiveRolloutStrategy()),
          resolution,
          rolloutPercentageProvider,
          contextResolver);

  private final FeatureFlagHandlerFilterFunction filterFunctionWithRollout =
      new FeatureFlagHandlerFilterFunction(
          buildPipeline(rolloutStrategy), resolution, rolloutPercentageProvider, contextResolver);

  private ServerRequest mockRequest() {
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(httpRequest.getHeaders()).thenReturn(new HttpHeaders());
    when(httpRequest.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
    when(httpRequest.getCookies()).thenReturn(new LinkedMultiValueMap<>());
    org.springframework.http.server.RequestPath path =
        mock(org.springframework.http.server.RequestPath.class);
    when(path.value()).thenReturn("/test");
    when(httpRequest.getPath()).thenReturn(path);
    when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(httpRequest.getRemoteAddress()).thenReturn(null);
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);
    return request;
  }

  private ServerRequest mockRequest(ServerHttpRequest httpRequest) {
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.exchange()).thenReturn(exchange);
    return request;
  }

  private ServerHttpRequest mockHttpRequest() {
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(httpRequest.getHeaders()).thenReturn(new HttpHeaders());
    when(httpRequest.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
    when(httpRequest.getCookies()).thenReturn(new LinkedMultiValueMap<>());
    org.springframework.http.server.RequestPath path =
        mock(org.springframework.http.server.RequestPath.class);
    when(path.value()).thenReturn("/functional/condition/header");
    when(httpRequest.getPath()).thenReturn(path);
    when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(httpRequest.getRemoteAddress()).thenReturn(null);
    return httpRequest;
  }

  // --- validation ---

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

  // --- enabled check ---

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenFeatureEnabled() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerRequest request = mockRequest();
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
    ServerRequest request = mockRequest();
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  // --- schedule check ---

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenScheduleIsInactive() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    // end in the past → inactive
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(reactiveScheduleProvider.getSchedule("my-feature"))
        .thenReturn(Mono.just(inactiveSchedule));

    ServerRequest request = mockRequest();
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(next);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenScheduleIsActive() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    // start in the past, no end → active
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(reactiveScheduleProvider.getSchedule("my-feature")).thenReturn(Mono.just(activeSchedule));

    ServerRequest request = mockRequest();
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  // --- condition check ---

  @Test
  @SuppressWarnings("unchecked")
  void of_skipsConditionCheck_whenConditionIsEmpty() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerRequest request = mockRequest();
    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(Mono.just(okResponse));

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    StepVerifier.create(filter.filter(request, next)).expectNext(okResponse).verifyComplete();

    verifyNoInteractions(conditionEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenConditionPasses() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerHttpRequest httpRequest = mockHttpRequest();
    ServerRequest request = mockRequest(httpRequest);

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
    ServerHttpRequest httpRequest = mockHttpRequest();
    ServerRequest request = mockRequest(httpRequest);

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
  void of_evaluatesConditionBeforeRollout() {
    // condition fails at step @Order(300); rollout step @Order(400) is never reached
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerHttpRequest httpRequest = mockHttpRequest();
    ServerRequest request = mockRequest(httpRequest);

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any()))
        .thenReturn(Mono.just(false));

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(Mono.just(deniedResponse));

    // rollout = 50, but condition fails first
    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", "headers['X-Beta'] != null", 50);
    StepVerifier.create(filter.filter(request, next)).expectNext(deniedResponse).verifyComplete();

    verifyNoInteractions(rolloutStrategy);
  }

  // --- rollout check ---

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenRolloutCheckPasses() {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerHttpRequest httpRequest = mockHttpRequest();
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(new FeatureFlagContext("u1")));
    ServerRequest request = mockRequest(httpRequest);

    FeatureFlagContext context = new FeatureFlagContext("u1");
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
    ServerHttpRequest httpRequest = mockHttpRequest();
    FeatureFlagContext context = new FeatureFlagContext("u1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    ServerRequest request = mockRequest(httpRequest);

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

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenContextIsEmpty() {
    // fail-open: when context is not available, rollout check is skipped
    when(provider.isFeatureEnabled("my-feature")).thenReturn(Mono.just(true));
    ServerRequest request = mockRequest();

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
