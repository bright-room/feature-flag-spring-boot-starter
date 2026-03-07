package net.brightroom.featureflag.webflux.aspect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeatureFlagAspectTest {

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final ReactiveFeatureFlagContextResolver contextResolver =
      mock(ReactiveFeatureFlagContextResolver.class);
  // Default: no rollout percentage configured in provider, falls back to annotation value
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider =
      mock(ReactiveRolloutPercentageProvider.class, invocation -> Mono.empty());
  private final FeatureFlagConditionEvaluator conditionEvaluator =
      mock(FeatureFlagConditionEvaluator.class);
  private final FeatureFlagAspect aspect =
      new FeatureFlagAspect(
          provider,
          new DefaultReactiveRolloutStrategy(),
          contextResolver,
          rolloutPercentageProvider,
          conditionEvaluator);

  // Aspect with mocked rollout strategy for rollout-specific tests
  private final ReactiveRolloutStrategy rolloutStrategy = mock(ReactiveRolloutStrategy.class);
  private final FeatureFlagAspect aspectWithRollout =
      new FeatureFlagAspect(
          provider,
          rolloutStrategy,
          contextResolver,
          rolloutPercentageProvider,
          conditionEvaluator);

  static class TestController {

    @FeatureFlag("")
    public Mono<String> emptyAnnotationMethod() {
      return Mono.just("ok");
    }

    @FeatureFlag("some-feature")
    public String nonReactiveMethod() {
      return "non-reactive";
    }

    @FeatureFlag("some-feature")
    public Mono<String> monoMethod() {
      return Mono.just("result");
    }

    @FeatureFlag("some-feature")
    public Flux<String> fluxMethod() {
      return Flux.just("result1", "result2");
    }

    @FeatureFlag(value = "some-feature", rollout = 50)
    public Mono<String> rolloutMonoMethod() {
      return Mono.just("result");
    }

    @FeatureFlag(value = "some-feature", rollout = 50)
    public Flux<String> rolloutFluxMethod() {
      return Flux.just("result1", "result2");
    }

    @FeatureFlag(value = "some-feature", rollout = -1)
    public Mono<String> negativeRolloutMethod() {
      return Mono.just("result");
    }

    @FeatureFlag(value = "some-feature", rollout = 101)
    public Mono<String> over100RolloutMethod() {
      return Mono.just("result");
    }
  }

  static class NoAnnotationController {

    public Mono<String> noAnnotationMethod() {
      return Mono.just("no-annotation");
    }
  }

  @Test
  void checkFeatureFlag_throwsIllegalStateException_whenAnnotationValueIsEmpty() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("emptyAnnotationMethod");
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(new TestController());

    assertThatThrownBy(() -> aspect.checkFeatureFlag(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("non-empty value");
  }

  @Test
  void checkFeatureFlag_throwsIllegalStateException_whenRolloutIsNegative() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("negativeRolloutMethod");
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(new TestController());

    assertThatThrownBy(() -> aspect.checkFeatureFlag(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void checkFeatureFlag_throwsIllegalStateException_whenRolloutIsOver100() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("over100RolloutMethod");
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(new TestController());

    assertThatThrownBy(() -> aspect.checkFeatureFlag(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("rollout must be between 0 and 100");
  }

  @Test
  void checkFeatureFlag_throwsIllegalStateException_whenReturnTypeIsNonReactive() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("nonReactiveMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(String.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));

    assertThatThrownBy(() -> aspect.checkFeatureFlag(joinPoint))
        .isInstanceOf(IllegalStateException.class)
        .hasMessageContaining("reactive return type");
  }

  @Test
  void checkFeatureFlag_proceedsWithoutCheck_whenNoAnnotationFound() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = NoAnnotationController.class.getMethod("noAnnotationMethod");
    when(signature.getMethod()).thenReturn(method);
    when(joinPoint.getTarget()).thenReturn(new NoAnnotationController());
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    aspect.checkFeatureFlag(joinPoint);

    verify(joinPoint).proceed();
    verifyNoInteractions(provider);
  }

  @Test
  void checkFeatureFlag_returnsMono_whenFeatureEnabled() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<String> mono = (Mono<String>) result;
    StepVerifier.create(mono).expectNext("result").verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsMonoError_whenFeatureDisabled() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(false));

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create((Mono<?>) result)
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_returnsMonoError_whenRolloutCheckFails() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("some-feature", context, 50)).thenReturn(Mono.just(false));

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_returnsMono_whenRolloutCheckPasses() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("some-feature", context, 50)).thenReturn(Mono.just(true));

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<String> mono = (Mono<String>) result;
    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsMono_whenContextIsEmpty() throws Throwable {
    // fail-open: when context is not available, rollout check is skipped
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.empty());

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<String> mono = (Mono<String>) result;
    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsFluxError_whenRolloutCheckFails() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("some-feature", context, 50)).thenReturn(Mono.just(false));

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_returnsFlux_whenRolloutCheckPasses() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);

    FeatureFlagContext context = new FeatureFlagContext("user-1");
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.just(context));
    when(rolloutStrategy.isInRollout("some-feature", context, 50)).thenReturn(Mono.just(true));

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Flux<String> flux = (Flux<String>) result;
    StepVerifier.create(flux.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result1", "result2")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsFlux_whenContextIsEmpty() throws Throwable {
    // fail-open: when context is not available, rollout check is skipped
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    when(contextResolver.resolve(httpRequest)).thenReturn(Mono.empty());

    Object result = aspectWithRollout.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Flux<String> flux = (Flux<String>) result;
    StepVerifier.create(flux.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result1", "result2")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsFlux_whenFeatureEnabled() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("fluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Flux<String> flux = (Flux<String>) result;
    StepVerifier.create(flux).expectNext("result1", "result2").verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsMonoError_whenProceedThrows() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    RuntimeException cause = new RuntimeException("unexpected");
    when(joinPoint.proceed()).thenThrow(cause);

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create((Mono<?>) result).expectErrorMatches(e -> e == cause).verify();
  }

  @Test
  void checkFeatureFlag_returnsFluxError_whenFeatureDisabled() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("fluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(false));

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create((Flux<?>) result)
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_returnsFluxError_whenProceedThrows() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("fluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    RuntimeException cause = new RuntimeException("unexpected");
    when(joinPoint.proceed()).thenThrow(cause);

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create((Flux<?>) result).expectErrorMatches(e -> e == cause).verify();
  }
}
