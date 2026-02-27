package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import({
  FeatureFlagMvcAutoConfiguration.class,
  FeatureFlagMvcInterceptorRegistrationAutoConfiguration.class
})
public class FeatureFlagMvcTestAutoConfiguration {}
