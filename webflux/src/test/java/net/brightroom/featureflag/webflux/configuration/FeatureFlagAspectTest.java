package net.brightroom.featureflag.webflux.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

class FeatureFlagAspectTest {

  private final ReactiveFeatureFlagProvider provider = mock(ReactiveFeatureFlagProvider.class);
  private final FeatureFlagAspect aspect = new FeatureFlagAspect(provider);

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
}
