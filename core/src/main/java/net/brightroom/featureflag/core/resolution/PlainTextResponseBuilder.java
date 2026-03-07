package net.brightroom.featureflag.core.resolution;

import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;

/** Utility for building plain-text 403 responses for feature flag access denial. */
public final class PlainTextResponseBuilder {

  private PlainTextResponseBuilder() {}

  /**
   * Builds a plain-text response body for a denied feature flag access.
   *
   * @param e the exception that triggered the denial
   * @return the exception message as a plain-text string
   */
  public static String build(FeatureFlagAccessDeniedException e) {
    return e.getMessage();
  }
}
