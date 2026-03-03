package net.brightroom.featureflag.webmvc.interceptor;

import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Optional;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;

class FeatureFlagInterceptorTest {

  private final FeatureFlagProvider provider = mock(FeatureFlagProvider.class);
  private final RolloutStrategy rolloutStrategy = mock(RolloutStrategy.class);
  private final FeatureFlagContextResolver contextResolver = mock(FeatureFlagContextResolver.class);
  private final FeatureFlagInterceptor interceptor =
      new FeatureFlagInterceptor(provider, rolloutStrategy, contextResolver);

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
    FeatureFlag annotation = mock(FeatureFlag.class);
    when(annotation.value()).thenReturn(value);
    when(annotation.rollout()).thenReturn(rollout);
    return annotation;
  }

  // --- validateAnnotation ---

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

    boolean result = interceptor.preHandle(request, response, handlerMethod);

    assertTrue(result);
    // contextResolver should not be called when rollout is 100
  }

  @Test
  void preHandle_returnsTrue_whenContextPresentAndInsideRollout() throws Exception {
    FeatureFlag annotation = featureFlagAnnotation("my-feature", 50);
    HandlerMethod handlerMethod = handlerMethodWithAnnotation(annotation);
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
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
}
