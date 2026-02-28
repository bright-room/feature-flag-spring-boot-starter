package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class FeatureFlagWebFilter implements WebFilter {

  private final RequestMappingHandlerMapping handlerMapping;
  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final AccessDeniedWebFilterResolution resolution;

  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    return doFilter(exchange, chain).then();
  }

  /**
   * Internal filter that uses a non-void return type so that {@code switchIfEmpty} correctly
   * distinguishes "handler found and processed" from "no handler found". Using {@code Mono<Void>}
   * would not work because {@code Mono<Void>} never emits items, causing {@code switchIfEmpty} to
   * always fire even after the filter chain has already completed.
   */
  private Mono<Boolean> doFilter(ServerWebExchange exchange, WebFilterChain chain) {
    Mono<FeatureFlag> annotationMono =
        handlerMapping.getHandler(exchange).flatMap(this::resolveAnnotationFromHandler);
    return annotationMono
        .flatMap(annotation -> checkAndFilter(exchange, chain, annotation))
        .switchIfEmpty(chain.filter(exchange).thenReturn(Boolean.FALSE));
  }

  private Mono<FeatureFlag> resolveAnnotationFromHandler(Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return Mono.empty();
    }
    return Mono.justOrEmpty(resolveAnnotation(handlerMethod));
  }

  private Mono<Boolean> checkAndFilter(
      ServerWebExchange exchange, WebFilterChain chain, FeatureFlag annotation) {
    validateAnnotation(annotation);
    return reactiveFeatureFlagProvider
        .isFeatureEnabled(annotation.value())
        .flatMap(enabled -> filterByFeatureEnabled(exchange, chain, annotation, enabled));
  }

  private Mono<Boolean> filterByFeatureEnabled(
      ServerWebExchange exchange, WebFilterChain chain, FeatureFlag annotation, boolean enabled) {
    if (enabled) {
      return chain.filter(exchange).thenReturn(Boolean.TRUE);
    }
    return resolution
        .resolve(exchange, new FeatureFlagAccessDeniedException(annotation.value()))
        .thenReturn(Boolean.FALSE);
  }

  @Nullable
  private FeatureFlag resolveAnnotation(HandlerMethod handlerMethod) {
    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }
    return handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
  }

  FeatureFlagWebFilter(
      RequestMappingHandlerMapping handlerMapping,
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedWebFilterResolution resolution) {
    this.handlerMapping = handlerMapping;
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.resolution = resolution;
  }
}
