package net.brightroom.featureflag.webmvc.filter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.AccessDecision;
import net.brightroom.featureflag.core.evaluation.ConditionEvaluationStep;
import net.brightroom.featureflag.core.evaluation.EnabledEvaluationStep;
import net.brightroom.featureflag.core.evaluation.EvaluationStep;
import net.brightroom.featureflag.core.evaluation.FeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.evaluation.RolloutEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ScheduleEvaluationStep;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.function.HandlerFilterFunction;
import org.springframework.web.servlet.function.HandlerFunction;
import org.springframework.web.servlet.function.ServerRequest;
import org.springframework.web.servlet.function.ServerResponse;

class FeatureFlagHandlerFilterFunctionTest {

  private final FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
  private final AccessDeniedHandlerFilterResolution resolution =
      mock(AccessDeniedHandlerFilterResolution.class);
  private final FeatureFlagContextResolver contextResolver = mock(FeatureFlagContextResolver.class);
  private final RolloutPercentageProvider rolloutPercentageProvider =
      mock(RolloutPercentageProvider.class);
  private final FeatureFlagConditionEvaluator conditionEvaluator =
      mock(FeatureFlagConditionEvaluator.class);
  private final ScheduleProvider scheduleProvider =
      mock(ScheduleProvider.class, invocation -> Optional.empty());
  private final RolloutStrategy rolloutStrategy = mock(RolloutStrategy.class);

  private FeatureFlagHandlerFilterFunction buildFilterFunction(RolloutStrategy strategy) {
    List<EvaluationStep> steps =
        List.of(
            new EnabledEvaluationStep(provider),
            new ScheduleEvaluationStep(scheduleProvider, Clock.systemDefaultZone()),
            new ConditionEvaluationStep(conditionEvaluator),
            new RolloutEvaluationStep(strategy));
    FeatureFlagEvaluationPipeline pipeline = new FeatureFlagEvaluationPipeline(steps);
    return new FeatureFlagHandlerFilterFunction(
        pipeline, resolution, rolloutPercentageProvider, contextResolver);
  }

  private final FeatureFlagHandlerFilterFunction filterFunction =
      buildFilterFunction(new DefaultRolloutStrategy());
  private final FeatureFlagHandlerFilterFunction filterFunctionWithRollout =
      buildFilterFunction(rolloutStrategy);

  // --- checkSchedule ---

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenScheduleIsInactive() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(inactiveSchedule));

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpServletRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(deniedResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(deniedResponse);
    verifyNoInteractions(next);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenScheduleIsActive() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(activeSchedule));

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpServletRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(next).handle(request);
  }

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

  private void stubServletRequest(ServerRequest serverRequest, HttpServletRequest httpRequest) {
    when(serverRequest.servletRequest()).thenReturn(httpRequest);
    stubServletRequestForConditionVariables(httpRequest);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenFeatureEnabled() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpRequest);
    when(contextResolver.resolve(httpRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenFeatureDisabled() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(false);

    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpRequest);
    when(contextResolver.resolve(httpRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(deniedResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(deniedResponse);
    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenRolloutCheckPasses() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpServletRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(true);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenRolloutCheckFails() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpServletRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(false);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(deniedResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(deniedResponse);
    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenContextIsEmpty() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_usesProviderRollout_whenProviderReturnsValue() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(70));

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpServletRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 70)).thenReturn(true);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 50);
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(rolloutStrategy).isInRollout("my-feature", context, 70);
    verify(next).handle(request);
  }

  private void stubServletRequestForConditionVariables(HttpServletRequest httpServletRequest) {
    when(httpServletRequest.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(httpServletRequest.getParameterMap()).thenReturn(Map.of());
    when(httpServletRequest.getCookies()).thenReturn(null);
    when(httpServletRequest.getRequestURI()).thenReturn("/functional/condition/header");
    when(httpServletRequest.getMethod()).thenReturn("GET");
    when(httpServletRequest.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToNext_whenConditionPasses() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpServletRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(true);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunction.of("my-feature", "headers['X-Beta'] != null");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(next).handle(request);
    verifyNoInteractions(resolution);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToResolution_whenConditionFails() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpServletRequest);

    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    when(conditionEvaluator.evaluate(eq("headers['X-Beta'] != null"), any())).thenReturn(false);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(deniedResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunction.of("my-feature", "headers['X-Beta'] != null");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(deniedResponse);
    verifyNoInteractions(next);
    verify(resolution).resolve(eq(request), any(FeatureFlagAccessDeniedException.class));
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_skipsConditionCheck_whenConditionIsEmpty() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpServletRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.empty());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFunction.of("my-feature");
    filter.filter(request, next);

    verifyNoInteractions(conditionEvaluator);
  }

  @Test
  @SuppressWarnings("unchecked")
  void of_usesFallbackRollout_whenProviderReturnsEmpty() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    stubServletRequest(request, httpServletRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpServletRequest)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 30)).thenReturn(false);

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse deniedResponse = mock(ServerResponse.class);
    when(resolution.resolve(eq(request), any(FeatureFlagAccessDeniedException.class)))
        .thenReturn(deniedResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter =
        filterFunctionWithRollout.of("my-feature", 30);
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(deniedResponse);
    verify(rolloutStrategy).isInRollout("my-feature", context, 30);
    verifyNoInteractions(next);
  }

  // --- pipeline delegation ---

  @Test
  @SuppressWarnings("unchecked")
  void of_delegatesToPipeline() throws Exception {
    FeatureFlagEvaluationPipeline pipeline = mock(FeatureFlagEvaluationPipeline.class);
    FeatureFlagHandlerFilterFunction filterFn =
        new FeatureFlagHandlerFilterFunction(
            pipeline, resolution, rolloutPercentageProvider, contextResolver);

    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    HttpServletRequest httpRequest = mock(HttpServletRequest.class);
    stubServletRequestForConditionVariables(httpRequest);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpRequest);
    when(contextResolver.resolve(httpRequest)).thenReturn(Optional.empty());
    when(pipeline.evaluate(any())).thenReturn(AccessDecision.allowed());

    HandlerFunction<ServerResponse> next = mock(HandlerFunction.class);
    ServerResponse okResponse = mock(ServerResponse.class);
    when(next.handle(request)).thenReturn(okResponse);

    HandlerFilterFunction<ServerResponse, ServerResponse> filter = filterFn.of("my-feature");
    ServerResponse result = filter.filter(request, next);

    assertThat(result).isEqualTo(okResponse);
    verify(pipeline).evaluate(any());
  }
}
