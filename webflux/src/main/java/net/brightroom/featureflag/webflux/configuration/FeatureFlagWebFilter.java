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
  private final AccessDeniedReactiveResolution resolution;

  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    return handlerMapping
        .getHandler(exchange)
        .flatMap(handler -> handleRequest(exchange, chain, handler))
        .switchIfEmpty(chain.filter(exchange));
  }

  private Mono<Void> handleRequest(
      ServerWebExchange exchange, WebFilterChain chain, Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return chain.filter(exchange);
    }
    FeatureFlag annotation = resolveAnnotation(handlerMethod);
    if (annotation == null) {
      return chain.filter(exchange);
    }
    return checkAndFilter(exchange, chain, annotation);
  }

  private Mono<Void> checkAndFilter(
      ServerWebExchange exchange, WebFilterChain chain, FeatureFlag annotation) {
    validateAnnotation(annotation);
    return reactiveFeatureFlagProvider
        .isFeatureEnabled(annotation.value())
        .flatMap(
            enabled ->
                enabled
                    ? chain.filter(exchange)
                    : resolution.resolve(
                        exchange, new FeatureFlagAccessDeniedException(annotation.value())));
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
      AccessDeniedReactiveResolution resolution) {
    this.handlerMapping = handlerMapping;
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.resolution = resolution;
  }
}
