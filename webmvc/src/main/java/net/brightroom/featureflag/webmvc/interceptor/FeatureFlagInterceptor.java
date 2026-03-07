package net.brightroom.featureflag.webmvc.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import net.brightroom.featureflag.core.annotation.FeatureFlag;
import net.brightroom.featureflag.core.condition.FeatureFlagConditionEvaluator;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.provider.FeatureFlagProvider;
import net.brightroom.featureflag.core.provider.RolloutPercentageProvider;
import net.brightroom.featureflag.core.rollout.RolloutStrategy;
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
  private final FeatureFlagProvider featureFlagProvider;
  private final RolloutStrategy rolloutStrategy;
  private final FeatureFlagContextResolver contextResolver;
  private final RolloutPercentageProvider rolloutPercentageProvider;
  private final FeatureFlagConditionEvaluator conditionEvaluator;

  /**
   * Creates a new {@link FeatureFlagInterceptor}.
   *
   * @param featureFlagProvider the provider used to check whether a feature flag is enabled; must
   *     not be null
   * @param rolloutStrategy the strategy used to determine rollout bucket membership; must not be
   *     null
   * @param contextResolver the resolver used to obtain the feature flag context from the request;
   *     must not be null
   * @param rolloutPercentageProvider the provider that supplies per-flag rollout percentages,
   *     overriding annotation-level values when present; must not be null
   * @param conditionEvaluator the evaluator used to evaluate SpEL condition expressions; must not
   *     be null
   */
  public FeatureFlagInterceptor(
      FeatureFlagProvider featureFlagProvider,
      RolloutStrategy rolloutStrategy,
      FeatureFlagContextResolver contextResolver,
      RolloutPercentageProvider rolloutPercentageProvider,
      FeatureFlagConditionEvaluator conditionEvaluator) {
    this.featureFlagProvider = featureFlagProvider;
    this.rolloutStrategy = rolloutStrategy;
    this.contextResolver = contextResolver;
    this.rolloutPercentageProvider = rolloutPercentageProvider;
    this.conditionEvaluator = conditionEvaluator;
  }

  @Override
  public boolean preHandle(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull Object handler) {
    if (!(handler instanceof HandlerMethod handlerMethod)) {
      return true;
    }

    FeatureFlag methodAnnotation = handlerMethod.getMethodAnnotation(FeatureFlag.class);
    if (methodAnnotation != null) {
      validateAnnotation(methodAnnotation);
      if (checkFeatureFlag(methodAnnotation)) {
        throw new FeatureFlagAccessDeniedException(methodAnnotation.value());
      }
      checkCondition(request, methodAnnotation);
      checkRollout(request, methodAnnotation);
      return true;
    }

    FeatureFlag classAnnotation = handlerMethod.getBeanType().getAnnotation(FeatureFlag.class);
    if (classAnnotation == null) {
      return true;
    }
    validateAnnotation(classAnnotation);
    if (checkFeatureFlag(classAnnotation)) {
      throw new FeatureFlagAccessDeniedException(classAnnotation.value());
    }
    checkCondition(request, classAnnotation);
    checkRollout(request, classAnnotation);

    return true;
  }

  private void validateAnnotation(FeatureFlag annotation) {
    if (annotation.value().isEmpty()) {
      throw new IllegalStateException(
          "@FeatureFlag must specify a non-empty value. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    if (annotation.rollout() < 0 || annotation.rollout() > 100) {
      throw new IllegalStateException(
          "@FeatureFlag rollout must be between 0 and 100, but was: " + annotation.rollout());
    }
  }

  private boolean checkFeatureFlag(FeatureFlag annotation) {
    return !featureFlagProvider.isFeatureEnabled(annotation.value());
  }

  private void checkCondition(HttpServletRequest request, FeatureFlag annotation) {
    String condition = annotation.condition();
    if (condition.isEmpty()) {
      return;
    }
    Map<String, Object> variables = buildConditionVariables(request);
    if (!conditionEvaluator.evaluate(condition, variables)) {
      throw new FeatureFlagAccessDeniedException(annotation.value());
    }
  }

  private Map<String, Object> buildConditionVariables(HttpServletRequest request) {
    Map<String, Object> variables = new HashMap<>();
    Map<String, String> headers = new HashMap<>();
    Collections.list(request.getHeaderNames())
        .forEach(name -> headers.put(name, request.getHeader(name)));
    variables.put("headers", headers);
    Map<String, String> params = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
    variables.put("params", params);
    Map<String, String> cookies = new HashMap<>();
    if (request.getCookies() != null) {
      Arrays.stream(request.getCookies()).forEach(c -> cookies.put(c.getName(), c.getValue()));
    }
    variables.put("cookies", cookies);
    variables.put("path", request.getRequestURI());
    variables.put("method", request.getMethod());
    variables.put("remoteAddress", request.getRemoteAddr());
    return variables;
  }

  private void checkRollout(HttpServletRequest request, FeatureFlag annotation) {
    int rollout =
        rolloutPercentageProvider
            .getRolloutPercentage(annotation.value())
            .orElse(annotation.rollout());
    if (rollout >= 100) return;
    Optional<FeatureFlagContext> context = contextResolver.resolve(request);
    if (context.isPresent()
        && !rolloutStrategy.isInRollout(annotation.value(), context.get(), rollout)) {
      throw new FeatureFlagAccessDeniedException(annotation.value());
    }
  }
}
