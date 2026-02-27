package net.brightroom.featureflag.webmvc.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test auto-configuration for the webmvc module.
 *
 * <p>{@code @EnableAutoConfiguration} loads {@code FeatureFlagAutoConfiguration} from the core
 * module via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. It also loads
 * the webmvc auto-configurations through the same mechanism, so the {@code @Import} below results
 * in the webmvc auto-configurations being registered twice. Spring handles this gracefully by
 * deduplicating bean definitions with the same name.
 *
 * <p>{@code @Import} is kept here to make the dependency on the webmvc auto-configurations explicit
 * and to ensure they are loaded even if the {@code @EnableAutoConfiguration} scanning order changes
 * in future Spring Boot versions.
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import({
  FeatureFlagMvcAutoConfiguration.class,
  FeatureFlagMvcInterceptorRegistrationAutoConfiguration.class
})
public class FeatureFlagMvcTestAutoConfiguration {}
