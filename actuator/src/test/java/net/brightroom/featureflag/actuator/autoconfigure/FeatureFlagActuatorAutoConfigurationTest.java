package net.brightroom.featureflag.actuator.autoconfigure;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.brightroom.featureflag.actuator.endpoint.FeatureFlagEndpoint;
import net.brightroom.featureflag.actuator.endpoint.ReactiveFeatureFlagEndpoint;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableInMemoryReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.MutableReactiveFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.ReactiveFeatureFlagProvider;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ReactiveWebApplicationContextRunner;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import reactor.core.publisher.Mono;

class FeatureFlagActuatorAutoConfigurationTest {

  @Nested
  class ServletTests {

    private final WebApplicationContextRunner contextRunner =
        new WebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    FeatureFlagAutoConfiguration.class,
                    FeatureFlagActuatorAutoConfiguration.class));

    @Test
    void registersServletProviderAndEndpoint() {
      contextRunner.run(
          context -> {
            assertThat(context).hasSingleBean(MutableInMemoryFeatureFlagProvider.class);
            assertThat(context).hasSingleBean(FeatureFlagEndpoint.class);
            assertThat(context).doesNotHaveBean(ReactiveFeatureFlagEndpoint.class);
          });
    }

    @Test
    void endpointNotCreated_whenNonMutableProviderExists() {
      contextRunner
          .withBean(
              FeatureFlagProvider.class, () -> new InMemoryFeatureFlagProvider(Map.of(), false))
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
  }

  @Nested
  class ReactiveTests {

    private final ReactiveWebApplicationContextRunner contextRunner =
        new ReactiveWebApplicationContextRunner()
            .withConfiguration(
                AutoConfigurations.of(
                    FeatureFlagAutoConfiguration.class,
                    FeatureFlagActuatorAutoConfiguration.class));

    @Test
    void registersReactiveProviderAndEndpoint() {
      contextRunner.run(
          context -> {
            assertThat(context).hasSingleBean(MutableInMemoryReactiveFeatureFlagProvider.class);
            assertThat(context).hasSingleBean(ReactiveFeatureFlagEndpoint.class);
            assertThat(context).doesNotHaveBean(FeatureFlagEndpoint.class);
          });
    }

    @Test
    void endpointNotCreated_whenCustomReactiveProviderExists() {
      contextRunner
          .withBean(ReactiveFeatureFlagProvider.class, () -> featureName -> Mono.just(false))
          .run(
              context -> {
                assertThat(context).doesNotHaveBean(ReactiveFeatureFlagEndpoint.class);
                assertThat(context)
                    .doesNotHaveBean(MutableInMemoryReactiveFeatureFlagProvider.class);
              });
    }

    @Test
    void customMutableReactiveProvider_usedByEndpoint_andDefaultNotRegistered() {
      var customProvider = new StubMutableReactiveFeatureFlagProvider();
      contextRunner
          .withBean(MutableReactiveFeatureFlagProvider.class, () -> customProvider)
          .run(
              context -> {
                assertThat(context).hasSingleBean(ReactiveFeatureFlagEndpoint.class);
                assertThat(context)
                    .doesNotHaveBean(MutableInMemoryReactiveFeatureFlagProvider.class);
                assertThat(context.getBean(MutableReactiveFeatureFlagProvider.class))
                    .isSameAs(customProvider);
              });
    }
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

  static class StubMutableReactiveFeatureFlagProvider
      implements MutableReactiveFeatureFlagProvider {

    private final Map<String, Boolean> store = new ConcurrentHashMap<>();

    @Override
    public Mono<Boolean> isFeatureEnabled(String featureName) {
      return Mono.just(store.getOrDefault(featureName, false));
    }

    @Override
    public Mono<Map<String, Boolean>> getFeatures() {
      return Mono.just(Map.copyOf(store));
    }

    @Override
    public Mono<Void> setFeatureEnabled(String featureName, boolean enabled) {
      store.put(featureName, enabled);
      return Mono.empty();
    }
  }
}
