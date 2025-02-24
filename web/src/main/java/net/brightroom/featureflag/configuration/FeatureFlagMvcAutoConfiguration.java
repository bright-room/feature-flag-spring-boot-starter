package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.interceptor.FeatureFlagInterceptor;
import net.brightroom.featureflag.provider.FeatureFlagProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * Autoconfiguration for Feature Flag MVC support. Configures the Feature Flag interceptor for
 * Spring MVC applications. This configuration is loaded after the main Feature Flag
 * autoconfiguration.
 */
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
public class FeatureFlagMvcAutoConfiguration {

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(FeatureFlagProvider featureFlagProvider) {
    return new FeatureFlagInterceptor(featureFlagProvider);
  }

  FeatureFlagMvcAutoConfiguration() {}
}
