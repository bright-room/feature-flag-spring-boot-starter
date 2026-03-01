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
 *
 * // Gradual rollout — allow only 30% of users
 * {@literal @}FeatureFlag(value = "new-checkout", rollout = 30)
 * public void newCheckout() {
 *     // Accessible to 30% of users based on a hash of the user identifier
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
   * Specifies the rollout percentage for gradual feature rollout (0–100).
   *
   * <p>The default value of {@code 100} means all users are granted access when the feature flag is
   * enabled, which preserves backward-compatible behavior.
   *
   * <p>When set to a value between {@code 1} and {@code 99}, access is determined by a
   * deterministic, hash-based bucket assignment using the user identifier resolved by {@code
   * FeatureFlagContextResolver}. The same user will always receive the same result for a given
   * feature (sticky rollout).
   *
   * <ul>
   *   <li>{@code 0} — no users are granted access (feature effectively disabled for all).
   *   <li>{@code 1}–{@code 99} — the specified percentage of users are granted access.
   *   <li>{@code 100} — all users are granted access (default behavior).
   * </ul>
   *
   * <p>Values outside the range {@code 0}–{@code 100} are not permitted and will cause an {@link
   * IllegalStateException} to be thrown at request time by the interceptor.
   *
   * <p>Note: rollout evaluation only occurs when the underlying feature flag is enabled. If the
   * feature is disabled via {@code FeatureFlagProvider}, access is always denied regardless of
   * {@code rollout}.
   *
   * @return the rollout percentage; must be between 0 and 100 inclusive
   */
  int rollout() default 100;
}
