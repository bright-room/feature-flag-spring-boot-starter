package net.brightroom.featureflag.webflux.condition;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import net.brightroom.featureflag.core.condition.ConditionVariablesBuilder;
import org.springframework.http.server.reactive.ServerHttpRequest;

/**
 * Utility for building the condition variables map from a {@link ServerHttpRequest}.
 *
 * <p>Shared by {@link net.brightroom.featureflag.webflux.aspect.FeatureFlagAspect} and {@link
 * net.brightroom.featureflag.webflux.filter.FeatureFlagHandlerFilterFunction} to avoid duplication.
 * Key names are defined by {@link ConditionVariablesBuilder}.
 */
public final class ServerHttpConditionVariables {

  private ServerHttpConditionVariables() {}

  /**
   * Builds the condition variables map from the given request.
   *
   * @param request the incoming reactive HTTP request
   * @return a map of condition variables keyed by name
   */
  public static Map<String, Object> build(ServerHttpRequest request) {
    Map<String, String> headers = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
    request.getHeaders().forEach((name, values) -> headers.put(name, values.getFirst()));
    Map<String, String> params = new HashMap<>();
    request.getQueryParams().forEach((name, values) -> params.put(name, values.getFirst()));
    Map<String, String> cookies = new HashMap<>();
    request
        .getCookies()
        .forEach(
            (name, cookieList) -> {
              if (!cookieList.isEmpty()) cookies.put(name, cookieList.getFirst().getValue());
            });
    InetSocketAddress remoteAddr = request.getRemoteAddress();
    return new ConditionVariablesBuilder()
        .headers(headers)
        .params(params)
        .cookies(cookies)
        .path(request.getPath().value())
        .method(request.getMethod().name())
        .remoteAddress(remoteAddr != null ? remoteAddr.getHostString() : "")
        .build();
  }
}
