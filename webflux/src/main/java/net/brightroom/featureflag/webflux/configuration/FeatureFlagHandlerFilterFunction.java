package net.brightroom.featureflag.webflux.configuration;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.configuration.FeatureFlagProperties;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.webflux.provider.ReactiveFeatureFlagProvider;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.util.HtmlUtils;
import reactor.core.publisher.Mono;

/**
 * A factory for {@link HandlerFilterFunction} that applies feature flag access control to
 * Functional Endpoints.
 *
 * <p>Use {@link #of(String)} to create a {@link HandlerFilterFunction} for a specific feature name
 * and apply it to a {@link org.springframework.web.reactive.function.server.RouterFunction}:
 *
 * <pre>{@code
 * @Bean
 * RouterFunction<ServerResponse> routes(FeatureFlagHandlerFilterFunction featureFlagFilter) {
 *     return route()
 *         .GET("/api/feature", handler::handle)
 *         .filter(featureFlagFilter.of("my-feature"))
 *         .build();
 * }
 * }</pre>
 *
 * <p>When the feature is disabled, the filter returns a {@code 403 Forbidden} response directly
 * without invoking the handler. The response format follows {@code feature-flags.response.type}
 * configuration.
 */
public class FeatureFlagHandlerFilterFunction {

  private final ReactiveFeatureFlagProvider reactiveFeatureFlagProvider;
  private final FeatureFlagProperties featureFlagProperties;

  /**
   * Creates a {@link HandlerFilterFunction} that guards the route with the specified feature flag.
   *
   * @param featureName the name of the feature flag to check; must not be empty
   * @return a {@link HandlerFilterFunction} that allows or denies access based on the feature flag
   * @throws IllegalArgumentException if {@code featureName} is empty
   */
  public HandlerFilterFunction<ServerResponse, ServerResponse> of(String featureName) {
    if (featureName.isEmpty()) {
      throw new IllegalArgumentException(
          "featureName must not be empty. "
              + "An empty value causes fail-open behavior and allows access unconditionally.");
    }
    return (request, next) ->
        reactiveFeatureFlagProvider
            .isFeatureEnabled(featureName)
            .flatMap(enabled -> filterByFeatureEnabled(request, next, featureName, enabled));
  }

  private Mono<ServerResponse> filterByFeatureEnabled(
      ServerRequest request,
      HandlerFunction<ServerResponse> next,
      String featureName,
      boolean enabled) {
    if (enabled) {
      return next.handle(request);
    }
    return buildDeniedResponse(request, new FeatureFlagAccessDeniedException(featureName));
  }

  private Mono<ServerResponse> buildDeniedResponse(
      ServerRequest request, FeatureFlagAccessDeniedException e) {
    return switch (featureFlagProperties.response().type()) {
      case JSON -> buildJsonResponse(request, e);
      case PLAIN_TEXT -> buildPlainTextResponse(e);
      case HTML -> buildHtmlResponse(e);
    };
  }

  private Mono<ServerResponse> buildJsonResponse(
      ServerRequest request, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create(
            "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(request.path()));

    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(MediaType.APPLICATION_PROBLEM_JSON)
        .bodyValue(problemDetail);
  }

  private Mono<ServerResponse> buildPlainTextResponse(FeatureFlagAccessDeniedException e) {
    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8))
        .bodyValue(e.getMessage());
  }

  private Mono<ServerResponse> buildHtmlResponse(FeatureFlagAccessDeniedException e) {
    String escapedMessage = HtmlUtils.htmlEscape(e.getMessage());
    String html =
        """
        <!DOCTYPE html>
        <html lang="en">
        <head>
          <meta charset="UTF-8">
          <title>Access Denied</title>
        </head>
        <body>
          <h1>403 - Access Denied</h1>
          <p>%s</p>
        </body>
        </html>
        """
            .formatted(escapedMessage);

    return ServerResponse.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .bodyValue(html);
  }

  FeatureFlagHandlerFilterFunction(
      ReactiveFeatureFlagProvider reactiveFeatureFlagProvider,
      FeatureFlagProperties featureFlagProperties) {
    this.reactiveFeatureFlagProvider = reactiveFeatureFlagProvider;
    this.featureFlagProperties = featureFlagProperties;
  }
}
