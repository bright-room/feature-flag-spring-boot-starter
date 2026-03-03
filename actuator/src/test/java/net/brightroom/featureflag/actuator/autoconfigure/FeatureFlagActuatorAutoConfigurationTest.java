package net.brightroom.featureflag.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpoint;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

class FeatureFlagActuatorAutoConfigurationTest {

  private final ApplicationContextRunner contextRunner =
      new ApplicationContextRunner()
          .withConfiguration(
              AutoConfigurations.of(
                  FeatureFlagAutoConfiguration.class, FeatureFlagActuatorAutoConfiguration.class));

  @Test
  void endpointNotCreated_whenNonMutableProviderExists() {
    contextRunner
        .withBean(FeatureFlagProvider.class, () -> new InMemoryFeatureFlagProvider(Map.of(), false))
        .run(
            context -> {
              assertThat(context).doesNotHaveBean(FeatureFlagEndpoint.class);
              assertThat(context).doesNotHaveBean(MutableInMemoryFeatureFlagProvider.class);
            });
  }

  @Test
  void customMutableProvider_usedByEndpoint_andDefaultNotRegistered() {
    var customProvider = new StubMutableFeatureFlagProvider();
    contextRunner
        .withBean(MutableFeatureFlagProvider.class, () -> customProvider)
        .run(
            context -> {
              assertThat(context).hasSingleBean(FeatureFlagEndpoint.class);
              assertThat(context).doesNotHaveBean(MutableInMemoryFeatureFlagProvider.class);
              assertThat(context.getBean(MutableFeatureFlagProvider.class))
                  .isSameAs(customProvider);
            });
  }

  static class StubMutableFeatureFlagProvider implements MutableFeatureFlagProvider {

    private final Map<String, Boolean> store = new ConcurrentHashMap<>();

    @Override
    public boolean isFeatureEnabled(String featureName) {
      return store.getOrDefault(featureName, false);
    }

    @Override
    public Map<String, Boolean> getFeatures() {
      return Map.copyOf(store);
    }

    @Override
    public void setFeatureEnabled(String featureName, boolean enabled) {
      store.put(featureName, enabled);
    }
  }
}
