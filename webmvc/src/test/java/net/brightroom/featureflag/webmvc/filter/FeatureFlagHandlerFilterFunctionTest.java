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
import java.util.Optional;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
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
  private final FeatureFlagHandlerFilterFunction filterFunction =
      new FeatureFlagHandlerFilterFunction(
          provider, resolution, new DefaultRolloutStrategy(), contextResolver);

  // Filter function with mocked rollout strategy for rollout-specific tests
  private final RolloutStrategy rolloutStrategy = mock(RolloutStrategy.class);
  private final FeatureFlagHandlerFilterFunction filterFunctionWithRollout =
      new FeatureFlagHandlerFilterFunction(provider, resolution, rolloutStrategy, contextResolver);

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
  void of_delegatesToNext_whenFeatureEnabled() throws Exception {
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);
    ServerRequest request = mock(ServerRequest.class);
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
    ServerRequest request = mock(ServerRequest.class);
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

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);

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

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);

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
    // fail-open: when context is not available, rollout check is skipped
    when(provider.isFeatureEnabled("my-feature")).thenReturn(true);

    HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
    ServerRequest request = mock(ServerRequest.class);
    when(request.servletRequest()).thenReturn(httpServletRequest);

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
}
