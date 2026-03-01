package net.brightroom.featureflag.webflux.provider;

import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableAutoConfiguration
@EnableConfigurationProperties(FeatureFlagProperties.class)
class ReactiveFeatureFlagProviderTestConfiguration {}
