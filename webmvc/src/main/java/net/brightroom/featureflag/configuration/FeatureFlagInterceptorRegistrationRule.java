package net.brightroom.featureflag.configuration;

import java.util.ArrayList;
import java.util.List;

class FeatureFlagInterceptorRegistrationRule {

  List<String> includePathPattern = new ArrayList<>();
  List<String> excludePathPattern = new ArrayList<>();

  FeatureFlagInterceptorRegistrationRule(
      List<String> includePathPattern, List<String> excludePathPattern) {
    this.includePathPattern = includePathPattern;
    this.excludePathPattern = excludePathPattern;
  }

  boolean isNotEmptyIncludePathPattern() {
    return !includePathPattern.isEmpty();
  }

  boolean isNotEmptyExcludePathPattern() {
    return !excludePathPattern.isEmpty();
  }

  List<String> includePathPattern() {
    return includePathPattern;
  }

  List<String> excludePathPattern() {
    return excludePathPattern;
  }

  FeatureFlagInterceptorRegistrationRule() {}
}
