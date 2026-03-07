package net.brightroom.featureflag.core.condition;

import java.util.Map;

/**
 * Immutable holder for request context variables available in SpEL condition expressions.
 *
 * <p>Instances are created via {@link ConditionVariablesBuilder}.
 *
 * <p>Available properties:
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
public final class ConditionVariables {

  private final Map<String, String> headers;
  private final Map<String, String> params;
  private final Map<String, String> cookies;
  private final String path;
  private final String method;
  private final String remoteAddress;

  ConditionVariables(
      Map<String, String> headers,
      Map<String, String> params,
      Map<String, String> cookies,
      String path,
      String method,
      String remoteAddress) {
    this.headers = headers;
    this.params = params;
    this.cookies = cookies;
    this.path = path;
    this.method = method;
    this.remoteAddress = remoteAddress;
  }

  public Map<String, String> getHeaders() {
    return headers;
  }

  public Map<String, String> getParams() {
    return params;
  }

  public Map<String, String> getCookies() {
    return cookies;
  }

  public String getPath() {
    return path;
  }

  public String getMethod() {
    return method;
  }

  public String getRemoteAddress() {
    return remoteAddress;
  }
}
