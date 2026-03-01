package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagPathPatterns;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@AutoConfiguration(after = FeatureFlagMvcAutoConfiguration.class)
class FeatureFlagMvcInterceptorRegistrationAutoConfiguration {

  FeatureFlagMvcInterceptorRegistrationAutoConfiguration() {}

  @Configuration(proxyBeanMethods = false)
  static class FeatureFlagMvcInterceptorRegistrationConfiguration implements WebMvcConfigurer {

    private static final Logger log =
        LoggerFactory.getLogger(FeatureFlagMvcInterceptorRegistrationConfiguration.class);

    private final FeatureFlagInterceptor featureFlagInterceptor;
    private final FeatureFlagProperties featureFlagProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
      registry.addInterceptor(featureFlagInterceptor).addPathPatterns("/**");

      @SuppressWarnings("deprecation")
      FeatureFlagPathPatterns featureFlagPathPatterns = featureFlagProperties.pathPatterns();

      if (featureFlagPathPatterns.hasIncludes() || featureFlagPathPatterns.hasExcludes()) {
        log.warn(
            "feature-flags.path-patterns is deprecated and has no effect. "
                + "The interceptor is now registered for all paths ('/**'). "
                + "Please remove path-patterns from your configuration.");
      }
    }

    FeatureFlagMvcInterceptorRegistrationConfiguration(
        FeatureFlagInterceptor featureFlagInterceptor,
        FeatureFlagProperties featureFlagProperties) {
      this.featureFlagInterceptor = featureFlagInterceptor;
      this.featureFlagProperties = featureFlagProperties;
    }
  }
}
