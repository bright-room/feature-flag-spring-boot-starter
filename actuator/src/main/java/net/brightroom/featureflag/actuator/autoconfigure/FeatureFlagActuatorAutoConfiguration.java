package net.brightroom.featureflag.actuator.autoconfigure;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpoint;
import net.brightroom.featureflag.actuator.endpoint.ReactiveFeatureFlagEndpoint;
import net.brightroom.featureflag.actuator.health.FeatureFlagHealthIndicator;
import net.brightroom.featureflag.actuator.health.ReactiveFeatureFlagHealthIndicator;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.MutableRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.health.autoconfigure.contributor.ConditionalOnEnabledHealthIndicator;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * {@link AutoConfiguration Auto-configuration} for feature flag actuator support.
 *
 * <p>This configuration runs after {@link FeatureFlagAutoConfiguration} (which binds {@link
 * FeatureFlagProperties}) and before the webmvc/webflux auto-configurations. The ordering ensures
 * that the mutable provider is registered before those modules evaluate their own {@code
 * ConditionalOnMissingBean} conditions, preventing duplicate provider registration.
 *
 * <p>Provider registration is split by web application type:
 *
 * <ul>
 *   <li><b>Servlet:</b> Registers {@link MutableInMemoryFeatureFlagProvider} and {@link
 *       FeatureFlagEndpoint}
 *   <li><b>Reactive:</b> Registers {@link MutableInMemoryReactiveFeatureFlagProvider} and {@link
 *       ReactiveFeatureFlagEndpoint}
 * </ul>
 */
@AutoConfiguration(
    after = FeatureFlagAutoConfiguration.class,
    beforeName = {
      "net.brightroom.featureflag.webmvc.autoconfigure.FeatureFlagMvcAutoConfiguration",
      "net.brightroom.featureflag.webflux.autoconfigure.FeatureFlagWebFluxAutoConfiguration"
    })
public class FeatureFlagActuatorAutoConfiguration {

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
  static class ServletConfiguration {

    private final FeatureFlagProperties featureFlagProperties;

    /**
     * Registers a {@link MutableInMemoryFeatureFlagProvider} bean when no other {@link
     * FeatureFlagProvider} bean is already present.
     *
     * @return the mutable in-memory provider initialized from {@link FeatureFlagProperties}
     */
    @Bean
    @ConditionalOnMissingBean(FeatureFlagProvider.class)
    MutableInMemoryFeatureFlagProvider mutableFeatureFlagProvider() {
      return new MutableInMemoryFeatureFlagProvider(
          featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
    }

    /**
     * Registers a {@link MutableInMemoryRolloutPercentageProvider} bean when no other {@link
     * RolloutPercentageProvider} bean is already present.
     *
     * @return the mutable in-memory rollout percentage provider initialized from {@link
     *     FeatureFlagProperties}
     */
    @Bean
    @ConditionalOnMissingBean(RolloutPercentageProvider.class)
    MutableInMemoryRolloutPercentageProvider mutableRolloutPercentageProvider() {
      return new MutableInMemoryRolloutPercentageProvider(
          featureFlagProperties.rolloutPercentages());
    }

    /**
     * Registers the {@link FeatureFlagHealthIndicator} bean when the {@code featureFlag} health
     * indicator is enabled.
     *
     * @param provider the feature flag provider
     * @return the feature flag health indicator
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("featureFlag")
    FeatureFlagHealthIndicator featureFlagHealthIndicator(FeatureFlagProvider provider) {
      return new FeatureFlagHealthIndicator(provider, featureFlagProperties);
    }

    /**
     * Registers the {@link FeatureFlagEndpoint} bean when a {@link MutableFeatureFlagProvider} bean
     * is present.
     *
     * @param provider the mutable feature flag provider
     * @param rolloutProvider the mutable rollout percentage provider
     * @param eventPublisher the publisher used to broadcast flag change events
     * @return the feature flag actuator endpoint
     */
    @Bean
    @ConditionalOnBean(MutableFeatureFlagProvider.class)
    FeatureFlagEndpoint featureFlagEndpoint(
        MutableFeatureFlagProvider provider,
        MutableRolloutPercentageProvider rolloutProvider,
        ApplicationEventPublisher eventPublisher) {
      return new FeatureFlagEndpoint(
          provider, rolloutProvider, featureFlagProperties.defaultEnabled(), eventPublisher);
    }

    ServletConfiguration(FeatureFlagProperties featureFlagProperties) {
      this.featureFlagProperties = featureFlagProperties;
    }
  }

  @Configuration(proxyBeanMethods = false)
  @ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
  static class ReactiveConfiguration {

    private final FeatureFlagProperties featureFlagProperties;

    /**
     * Registers a {@link MutableInMemoryReactiveFeatureFlagProvider} bean when no other {@link
     * ReactiveFeatureFlagProvider} bean is already present.
     *
     * @return the mutable in-memory reactive provider initialized from {@link
     *     FeatureFlagProperties}
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveFeatureFlagProvider.class)
    MutableInMemoryReactiveFeatureFlagProvider mutableReactiveFeatureFlagProvider() {
      return new MutableInMemoryReactiveFeatureFlagProvider(
          featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
    }

    /**
     * Registers a {@link MutableInMemoryReactiveRolloutPercentageProvider} bean when no other
     * {@link ReactiveRolloutPercentageProvider} bean is already present.
     *
     * @return the mutable in-memory reactive rollout percentage provider initialized from {@link
     *     FeatureFlagProperties}
     */
    @Bean
    @ConditionalOnMissingBean(ReactiveRolloutPercentageProvider.class)
    MutableInMemoryReactiveRolloutPercentageProvider mutableReactiveRolloutPercentageProvider() {
      return new MutableInMemoryReactiveRolloutPercentageProvider(
          featureFlagProperties.rolloutPercentages());
    }

    /**
     * Registers the {@link ReactiveFeatureFlagHealthIndicator} bean when the {@code featureFlag}
     * health indicator is enabled.
     *
     * @param provider the reactive feature flag provider
     * @return the reactive feature flag health indicator
     */
    @Bean
    @ConditionalOnEnabledHealthIndicator("featureFlag")
    ReactiveFeatureFlagHealthIndicator reactiveFeatureFlagHealthIndicator(
        ReactiveFeatureFlagProvider provider) {
      return new ReactiveFeatureFlagHealthIndicator(provider, featureFlagProperties);
    }

    /**
     * Registers the {@link ReactiveFeatureFlagEndpoint} bean when a {@link
     * MutableReactiveFeatureFlagProvider} bean is present.
     *
     * @param provider the mutable reactive feature flag provider
     * @param rolloutProvider the mutable reactive rollout percentage provider
     * @param eventPublisher the publisher used to broadcast flag change events
     * @return the reactive feature flag actuator endpoint
     */
    @Bean
    @ConditionalOnBean(MutableReactiveFeatureFlagProvider.class)
    ReactiveFeatureFlagEndpoint reactiveFeatureFlagEndpoint(
        MutableReactiveFeatureFlagProvider provider,
        MutableReactiveRolloutPercentageProvider rolloutProvider,
        ApplicationEventPublisher eventPublisher) {
      return new ReactiveFeatureFlagEndpoint(
          provider, rolloutProvider, featureFlagProperties.defaultEnabled(), eventPublisher);
    }

    ReactiveConfiguration(FeatureFlagProperties featureFlagProperties) {
      this.featureFlagProperties = featureFlagProperties;
    }
  }
}
