package net.brightroom.featureflag.webflux.autoconfigure;

import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.webflux.aspect.FeatureFlagAspect;
import net.brightroom.featureflag.webflux.context.RandomReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolution;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolutionFactory;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolutionFactory;
import net.brightroom.featureflag.webflux.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FeatureFlagWebFluxAutoConfiguration {

  private static final Logger log =
      LoggerFactory.getLogger(FeatureFlagWebFluxAutoConfiguration.class);

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  AccessDeniedExceptionHandlerResolution accessDeniedExceptionHandlerResolution() {
    return new AccessDeniedExceptionHandlerResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  FeatureFlagExceptionHandler featureFlagExceptionHandler(
      AccessDeniedExceptionHandlerResolution accessDeniedExceptionHandlerResolution) {
    return new FeatureFlagExceptionHandler(accessDeniedExceptionHandlerResolution);
  }

  @Bean
  @ConditionalOnMissingBean
  ReactiveRolloutStrategy reactiveRolloutStrategy() {
    return new DefaultReactiveRolloutStrategy();
  }

  @Bean
  @ConditionalOnMissingBean
  ReactiveFeatureFlagContextResolver reactiveFeatureFlagContextResolver() {
    return new RandomReactiveFeatureFlagContextResolver();
  }

  /**
   * Propagates {@link ServerWebExchange} into the Reactor context so that {@link FeatureFlagAspect}
   * can access it via {@code Mono.deferContextual} during rollout percentage checks.
   *
   * <p>Spring WebFlux does not automatically add {@link ServerWebExchange} to the Reactor context,
   * so this filter bridges the gap between the servlet-style exchange object and the reactive
   * context, enabling the aspect to resolve the request for sticky rollout without requiring
   * constructor injection of the exchange.
   */
  @Bean
  WebFilter featureFlagServerWebExchangeContextFilter() {
    return (exchange, chain) ->
        chain.filter(exchange).contextWrite(ctx -> ctx.put(ServerWebExchange.class, exchange));
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagAspect featureFlagAspect(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      ReactiveRolloutStrategy reactiveRolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver) {
    return new FeatureFlagAspect(
        reactiveFeatureFlagProvider, reactiveRolloutStrategy, contextResolver);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedHandlerFilterResolution.class)
  AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution() {
    return new AccessDeniedHandlerFilterResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      AccessDeniedHandlerFilterResolution accessDeniedHandlerResolution,
      ReactiveRolloutStrategy reactiveRolloutStrategy,
      ReactiveFeatureFlagContextResolver contextResolver) {
    return new FeatureFlagHandlerFilterFunction(
        reactiveFeatureFlagProvider,
        accessDeniedHandlerResolution,
        reactiveRolloutStrategy,
        contextResolver);
  }

  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;

    @SuppressWarnings("deprecation")
    FeatureFlagPathPatterns pathPatterns = featureFlagProperties.pathPatterns();
    if (pathPatterns.hasIncludes() || pathPatterns.hasExcludes()) {
      log.warn(
          "The 'feature-flags.path-patterns' configuration is set but is not supported by the "
              + "webflux module. Path-based filtering is not applicable because AOP aspects target "
              + "@FeatureFlag-annotated methods directly. "
              + "Consider removing this configuration to avoid confusion.");
    }
  }
}
