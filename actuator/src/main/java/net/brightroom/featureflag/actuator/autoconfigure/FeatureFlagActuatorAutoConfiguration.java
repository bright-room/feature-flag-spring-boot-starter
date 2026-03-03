package net.brightroom.featureflag.actuator.autoconfigure;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpoint;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-configuration} for feature flag actuator support.
 *
 * <p>This configuration runs after {@link FeatureFlagAutoConfiguration} (which binds {@link
 * FeatureFlagProperties}) and before the webmvc/webflux auto-configurations. The ordering ensures
 * that {@link MutableInMemoryFeatureFlagProvider} is registered as the {@link FeatureFlagProvider}
 * bean before those modules evaluate their own {@code @ConditionalOnMissingBean(FeatureFlagProvider
 * .class)} conditions, preventing duplicate provider registration.
 *
 * <p>The {@link FeatureFlagEndpoint} bean is only created when a {@link MutableFeatureFlagProvider}
 * bean is present in the context.
 */
@AutoConfiguration(
    after = FeatureFlagAutoConfiguration.class,
    beforeName = {
      "net.brightroom.featureflag.webmvc.autoconfigure.FeatureFlagMvcAutoConfiguration",
      "net.brightroom.featureflag.webflux.autoconfigure.FeatureFlagWebFluxAutoConfiguration"
    })
public class FeatureFlagActuatorAutoConfiguration {

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
   * Registers the {@link FeatureFlagEndpoint} bean when a {@link MutableFeatureFlagProvider} bean
   * is present.
   *
   * @param provider the mutable feature flag provider
   * @param eventPublisher the publisher used to broadcast flag change events
   * @return the feature flag actuator endpoint
   */
  @Bean
  @ConditionalOnBean(MutableFeatureFlagProvider.class)
  FeatureFlagEndpoint featureFlagEndpoint(
      MutableFeatureFlagProvider provider, ApplicationEventPublisher eventPublisher) {
    return new FeatureFlagEndpoint(
        provider, featureFlagProperties.defaultEnabled(), eventPublisher);
  }

  /** Package-private constructor for auto-configuration. */
  FeatureFlagActuatorAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
