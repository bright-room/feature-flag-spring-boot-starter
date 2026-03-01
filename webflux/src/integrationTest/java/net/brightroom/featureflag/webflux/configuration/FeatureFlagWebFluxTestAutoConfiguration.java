package net.brightroom.featureflag.webflux.configuration;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test auto-configuration for the webflux module.
 *
 * <p>{@code @EnableAutoConfiguration} loads {@code FeatureFlagAutoConfiguration} from the core
 * module via {@code
 * META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}. It also loads
 * the webflux auto-configuration through the same mechanism, so the {@code @Import} below results
 * in the webflux auto-configuration being registered twice. Spring handles this gracefully by
 * deduplicating bean definitions with the same name.
 *
 * <p>{@code @Import} is kept here to make the dependency on the webflux auto-configuration explicit
 * and to ensure it is loaded even if the {@code @EnableAutoConfiguration} scanning order changes in
 * future Spring Boot versions.
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import(FeatureFlagWebFluxAutoConfiguration.class)
public class FeatureFlagWebFluxTestAutoConfiguration {}
