package net.brightroom.featureflag.webmvc.autoconfigure;

import net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Auto-configuration that registers {@link
 * net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor} with the Spring MVC
 * interceptor registry for all request paths ({@code /**}).
 *
 * <p>This configuration runs after {@link FeatureFlagMvcAutoConfiguration} to ensure the
 * interceptor bean is available before registration.
 */
@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
public class FeatureFlagMvcInterceptorRegistrationAutoConfiguration {

  /** Creates a new {@link FeatureFlagMvcInterceptorRegistrationAutoConfiguration}. */
  FeatureFlagMvcInterceptorRegistrationAutoConfiguration() {}

  @Configuration(proxyBeanMethods = false)
  static class FeatureFlagMvcInterceptorRegistrationConfiguration implements WebMvcConfigurer {

    private final FeatureFlagInterceptor featureFlagInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(featureFlagInterceptor).addPathPatterns("/**");
    }

    FeatureFlagMvcInterceptorRegistrationConfiguration(
        FeatureFlagInterceptor featureFlagInterceptor) {
      this.featureFlagInterceptor = featureFlagInterceptor;
    }
  }
}
