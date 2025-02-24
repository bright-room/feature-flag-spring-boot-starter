package net.brightroom.featureflag.annotation;

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
 * <pre>
 * // Method level
 * {@literal @}FeatureFlag(feature = "new-api")
 * public void newFeature() {
 *     // This method will only be accessible if "new-api" feature is enabled
 * }
 *
 * // Class level
 * {@literal @}FeatureFlag(feature = "beta-features")
 * public class BetaController {
 *     // All methods in this class will only be accessible if "beta-features" is enabled
 * }
 * </pre>
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface FeatureFlag {

  /**
   * The name of the feature to check. This name will be used to look up the feature's
   * enabled/disabled status.
   *
   * @return the feature name
   */
  String feature() default "";

  /**
   * Indicates whether the feature flag check is required. If set to true (default), access will be
   * denied when the feature is disabled. If set to false, access will be allowed even if the
   * feature is disabled.
   *
   * @return true if the feature check is required, false otherwise
   */
  boolean required() default true;
}
