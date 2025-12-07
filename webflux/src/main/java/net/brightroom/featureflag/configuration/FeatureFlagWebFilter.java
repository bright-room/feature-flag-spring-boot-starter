package net.brightroom.featureflag.configuration;

import java.util.Objects;
import net.brightroom.featureflag.annotation.FeatureFlag;
import net.brightroom.featureflag.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

class FeatureFlagWebFilter implements WebFilter {

  FeatureFlagProvider featureFlagProvider;
  HandlerMapping handlerMapping;

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    return handlerMapping
        .getHandler(exchange)
        .ofType(HandlerMethod.class)
        .flatMap(handlerMethod -> validateFeatureFlag(handlerMethod, chain, exchange))
        .switchIfEmpty(chain.filter(exchange));
  }

  private boolean checkFeatureFlag(FeatureFlag annotation, WebFilterChain chain, ServerWebExchange exchange) {
    return featureFlagProvider
            .isFeatureEnabled(annotation.value())
            .filter(Boolean::booleanValue)
            .flatMap(_ -> chain.filter(exchange))
            .switchIfEmpty(
                    Mono.error(new FeatureFlagAccessDeniedException("Feature flag is disabled.")));
  }

  private Mono<Void> validateFeatureFlag(HandlerMethod handlerMethod, WebFilterChain chain, ServerWebExchange exchange) {
    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (Objects.nonNull(methodAnnotation)) {
      return checkFeatureFlagAndProceed(methodAnnotation, chain, exchange);
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (Objects.nonNull(classAnnotation)) {
      return checkFeatureFlagAndProceed(classAnnotation, chain, exchange);
    }

    return chain.filter(exchange);
  }

  private Mono<Void> checkFeatureFlagAndProceed(FeatureFlag annotation, WebFilterChain chain, ServerWebExchange exchange) {
    return featureFlagProvider
        .isFeatureEnabled(annotation.value())
        .filter(Boolean::booleanValue)
        .flatMap(_ -> chain.filter(exchange))
        .switchIfEmpty(
            Mono.error(new FeatureFlagAccessDeniedException("Feature flag is disabled.")));
  }

  FeatureFlagWebFilter(FeatureFlagProvider featureFlagProvider, HandlerMapping handlerMapping) {
    this.featureFlagProvider = featureFlagProvider;
    this.handlerMapping = handlerMapping;
  }
}
