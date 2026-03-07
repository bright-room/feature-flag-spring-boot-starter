package net.brightroom.featureflag.core.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Feature flag annotation to control access to specific features. This annotation can be applied at
 * both method and class levels to manage feature availability.
 *
 * <p>Usage examples:
 *
 * <pre>{@code
 * // Method level
 * {@literal @}FeatureFlag(value = "new-api")
 * public void newFeature() {
 *     // This method will only be accessible if "new-api" feature is enabled
 * }
 *
 * // Class level
 * {@literal @}FeatureFlag(value = "beta-features")
 * public class BetaController {
 *     // All methods in this class will only be accessible if "beta-features" is enabled
 * }
 * }</pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureFlag {

  /**
   * Specifies the feature flag associated with a method or class. The value represents the unique
   * identifier of the feature flag that determines whether the annotated method or class is
   * accessible or enabled.
   *
   * <p>This element is required. {@code @FeatureFlag} without a value will result in a compile-time
   * error. An explicit empty string (e.g., {@code @FeatureFlag("")}) is also not permitted and will
   * cause an {@link IllegalStateException} to be thrown at request time by the interceptor.
   *
   * @return the identifier of the feature flag; must be a non-empty string
   */
  String value();

  /**
   * SpEL condition expression evaluated against request context.
   *
   * <p>When non-empty, the feature is enabled only if the expression evaluates to {@code true}.
   *
   * <p>Available variables:
   *
   * <ul>
   *   <li>{@code headers} — request headers as {@code Map<String, String>}
   *   <li>{@code params} — query parameters as {@code Map<String, String>}
   *   <li>{@code cookies} — cookies as {@code Map<String, String>}
   *   <li>{@code path} — request path as {@code String}
   *   <li>{@code method} — HTTP method as {@code String}
   *   <li>{@code remoteAddress} — client IP as {@code String}
   * </ul>
   *
   * <p>Example: {@code @FeatureFlag(value = "beta", condition = "headers['X-Beta'] != null")}
   *
   * @return SpEL expression; empty string (default) means no condition
   */
  String condition() default "";

  /**
   * Rollout percentage (0–100). 100 means fully enabled (default).
   *
   * <p>When less than 100, the feature is enabled only for a percentage of requests (or users, if a
   * sticky {@code FeatureFlagContextResolver} is provided). When 0, the feature is effectively
   * disabled for all requests even if the flag itself is enabled.
   *
   * @return the rollout percentage; must be between 0 and 100 inclusive
   */
  int rollout() default 100;
}
