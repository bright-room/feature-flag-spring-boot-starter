package net.brightroom.featureflag.core.condition;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

/**
 * Immutable holder for request context variables available in SpEL condition expressions.
 *
 * <p>Instances are created via {@link ConditionVariablesBuilder}.
 *
 * <p>Available properties:
 *
 * <ul>
 *   <li>{@code headers} — {@code Map<String, String>} of request headers (first value per name,
 *       case-insensitive keys)
 *   <li>{@code params} — {@code Map<String, String>} of query parameters (first value per name)
 *   <li>{@code cookies} — {@code Map<String, String>} of cookie values keyed by cookie name
 *   <li>{@code path} — {@code String} request path (e.g. {@code /api/resource})
 *   <li>{@code method} — {@code String} HTTP method (e.g. {@code GET}, {@code POST})
 *   <li>{@code remoteAddress} — {@code String} client IP address
 * </ul>
 *
 * @param headers request headers as {@code Map<String, String>}
 * @param params query parameters as {@code Map<String, String>}
 * @param cookies cookies as {@code Map<String, String>}
 * @param path request path
 * @param method HTTP method
 * @param remoteAddress client IP address
 */
public record ConditionVariables(
    Map<String, String> headers,
    Map<String, String> params,
    Map<String, String> cookies,
    String path,
    String method,
    String remoteAddress) {

  public ConditionVariables {
    TreeMap<String, String> headersCopy = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    if (headers != null) {
      headersCopy.putAll(headers);
    }
    headers = Collections.unmodifiableMap(headersCopy);
    params = params != null ? Map.copyOf(params) : Map.of();
    cookies = cookies != null ? Map.copyOf(cookies) : Map.of();
    path = path != null ? path : "";
    method = method != null ? method : "";
    remoteAddress = remoteAddress != null ? remoteAddress : "";
  }
}
