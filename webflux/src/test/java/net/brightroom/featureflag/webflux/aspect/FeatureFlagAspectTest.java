package net.brightroom.featureflag.webflux.aspect;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.condition.ReactiveFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.evaluation.ReactiveConditionEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveEnabledEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveFeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.evaluation.ReactiveRolloutEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ReactiveScheduleEvaluationStep;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ReactiveConditionProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveScheduleProvider;
import net.brightroom.featureflag.core.provider.Schedule;
import net.brightroom.featureflag.core.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.core.rollout.ReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeatureFlagAspectTest {

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final ReactiveFeatureFlagContextResolver contextResolver =
      mock(ReactiveFeatureFlagContextResolver.class, invocation -> Mono.empty());
  private final ReactiveRolloutPercentageProvider rolloutPercentageProvider =
      mock(ReactiveRolloutPercentageProvider.class, invocation -> Mono.empty());
  private final ReactiveConditionProvider conditionProvider =
      mock(ReactiveConditionProvider.class, invocation -> Mono.empty());
  private final ReactiveFeatureFlagConditionEvaluator conditionEvaluator =
      mock(ReactiveFeatureFlagConditionEvaluator.class);
  private final ReactiveScheduleProvider reactiveScheduleProvider =
      mock(ReactiveScheduleProvider.class, invocation -> Mono.empty());
  private final ReactiveRolloutStrategy rolloutStrategy = mock(ReactiveRolloutStrategy.class);

  private ReactiveFeatureFlagEvaluationPipeline buildPipeline(ReactiveRolloutStrategy strategy) {
    List<ReactiveEvaluationStep> steps =
        List.of(
            new ReactiveEnabledEvaluationStep(provider),
            new ReactiveScheduleEvaluationStep(reactiveScheduleProvider, Clock.systemDefaultZone()),
            new ReactiveConditionEvaluationStep(conditionEvaluator),
            new ReactiveRolloutEvaluationStep(strategy));
    return new ReactiveFeatureFlagEvaluationPipeline(steps);
  }

  private final FeatureFlagAspect aspect =
      new FeatureFlagAspect(
          buildPipeline(new DefaultReactiveRolloutStrategy()),
          contextResolver,
          rolloutPercentageProvider,
          conditionProvider);

  private final FeatureFlagAspect aspectWithRollout =
      new FeatureFlagAspect(
          buildPipeline(rolloutStrategy),
          contextResolver,
          rolloutPercentageProvider,
          conditionProvider);

  // Helper: creates a mock exchange with a mocked request
  private ServerWebExchange mockExchange() {
    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);
    return exchange;
  }

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

    @FeatureFlag("some-feature")
    public Mono<String> rolloutMonoMethod() {
      return Mono.just("result");
    }

    @FeatureFlag("some-feature")
    public Flux<String> rolloutFluxMethod() {
      return Flux.just("result1", "result2");
    }

    @FeatureFlag("some-feature")
    public Mono<String> conditionMonoMethod() {
      return Mono.just("result");
    }

    @FeatureFlag("some-feature")
    public Flux<String> conditionFluxMethod() {
      return Flux.just("result1", "result2");
    }
  }

  static class NoAnnotationController {

    public Mono<String> noAnnotationMethod() {
      return Mono.just("no-annotation");
    }
  }

  // --- checkSchedule for Mono ---

  @Test
  @SuppressWarnings("unchecked")
  void checkFeatureFlag_emitsError_whenScheduleIsInactive_forMono() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(reactiveScheduleProvider.getSchedule("some-feature"))
        .thenReturn(Mono.just(inactiveSchedule));

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<Object>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectError(FeatureFlagAccessDeniedException.class)
        .verify();
  }

  @Test
  @SuppressWarnings("unchecked")
  void checkFeatureFlag_proceeds_whenScheduleIsActive_forMono() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(reactiveScheduleProvider.getSchedule("some-feature"))
        .thenReturn(Mono.just(activeSchedule));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<Object>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result")
        .verifyComplete();
  }

  // --- checkSchedule for Flux ---

  @Test
  @SuppressWarnings("unchecked")
  void checkFeatureFlag_emitsError_whenScheduleIsInactive_forFlux() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("fluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    Schedule inactiveSchedule = new Schedule(null, LocalDateTime.of(2020, 1, 1, 0, 0), null);
    when(reactiveScheduleProvider.getSchedule("some-feature"))
        .thenReturn(Mono.just(inactiveSchedule));

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<Object>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectError(FeatureFlagAccessDeniedException.class)
        .verify();
  }

  @Test
  @SuppressWarnings("unchecked")
  void checkFeatureFlag_proceeds_whenScheduleIsActive_forFlux() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("fluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    Schedule activeSchedule = new Schedule(LocalDateTime.of(2020, 1, 1, 0, 0), null, null);
    when(reactiveScheduleProvider.getSchedule("some-feature"))
        .thenReturn(Mono.just(activeSchedule));
    when(joinPoint.proceed()).thenReturn(Flux.just("r1", "r2"));

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<Object>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("r1", "r2")
        .verifyComplete();
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
  void checkFeatureFlag_throwsIllegalStateException_whenReturnTypeIsNonReactive() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("nonReactiveMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(String.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());

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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<String> mono = (Mono<String>) result;
    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result")
        .verifyComplete();
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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
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
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

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
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

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
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);
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
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

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
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

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
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("rolloutFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(rolloutPercentageProvider.getRolloutPercentage("some-feature")).thenReturn(Mono.just(50));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);
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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Flux<String> flux = (Flux<String>) result;
    StepVerifier.create(flux.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result1", "result2")
        .verifyComplete();
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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(e -> e == cause)
        .verify();
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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  // --- condition ---

  private void stubRequestForConditionVariables(ServerHttpRequest httpRequest) {
    when(httpRequest.getHeaders()).thenReturn(new HttpHeaders());
    when(httpRequest.getQueryParams()).thenReturn(new LinkedMultiValueMap<>());
    when(httpRequest.getCookies()).thenReturn(new LinkedMultiValueMap<>());
    org.springframework.http.server.RequestPath path =
        mock(org.springframework.http.server.RequestPath.class);
    when(path.value()).thenReturn("/test");
    when(httpRequest.getPath()).thenReturn(path);
    when(httpRequest.getMethod()).thenReturn(HttpMethod.GET);
    when(httpRequest.getRemoteAddress()).thenReturn(null);
  }

  @Test
  void checkFeatureFlag_returnsMono_whenConditionIsTrue() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("conditionMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(conditionProvider.getCondition("some-feature"))
        .thenReturn(Mono.just("headers['X-Beta'] != null"));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<String> mono = (Mono<String>) result;
    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsMonoError_whenConditionIsFalse() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("conditionMonoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(conditionProvider.getCondition("some-feature"))
        .thenReturn(Mono.just("headers['X-Beta'] != null"));
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.just(false));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Mono<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_returnsFlux_whenConditionIsTrue() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("conditionFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(conditionProvider.getCondition("some-feature"))
        .thenReturn(Mono.just("headers['X-Beta'] != null"));
    when(joinPoint.proceed()).thenReturn(Flux.just("result1", "result2"));
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.just(true));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Flux<String> flux = (Flux<String>) result;
    StepVerifier.create(flux.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNext("result1", "result2")
        .verifyComplete();
  }

  @Test
  void checkFeatureFlag_returnsFluxError_whenConditionIsFalse() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("conditionFluxMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Flux.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(conditionProvider.getCondition("some-feature"))
        .thenReturn(Mono.just("headers['X-Beta'] != null"));
    when(conditionEvaluator.evaluate(
            org.mockito.ArgumentMatchers.eq("headers['X-Beta'] != null"),
            org.mockito.ArgumentMatchers.any()))
        .thenReturn(Mono.just(false));

    ServerWebExchange exchange = mock(ServerWebExchange.class);
    ServerHttpRequest httpRequest = mock(ServerHttpRequest.class);
    when(exchange.getRequest()).thenReturn(httpRequest);
    stubRequestForConditionVariables(httpRequest);

    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(
            e ->
                e instanceof FeatureFlagAccessDeniedException
                    && ((FeatureFlagAccessDeniedException) e).featureName().equals("some-feature"))
        .verify();
  }

  @Test
  void checkFeatureFlag_skipsConditionCheck_whenConditionIsEmpty() throws Throwable {
    ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
    MethodSignature signature = mock(MethodSignature.class);
    when(joinPoint.getSignature()).thenReturn(signature);

    Method method = TestController.class.getMethod("monoMethod");
    when(signature.getMethod()).thenReturn(method);
    when(signature.getReturnType()).thenReturn(Mono.class);
    when(joinPoint.getTarget()).thenReturn(new TestController());
    when(provider.isFeatureEnabled("some-feature")).thenReturn(Mono.just(true));
    when(joinPoint.proceed()).thenReturn(Mono.just("result"));

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    @SuppressWarnings("unchecked")
    Mono<Object> mono = (Mono<Object>) result;
    StepVerifier.create(mono.contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectNextCount(1)
        .verifyComplete();
    verifyNoInteractions(conditionEvaluator);
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

    ServerWebExchange exchange = mockExchange();
    Object result = aspect.checkFeatureFlag(joinPoint);

    StepVerifier.create(
            ((Flux<?>) result).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange)))
        .expectErrorMatches(e -> e == cause)
        .verify();
  }
}
