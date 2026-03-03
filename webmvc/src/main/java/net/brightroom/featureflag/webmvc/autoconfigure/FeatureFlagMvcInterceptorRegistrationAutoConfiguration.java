package net.brightroom.featureflag.webmvc.autoconfigure;

import net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
public class FeatureFlagMvcInterceptorRegistrationAutoConfiguration {

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
