package net.brightroom.featureflag.actuator.configuration;

import net.brightroom.featureflag.actuator.autoconfigure.FeatureFlagActuatorAutoConfiguration;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Test auto-configuration for the actuator module integration tests.
 *
 * <p>Mirrors the pattern of {@code FeatureFlagMvcTestAutoConfiguration} in the webmvc module.
 * Explicitly imports the core and actuator auto-configurations to ensure they are loaded even when
 * the auto-configuration scanning order changes.
 */
@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
@Import({FeatureFlagAutoConfiguration.class, FeatureFlagActuatorAutoConfiguration.class})
public class FeatureFlagActuatorTestAutoConfiguration {}
