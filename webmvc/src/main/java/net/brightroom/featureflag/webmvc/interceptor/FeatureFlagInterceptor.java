package net.brightroom.featureflag.webmvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.evaluation.AccessDecision;
import net.brightroom.featureflag.core.evaluation.EvaluationContext;
import net.brightroom.featureflag.core.evaluation.FeatureFlagEvaluationPipeline;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.ConditionProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.webmvc.condition.HttpServletConditionVariables;
import net.brightroom.featureflag.webmvc.context.FeatureFlagContextResolver;
import org.jspecify.annotations.NonNull;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Spring MVC interceptor that enforces feature flag access control on annotated controllers.
 *
 * <p>Checks the {@link net.brightroom.featureflag.core.annotation.FeatureFlag} annotation on the
 * handler method first, then on the handler class. Method-level annotations take priority over
 * class-level annotations. If the feature is disabled, a {@link
 * net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException} is thrown and handled
 * by {@link net.brightroom.featureflag.webmvc.exception.FeatureFlagExceptionHandler}.
 */
public class FeatureFlagInterceptor implements HandlerInterceptor {

  private final FeatureFlagEvaluationPipeline pipeline;
  private final RolloutPercentageProvider rolloutPercentageProvider;
  private final ConditionProvider conditionProvider;
  private final FeatureFlagContextResolver contextResolver;

  /**
   * Creates a new {@link FeatureFlagInterceptor}.
   *
   * @param pipeline the evaluation pipeline that performs all feature flag checks; must not be null
   * @param rolloutPercentageProvider the provider that supplies per-flag rollout percentages; must
   *     not be null
   * @param conditionProvider the provider that supplies per-flag condition expressions; must not be
   *     null
   * @param contextResolver the resolver used to obtain the feature flag context from the request;
   *     must not be null
   */
  public FeatureFlagInterceptor(
      FeatureFlagEvaluationPipeline pipeline,
      RolloutPercentageProvider rolloutPercentageProvider,
      ConditionProvider conditionProvider,
      FeatureFlagContextResolver contextResolver) {
    this.pipeline = pipeline;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.conditionProvider = conditionProvider;
    this.contextResolver = contextResolver;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    FeatureFlag annotation = resolveAnnotation(handlerMethod);
    if (annotation == null) {
      return true;
    }

    validateAnnotation(annotation);

    EvaluationContext context = buildContext(request, annotation);
    AccessDecision decision = pipeline.evaluate(context);

    if (decision instanceof AccessDecision.Denied denied) {
      throw new FeatureFlagAccessDeniedException(denied.featureName());
    }
    return true;
  }

  private FeatureFlag resolveAnnotation(HandlerMethod handlerMethod) {
    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (methodAnnotation != null) {
      return methodAnnotation;
    }
    return handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
  }

  private EvaluationContext buildContext(HttpServletRequest request, FeatureFlag annotation) {
    String featureName = annotation.value();
    String condition = conditionProvider.getCondition(featureName).orElse("");
    int rollout = rolloutPercentageProvider.getRolloutPercentage(featureName).orElse(100);
    return new EvaluationContext(
        featureName,
        condition,
        rollout,
        HttpServletConditionVariables.build(request),
        () -> contextResolver.resolve(request).orElse(null));
  }
}
