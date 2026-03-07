package net.brightroom.featureflag.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class FeatureFlagInterceptorTest {

  private final FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
  private final RolloutStrategy rolloutStrategy = mock(RolloutStrategy.class);
  private final FeatureFlagContextResolver contextResolver = mock(FeatureFlagContextResolver.class);
  private final RolloutPercentageProvider rolloutPercentageProvider =
      mock(RolloutPercentageProvider.class);
  private final FeatureFlagConditionEvaluator conditionEvaluator =
      mock(FeatureFlagConditionEvaluator.class);
  private final ScheduleProvider scheduleProvider =
      mock(ScheduleProvider.class, invocation -> Optional.empty());
  private final FeatureFlagInterceptor interceptor =
      new FeatureFlagInterceptor(
          provider,
          rolloutStrategy,
          contextResolver,
          rolloutPercentageProvider,
          conditionEvaluator,
          scheduleProvider,
          Clock.systemDefaultZone());

  private final HttpServletRequest request = mock(HttpServletRequest.class);
  private final HttpServletResponse response = mock(HttpServletResponse.class);

  /**
   * Creates a mocked HandlerMethod whose method-level @FeatureFlag returns the given annotation.
   */
  private HandlerMethod handlerMethodWithAnnotation(FeatureFlag annotation) {
    HandlerMethod handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getMethodAnnotation(FeatureFlag.class)).thenReturn(annotation);
    return handlerMethod;
  }

  private FeatureFlag featureFlagAnnotation(String value, int rollout) {
    return featureFlagAnnotation(value, "", rollout);
  }

  private FeatureFlag featureFlagAnnotation(String value, String condition, int rollout) {
    FeatureFlag annotation = mock(FeatureFlag.class);
    when(annotation.value()).thenReturn(value);
    when(annotation.condition()).thenReturn(condition);
    when(annotation.rollout()).thenReturn(rollout);
    return annotation;
  }

  // --- checkSchedule ---

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenScheduleIsInactive() {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    // end in the past → inactive
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(inactiveSchedule));

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  @Test
  void preHandle_returnsTrue_whenScheduleIsActive() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    // start in the past, no end → active
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(scheduleProvider.getSchedule("my-feature")).thenReturn(Optional.of(activeSchedule));

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  // --- validateAnnotation ---

  @Test
  void preHandle_throwsIllegalStateException_whenFeatureFlagValueIsEmpty() {
    FeatureFlag annotation = featureFlagAnnotation("", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);

    assertThatIllegalStateException()
        .isThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .withMessageContaining("non-empty value");
  }

  @Test
  void preHandle_throwsIllegalStateException_whenRolloutIsNegative() {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", -1);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);

    assertThatIllegalStateException()
        .isThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .withMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void preHandle_throwsIllegalStateException_whenRolloutIsOver100() {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 101);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);

    assertThatIllegalStateException()
        .isThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .withMessageContaining("rollout must be between 0 and 100");
  }

  // --- checkRollout ---

  @Test
  void preHandle_returnsTrue_whenRolloutIs100() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verifyNoInteractions(contextResolver);
  }

  @Test
  void preHandle_returnsTrue_whenContextPresentAndInsideRollout() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 50);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenContextPresentAndOutsideRollout() {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 50);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  @Test
  void preHandle_returnsTrue_whenContextIsEmpty() throws Exception {
    // fail-open: when no context is available, rollout check is skipped
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 50);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    when(contextResolver.resolve(request)).thenReturn(Optional.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void preHandle_returnsTrue_whenHandlerIsNotHandlerMethod() throws Exception {
    // non-HandlerMethod handler is passed through without any check
    Object notAHandlerMethod = new Object();
    boolean result = interceptor.preHandle(request, response, notAHandlerMethod);
    assertTrue(result);
  }

  @Test
  void preHandle_returnsTrue_whenNoAnnotationOnMethodOrClass() throws Exception {
    HandlerMethod handlerMethod = mock(HandlerMethod.class);
    when(handlerMethod.getMethodAnnotation(FeatureFlag.class)).thenReturn(null);
    when(handlerMethod.getBeanType()).thenAnswer(inv -> Object.class);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  // --- rollout=0 boundary (L-3) ---

  @Test
  void preHandle_throwsFeatureFlagAccessDeniedException_whenRolloutIsZero() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 0);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 0)).thenReturn(false);

    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
  }

  // --- class-level @FeatureFlag + rollout (L-4) ---

  @FeatureFlag(value = "my-feature", rollout = 50)
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
    HandlerMethod handlerMethod = handlerMethodWithClassAnnotation();
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(request)).thenReturn(Optional.of(context));
    when(rolloutStrategy.isInRollout("my-feature", context, 50)).thenReturn(true);

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
  }

  @Test
  void
      preHandle_throwsFeatureFlagAccessDeniedException_whenClassAnnotationWithRolloutAndContextOutsideRollout() {
    HandlerMethod handlerMethod = handlerMethodWithClassAnnotation();
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
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
    FeatureFlag annotation = featureFlagAnnotation("my-feature", "headers['X-Beta'] != null", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());
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
    FeatureFlag annotation = featureFlagAnnotation("my-feature", "headers['X-Beta'] != null", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
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
    FeatureFlag annotation = featureFlagAnnotation("my-feature", "", 100);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    when(rolloutPercentageProvider.getRolloutPercentage("my-feature"))
        .thenReturn(OptionalInt.empty());

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    verifyNoInteractions(conditionEvaluator);
  }

  @Test
  void preHandle_evaluatesConditionBeforeRollout() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", "headers['X-Beta'] != null", 50);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    stubRequestForConditionVariables();
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(false);

    // Condition fails, so rollout should not be checked
    assertThatThrownBy(() -> interceptor.preHandle(request, response, handlerMethod))
        .isInstanceOf(FeatureFlagAccessDeniedException.class);
    verifyNoInteractions(rolloutStrategy);
  }
}
