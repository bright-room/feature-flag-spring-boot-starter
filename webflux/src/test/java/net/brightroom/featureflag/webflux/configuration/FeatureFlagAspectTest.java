package net.brightroom.featureflag.webflux.configuration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Mono;

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
}
