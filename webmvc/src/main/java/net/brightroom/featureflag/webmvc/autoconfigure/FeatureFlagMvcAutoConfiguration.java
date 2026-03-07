package net.brightroom.featureflag.webmvc.autoconfigure;

import java.time.Clock;
import java.util.List;
import net.brightroom.featureflag.core.autoconfigure.FeatureFlagAutoConfiguration;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.condition.SpelFeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.evaluation.ConditionEvaluationStep;
import net.brightroom.featureflag.core.evaluation.EnabledEvaluationStep;
import net.brightroom.featureflag.core.evaluation.EvaluationStep;
import net.brightroom.featureflag.core.evaluation.FeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.evaluation.RolloutEvaluationStep;
import net.brightroom.featureflag.core.evaluation.ScheduleEvaluationStep;
import net.brightroom.featureflag.core.properties.FeatureFlagProperties;
import net.brightroom.featureflag.core.provider.ConditionProvider;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryConditionProvider;
import net.brightroom.featureflag.core.provider.InMemoryFeatureFlagProvider;
import net.brightroom.featureflag.core.provider.InMemoryRolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.InMemoryScheduleProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.provider.ScheduleProvider;
import net.brightroom.featureflag.core.rollout.DefaultRolloutStrategy;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.context.RandomFeatureFlagContextResolver;
import net.brightroom.featureflag.webmvc.exception.FeatureFlagExceptionHandler;
import net.brightroom.featureflag.webmvc.filter.FeatureFlagHandlerFilterFunction;
import net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolution;
import net.brightroom.featureflag.webmvc.resolution.AccessDeniedInterceptResolutionFactory;
import net.brightroom.featureflag.webmvc.resolution.handlerfilter.AccessDeniedHandlerFilterResolution;
import net.brightroom.featureflag.webmvc.resolution.handlerfilter.AccessDeniedHandlerFilterResolutionFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;

/**
 * Auto-configuration for Spring MVC feature flag support.
 *
 * <p>Registers the core beans required for feature flag enforcement in servlet-based Spring MVC
 * applications, including {@link net.brightroom.featureflag.core.provider.FeatureFlagProvider},
 * {@link net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor}, {@link
 * net.brightroom.featureflag.webmvc.exception.FeatureFlagExceptionHandler}, and related resolution
 * and rollout beans. Beans annotated with {@code @ConditionalOnMissingBean} can be replaced by
 * user-defined beans.
 */
@AutoConfiguration(after = FeatureFlagAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class FeatureFlagMvcAutoConfiguration {

  private final FeatureFlagProperties featureFlagProperties;

  @Bean
  @ConditionalOnMissingBean(FeatureFlagProvider.class)
  FeatureFlagProvider featureFlagProvider() {
    return new InMemoryFeatureFlagProvider(
        featureFlagProperties.featureNames(), featureFlagProperties.defaultEnabled());
  }

  @Bean
  @ConditionalOnMissingBean
  AccessDeniedInterceptResolution featureFlagAccessDeniedResponse() {
    return new AccessDeniedInterceptResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean
  RolloutStrategy rolloutStrategy() {
    return new DefaultRolloutStrategy();
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagContextResolver featureFlagContextResolver() {
    return new RandomFeatureFlagContextResolver();
  }

  @Bean
  @ConditionalOnMissingBean(RolloutPercentageProvider.class)
  RolloutPercentageProvider rolloutPercentageProvider() {
    return new InMemoryRolloutPercentageProvider(featureFlagProperties.rolloutPercentages());
  }

  @Bean
  @ConditionalOnMissingBean(ConditionProvider.class)
  ConditionProvider conditionProvider() {
    return new InMemoryConditionProvider(featureFlagProperties.conditions());
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagConditionEvaluator featureFlagConditionEvaluator() {
    return new SpelFeatureFlagConditionEvaluator(featureFlagProperties.condition().failOnError());
  }

  @Bean
  @ConditionalOnMissingBean
  Clock featureFlagClock() {
    return Clock.systemDefaultZone();
  }

  @Bean
  @ConditionalOnMissingBean(ScheduleProvider.class)
  ScheduleProvider scheduleProvider() {
    return new InMemoryScheduleProvider(featureFlagProperties.schedules());
  }

  @Bean
  @ConditionalOnMissingBean(EnabledEvaluationStep.class)
  EnabledEvaluationStep enabledEvaluationStep(FeatureFlagProvider featureFlagProvider) {
    return new EnabledEvaluationStep(featureFlagProvider);
  }

  @Bean
  @ConditionalOnMissingBean(ScheduleEvaluationStep.class)
  ScheduleEvaluationStep scheduleEvaluationStep(ScheduleProvider scheduleProvider, Clock clock) {
    return new ScheduleEvaluationStep(scheduleProvider, clock);
  }

  @Bean
  @ConditionalOnMissingBean(ConditionEvaluationStep.class)
  ConditionEvaluationStep conditionEvaluationStep(
      FeatureFlagConditionEvaluator conditionEvaluator) {
    return new ConditionEvaluationStep(conditionEvaluator);
  }

  @Bean
  @ConditionalOnMissingBean(RolloutEvaluationStep.class)
  RolloutEvaluationStep rolloutEvaluationStep(RolloutStrategy rolloutStrategy) {
    return new RolloutEvaluationStep(rolloutStrategy);
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagEvaluationPipeline featureFlagEvaluationPipeline(List<EvaluationStep> steps) {
    return new FeatureFlagEvaluationPipeline(steps);
  }

  @Bean
  FeatureFlagInterceptor featureFlagInterceptor(
      FeatureFlagEvaluationPipeline pipeline,
      RolloutPercentageProvider rolloutPercentageProvider,
      ConditionProvider conditionProvider,
      FeatureFlagContextResolver contextResolver) {
    return new FeatureFlagInterceptor(
        pipeline, rolloutPercentageProvider, conditionProvider, contextResolver);
  }

  @Bean
  FeatureFlagExceptionHandler featureFlagExceptionHandler(
      AccessDeniedInterceptResolution accessDeniedInterceptResolution) {
    return new FeatureFlagExceptionHandler(accessDeniedInterceptResolution);
  }

  @Bean
  @ConditionalOnMissingBean(AccessDeniedHandlerFilterResolution.class)
  AccessDeniedHandlerFilterResolution accessDeniedHandlerFilterResolution() {
    return new AccessDeniedHandlerFilterResolutionFactory().create(featureFlagProperties);
  }

  @Bean
  @ConditionalOnMissingBean
  FeatureFlagHandlerFilterFunction featureFlagHandlerFilterFunction(
      FeatureFlagEvaluationPipeline pipeline,
      AccessDeniedHandlerFilterResolution accessDeniedHandlerFilterResolution,
      RolloutPercentageProvider rolloutPercentageProvider,
      ConditionProvider conditionProvider,
      FeatureFlagContextResolver contextResolver) {
    return new FeatureFlagHandlerFilterFunction(
        pipeline,
        accessDeniedHandlerFilterResolution,
        rolloutPercentageProvider,
        conditionProvider,
        contextResolver);
  }

  /**
   * Creates a new {@link FeatureFlagMvcAutoConfiguration}.
   *
   * @param featureFlagProperties the feature flag configuration properties; must not be null
   */
  FeatureFlagMvcAutoConfiguration(FeatureFlagProperties featureFlagProperties) {
    this.featureFlagProperties = featureFlagProperties;
  }
}
