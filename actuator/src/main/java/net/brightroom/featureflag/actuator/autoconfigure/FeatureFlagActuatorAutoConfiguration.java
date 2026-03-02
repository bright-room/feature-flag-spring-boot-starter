package net.brightroom.featureflag.actuator.autoconfigure;

import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpoint;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.InMemoryMutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import org.springframework.boot.actuate.autoconfigure.endpoint.condition.ConditionalOnAvailableEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

/**
 * {@link AutoConfiguration Auto-configuration} for feature flag Actuator endpoint support.
 *
 * <p>Registers the following beans when Spring Boot Actuator is on the classpath:
 *
 * <ul>
 *   <li>{@link InMemoryMutableFeatureFlagProvider} — the default mutable provider, registered only
 *       when no {@link net.brightroom.featureflag.core.provider.FeatureFlagProvider} bean already
 *       exists
 *   <li>{@link FeatureFlagEndpoint} — the Actuator endpoint, registered only when a {@link
 *       MutableFeatureFlagProvider} bean is present and the endpoint is available for exposure
 * </ul>
 *
 * <p>This auto-configuration is ordered before {@code FeatureFlagMvcAutoConfiguration} to ensure
 * that the mutable provider is registered as the primary {@code FeatureFlagProvider} when both the
 * {@code actuator} and {@code webmvc} modules are on the classpath.
 */
@AutoConfiguration(
    after = FeatureFlagAutoConfiguration.class,
    beforeName = "net.brightroom.featureflag.webmvc.autoconfigure.FeatureFlagMvcAutoConfiguration")
@ConditionalOnClass(Endpoint.class)
public class FeatureFlagActuatorAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(
      value = {
        MutableFeatureFlagProvider.class,
        net.brightroom.featureflag.core.provider.FeatureFlagProvider.class
      })
  InMemoryMutableFeatureFlagProvider inMemoryMutableFeatureFlagProvider(
      FeatureFlagProperties props, ApplicationEventPublisher publisher) {
    return new InMemoryMutableFeatureFlagProvider(
        props.featureNames(), props.defaultEnabled(), publisher);
  }

  @Bean
  @ConditionalOnMissingBean
  @ConditionalOnAvailableEndpoint
  FeatureFlagEndpoint featureFlagEndpoint(MutableFeatureFlagProvider provider) {
    return new FeatureFlagEndpoint(provider);
  }
}
