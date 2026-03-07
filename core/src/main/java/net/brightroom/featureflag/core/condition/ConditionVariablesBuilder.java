package net.brightroom.featureflag.core.condition;

import java.util.HashMap;
import java.util.Map;

/**
 * Builder for constructing the variables map passed to {@link FeatureFlagConditionEvaluator} and
 * {@link ReactiveFeatureFlagConditionEvaluator}.
 *
 * <p>Centralizes the variable key names so that all modules ({@code webmvc}, {@code webflux}) use
 * consistent keys. Each module extracts the request-specific data and delegates to this builder to
 * construct the final map.
 *
 * <p>Available variable keys:
 *
 * <ul>
 *   <li>{@code headers} — {@code Map<String, String>} of request headers (first value per name)
 *   <li>{@code params} — {@code Map<String, String>} of query parameters (first value per name)
 *   <li>{@code cookies} — {@code Map<String, String>} of cookie values keyed by cookie name
 *   <li>{@code path} — {@code String} request path (e.g. {@code /api/resource})
 *   <li>{@code method} — {@code String} HTTP method (e.g. {@code GET}, {@code POST})
 *   <li>{@code remoteAddress} — {@code String} client IP address
 * </ul>
 */
public final class ConditionVariablesBuilder {

  private final Map<String, Object> variables = new HashMap<>();

  /** Sets the {@code headers} variable. */
  public ConditionVariablesBuilder headers(Map<String, String> headers) {
    variables.put("headers", headers);
    return this;
  }

  /** Sets the {@code params} variable. */
  public ConditionVariablesBuilder params(Map<String, String> params) {
    variables.put("params", params);
    return this;
  }

  /** Sets the {@code cookies} variable. */
  public ConditionVariablesBuilder cookies(Map<String, String> cookies) {
    variables.put("cookies", cookies);
    return this;
  }

  /** Sets the {@code path} variable. */
  public ConditionVariablesBuilder path(String path) {
    variables.put("path", path);
    return this;
  }

  /** Sets the {@code method} variable. */
  public ConditionVariablesBuilder method(String method) {
    variables.put("method", method);
    return this;
  }

  /** Sets the {@code remoteAddress} variable. */
  public ConditionVariablesBuilder remoteAddress(String remoteAddress) {
    variables.put("remoteAddress", remoteAddress);
    return this;
  }

  /** Returns the built variables map. */
  public Map<String, Object> build() {
    return Map.copyOf(variables);
  }
}
