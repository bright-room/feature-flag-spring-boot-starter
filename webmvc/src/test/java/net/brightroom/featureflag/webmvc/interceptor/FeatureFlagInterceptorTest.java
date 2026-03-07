package net.brightroom.featureflag.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
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
import net.brightroom.featureflag.core.provider.ConditionProvider;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class FeatureFlagInterceptorTest {

  // --- helpers: build a pipeline from individual step mocks ---

  private final FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
  private final RolloutStrategy rolloutStrategy = mock(RolloutStrategy.class);
  private final FeatureFlagContextResolver contextResolver = mock(FeatureFlagContextResolver.class);
  private final RolloutPercentageProvider rolloutPercentageProvider =
      mock(RolloutPercentageProvider.class);
  private final ConditionProvider conditionProvider = mock(ConditionProvider.class);
  private final FeatureFlagConditionEvaluator conditionEvaluator =
      mock(FeatureFlagConditionEvaluator.class);
  private final ScheduleProvider scheduleProvider =
      mock(ScheduleProvider.class, invocation -> Optional.empty());

  private FeatureFlagInterceptor buildInterceptor() {
    List<EvaluationStep> steps =
        List.of(
            new EnabledEvaluationStep(provider),
            new ScheduleEvaluationStep(scheduleProvider, Clock.systemDefaultZone()),
            new ConditionEvaluationStep(conditionEvaluator),
            new RolloutEvaluationStep(rolloutStrategy));
    FeatureFlagEvaluationPipeline pipeline = new FeatureFlagEvaluationPipeline(steps);
    return new FeatureFlagInterceptor(
        pipeline, rolloutPercentageProvider, conditionProvider, contextResolver);
  }

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);

  @BeforeEach
  void setUp() {
    // EvaluationContext is always built eagerly, so stub request and default dependencies
    stubRequestForConditionVariables();
    when(rolloutPercentageProvider.getRolloutPercentage(any())).thenReturn(OptionalInt.empty());
    when(conditionProvider.getCondition(any())).thenReturn(Optional.empty());
    when(contextResolver.resolve(request)).thenReturn(Optional.empty());
  }

  private HandlerMethod handlerMethodWithAnnotation(FeatureFlag annotation) {
    HandlerMethod handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getMethodAnnotation(FeatureFlag.class)).thenReturn(annotation);
    return handlerMethod;
  }

  private FeatureFlag featureFlagAnnotation(String value) {
    FeatureFlag annotation = mock(FeatureFlag.class);
    when(annotation.value()).thenReturn(value);
    return annotation;
  }

  // --- checkSchedule ---

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenScheduleIsInactive() {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(inactiveSchedule));

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  @Test
  void preHandle_returnsTrue_whenScheduleIsActive() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(activeSchedule));

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  // --- validateAnnotation ---

  @Test
  void preHandle_throwsIllegalStateException_whenFeatureFlagValueIsEmpty() {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);

    assertThatIllegalStateException()
        .isThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .withMessageContaining("non-empty value");
  }

  // --- checkRollout ---

  @Test
  void preHandle_returnsTrue_whenRolloutIs100() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_returnsTrue_whenContextPresentAndInsideRollout() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(50));
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenContextPresentAndOutsideRollout() {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(50));
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  @Test
  void preHandle_returnsTrue_whenContextIsEmpty() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(50));
    when(contextResolver.resolve(request)).thenReturn(Optional.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_returnsTrue_whenHandlerIsNotHandlerMethod() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    Object notAHandlerMethod = new Object();
    boolean result = interceptor.preHandle(request, response, notAHandlerMethod);
    assertTrue(result);
  }

  @Test
  void preHandle_returnsTrue_whenNoAnnotationOnMethodOrClass() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    HandlerMethod handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getMethodAnnotation(FeatureFlag.class)).thenReturn(null);
    when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  // --- rollout=0 boundary ---

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenRolloutIsZero() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(0));
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 0)).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  // --- class-level @FeatureFlag + rollout ---

  @FeatureFlag("my-feature")
  static class RolloutAnnotatedController {}

  private HandlerMethod handlerMethodWithClassAnnotation() {
    HandlerMethod handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getMethodAnnotation(FeatureFlag.class)).thenReturn(null);
    when(handlerMethod.getBeanType()).thenAnswer(inv -> RolloutAnnotatedController.class);
    return handlerMethod;
  }

  @Test
  void preHandle_returnsTrue_whenClassAnnotationWithRolloutAndContextInsideRollout()
      throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    HandlerMethod handlerMethod = handlerMethodWithClassAnnotation();
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(50));
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void
      preHandle_throwsFeatureFlagAccessDeniedException_whenClassAnnotationWithRolloutAndContextOutsideRollout() {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    HandlerMethod handlerMethod = handlerMethodWithClassAnnotation();
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.of(50));
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  // --- condition ---

  private void stubRequestForConditionVariables() {
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getParameterMap()).thenReturn(Map.of());
    when(request.getCookies()).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/test");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
  }

  @Test
  void preHandle_returnsTrue_whenConditionIsTrue() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    when(conditionProvider.getCondition("my-feature"))
        .thenReturn(Optional.of("headers['X-Beta'] != null"));
    stubRequestForConditionVariables();
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(true);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenConditionIsFalse() {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(conditionProvider.getCondition("my-feature"))
        .thenReturn(Optional.of("headers['X-Beta'] != null"));
    stubRequestForConditionVariables();
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  @Test
  void preHandle_skipsConditionCheck_whenConditionIsEmpty() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    when(conditionProvider.getCondition("my-feature")).thenReturn(Optional.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_evaluatesConditionBeforeRollout() throws Exception {
    FeatureFlagInterceptor interceptor = buildInterceptor();
    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(conditionProvider.getCondition("my-feature"))
        .thenReturn(Optional.of("headers['X-Beta'] != null"));
    stubRequestForConditionVariables();
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  // --- pipeline delegation ---

  @Test
  void preHandle_delegatesToPipeline_whenDecisionIsAllowed() throws Exception {
    FeatureFlagEvaluationPipeline pipeline = mock(FeatureFlagEvaluationPipeline.class);
    FeatureFlagInterceptor interceptor =
        new FeatureFlagInterceptor(
            pipeline, rolloutPercentageProvider, conditionProvider, contextResolver);

    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    when(conditionProvider.getCondition("my-feature")).thenReturn(Optional.empty());
    when(contextResolver.resolve(request)).thenReturn(Optional.empty());
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getParameterMap()).thenReturn(Map.of());
    when(request.getCookies()).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/test");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(pipeline.evaluate(org.mockito.ArgumentMatchers.any()))
        .thenReturn(AccessDecision.allowed());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_throwsException_whenPipelineReturnsDenied() {
    FeatureFlagEvaluationPipeline pipeline = mock(FeatureFlagEvaluationPipeline.class);
    FeatureFlagInterceptor interceptor =
        new FeatureFlagInterceptor(
            pipeline, rolloutPercentageProvider, conditionProvider, contextResolver);

    FeatureFlag annotation = featureFlagAnnotation("my-feature");
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    when(conditionProvider.getCondition("my-feature")).thenReturn(Optional.empty());
    when(contextResolver.resolve(request)).thenReturn(Optional.empty());
    when(request.getHeaderNames()).thenReturn(Collections.emptyEnumeration());
    when(request.getParameterMap()).thenReturn(Map.of());
    when(request.getCookies()).thenReturn(null);
    when(request.getRequestURI()).thenReturn("/test");
    when(request.getMethod()).thenReturn("GET");
    when(request.getRemoteAddr()).thenReturn("127.0.0.1");
    when(pipeline.evaluate(org.mockito.ArgumentMatchers.any()))
        .thenReturn(AccessDecision.denied("my-feature", AccessDecision.DeniedReason.DISABLED));

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }
}
