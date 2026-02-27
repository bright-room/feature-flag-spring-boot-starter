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
   * @return the identifier of the feature flag, defaulting to an empty string if not specified
   */
  String value() default "";
}
