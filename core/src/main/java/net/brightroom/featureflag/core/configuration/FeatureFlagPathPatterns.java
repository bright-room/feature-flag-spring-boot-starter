package net.brightroom.featureflag.core.configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Path patterns for feature flag interceptor registration.
 *
 * <p>This class defines which request paths should be included or excluded when registering the
 * feature flag interceptor.
 *
 * @deprecated {@code feature-flags.path-patterns} has no effect as of this version. The interceptor
 *     is now registered for all paths ({@code /**}) unconditionally. Remove {@code path-patterns}
 *     from your configuration. This class will be removed in the next major version.
 */
@Deprecated
public class FeatureFlagPathPatterns {

  /** The list of path patterns to include. */
  private List<String> includes = new ArrayList<>();

  /** The list of path patterns to exclude. */
  private List<String> excludes = new ArrayList<>();

  /**
   * Returns whether the include path patterns are not empty.
   *
   * @return true if include path patterns are not empty
   */
  public boolean hasIncludes() {
    return !includes.isEmpty();
  }

  /**
   * Returns whether the exclude path patterns are not empty.
   *
   * @return true if exclude path patterns are not empty
   */
  public boolean hasExcludes() {
    return !excludes.isEmpty();
  }

  /**
   * Returns an unmodifiable copy of the include path patterns.
   *
   * @return the list of include path patterns
   */
  public List<String> includes() {
    return List.copyOf(includes);
  }

  /**
   * Returns an unmodifiable copy of the exclude path patterns.
   *
   * @return the list of exclude path patterns
   */
  public List<String> excludes() {
    return List.copyOf(excludes);
  }

  void setIncludes(List<String> includes) {
    this.includes = includes;
  }

  void setExcludes(List<String> excludes) {
    this.excludes = excludes;
  }

  FeatureFlagPathPatterns() {}
}
