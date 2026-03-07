package net.brightroom.featureflag.webmvc.condition;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.brightroom.featureflag.core.condition.ConditionVariablesBuilder;

/**
 * Utility for building the condition variables map from an {@link HttpServletRequest}.
 *
 * <p>Shared by {@link net.brightroom.featureflag.webmvc.interceptor.FeatureFlagInterceptor} and
 * {@link net.brightroom.featureflag.webmvc.filter.FeatureFlagHandlerFilterFunction} to avoid
 * duplication. Key names are defined by {@link ConditionVariablesBuilder}.
 */
public final class HttpServletConditionVariables {

  private HttpServletConditionVariables() {}

  /**
   * Builds the condition variables map from the given request.
   *
   * @param request the incoming HTTP servlet request
   * @return a map of condition variables keyed by name
   */
  public static Map<String, Object> build(HttpServletRequest request) {
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    Collections.list(request.getHeaderNames())
        .forEach(name -> headers.put(name, request.getHeader(name)));
    Map<String, String> params = new HashMap<>();
    request.getParameterMap().forEach((k, v) -> params.put(k, v.length > 0 ? v[0] : ""));
    Map<String, String> cookies = new HashMap<>();
    if (request.getCookies() != null) {
      Arrays.stream(request.getCookies()).forEach(c -> cookies.put(c.getName(), c.getValue()));
    }
    return new ConditionVariablesBuilder()
        .headers(headers)
        .params(params)
        .cookies(cookies)
        .path(request.getRequestURI())
        .method(request.getMethod())
        .remoteAddress(request.getRemoteAddr())
        .build();
  }
}
