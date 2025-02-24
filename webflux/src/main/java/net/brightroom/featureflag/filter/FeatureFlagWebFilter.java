package net.brightroom.featureflag.filter;

import java.util.Objects;
import net.brightroom.featureflag.annotation.FeatureFlag;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.lang.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.reactive.result.method.annotation.RequestMappingHandlerMapping;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * WebFlux filter implementation for Feature Flag checking. This filter intercepts WebFlux requests
 * and checks for Feature Flag annotations at both method and class levels.
 *
 * <p>The filter is configured with the highest precedence to ensure feature flag checking occurs
 * before other processing.
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
public class FeatureFlagWebFilter implements WebFilter {

  FeatureFlagProvider featureFlagProvider;
  RequestMappingHandlerMapping handlerMapping;

  @Override
  @NonNull
  public Mono<Void> filter(@NonNull ServerWebExchange exchange, @NonNull WebFilterChain chain) {
    return handlerMapping
        .getHandler(exchange)
        .cast(HandlerMethod.class)
        .flatMap(handlerMethod -> checkFeatureFlags(handlerMethod, exchange))
        .switchIfEmpty(chain.filter(exchange))
        .onErrorResume(
            FeatureFlagDisabledException.class, ex -> exchange.getResponse().setComplete());
  }

  private Mono<Void> checkFeatureFlags(HandlerMethod handlerMethod, ServerWebExchange exchange) {
    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (Objects.nonNull(methodAnnotation)) {
      return featureFlagProvider
          .isFeatureEnabled(methodAnnotation.feature())
          .flatMap(isEnabled -> isEnabled ? Mono.empty() : handleDisabledFeature(exchange));
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (Objects.nonNull(classAnnotation)) {
      return featureFlagProvider
          .isFeatureEnabled(classAnnotation.feature())
          .flatMap(isEnabled -> isEnabled ? Mono.empty() : handleDisabledFeature(exchange));
    }

    return Mono.empty();
  }

  private Mono<Void> handleDisabledFeature(ServerWebExchange exchange) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.METHOD_NOT_ALLOWED);

    DataBufferFactory bufferFactory = response.bufferFactory();
    DataBuffer buffer = bufferFactory.wrap("This feature is not available".getBytes());

    return response.writeWith(Mono.just(buffer));
  }

  /**
   * Constructor
   *
   * @param featureFlagProvider featureFlagProvider
   * @param handlerMapping handlerMapping
   */
  public FeatureFlagWebFilter(
      FeatureFlagProvider featureFlagProvider, RequestMappingHandlerMapping handlerMapping) {
    this.featureFlagProvider = featureFlagProvider;
    this.handlerMapping = handlerMapping;
  }
}
