package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.interceptor.FeatureFlagInterceptor;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import net.brightroom.featureflag.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.response.AccessDeniedResponse;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Feature Flag MVC support. Configures the Feature Flag interceptor for
 * Spring MVC applications. This configuration is loaded after the main Feature Flag
 * autoconfiguration.
 */
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
public class FeatureFlagMvcAutoConfiguration {

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider(FeatureFlagProperties properties) {
    return new InMemoryFeatureFlagProvider(properties.features());
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider, AccessDeniedResponse.Builder builder) {
    return new FeatureFlagInterceptor(featureFlagProvider, builder);
  }

  FeatureFlagMvcAutoConfiguration() {}
}
