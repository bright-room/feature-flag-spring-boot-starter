package net.brightroom.featureflag.webflux.autoconfigure;

import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.InMemoryReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.webflux.aspect.FeatureFlagAspect;
import net.brightroom.featureflag.webflux.context.RandomReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.context.ReactiveFeatureFlagContextResolver;
import net.brightroom.featureflag.webflux.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction;
import net.brightroom.featureflag.webflux.provider.InMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolution;
import net.brightroom.featureflag.webflux.resolution.exceptionhandler.AccessDeniedExceptionHandlerResolutionFactory;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webflux.resolution.handlerfilter.AccessDeniedHandlerFilterResolutionFactory;
import net.brightroom.featureflag.webflux.rollout.DefaultReactiveRolloutStrategy;
import net.brightroom.featureflag.webflux.rollout.ReactiveRolloutStrategy;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;

/**
 * Auto-configuration for the Spring WebFlux feature flag integration.
 *
 * <p>Registers the following beans when running in a reactive web application:
 *
 * <ul>
 *   <li>{@link net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider} — in-memory
 *       provider backed by {@code feature-flags.feature-names} config (conditional on missing bean)
 *   <li>{@link net.brightroom.featureflag.webflux.aspect.FeatureFlagAspect} — AOP aspect for
 *       annotation-based controllers (conditional on missing bean)
 *   <li>{@link net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction} — filter
 *       factory for functional endpoints (conditional on missing bean)
 *   <li>{@link org.springframework.web.server.WebFilter} — propagates {@link
 *       org.springframework.web.server.ServerWebExchange} into the Reactor context
 * </ul>
 */
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
@EnableAspectJAutoProxy(proxyTargetClass = true)
public class FeatureFlagWebFluxAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
  ReactiveFeatureFlagProvider reactiveFeatureFlagProvider() {
    return new InMemoryReactiveFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  @ConditionalOnMissingBean
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

  @Bean
  @ConditionalOnMissingBean(ReactiveRolloutPercentageProvider.class)
  ReactiveRolloutPercentageProvider reactiveRolloutPercentageProvider() {
    return new InMemoryReactiveRolloutPercentageProvider(
        featureFlagProperties.rolloutPercentages());
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
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider reactiveRolloutPercentageProvider) {
    return new FeatureFlagAspect(
        reactiveFeatureFlagProvider,
        reactiveRolloutStrategy,
        contextResolver,
        reactiveRolloutPercentageProvider);
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
      ReactiveFeatureFlagContextResolver contextResolver,
      ReactiveRolloutPercentageProvider reactiveRolloutPercentageProvider) {
    return new FeatureFlagHandlerFilterFunction(
        reactiveFeatureFlagProvider,
        accessDeniedHandlerResolution,
        reactiveRolloutStrategy,
        contextResolver,
        reactiveRolloutPercentageProvider);
  }

  /**
   * Creates a new {@code FeatureFlagWebFluxAutoConfiguration}.
   *
   * @param featureFlagProperties the feature flag configuration properties
   */
  FeatureFlagWebFluxAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
