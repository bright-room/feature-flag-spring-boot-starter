package net.brightroom.featureflag.core.resolution;

import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Utility class for building a {@link ProblemDetail} response for feature flag access denials. */
public final class ProblemDetailBuilder {

  /**
   * Builds a {@link ProblemDetail} for a feature flag access denial.
   *
   * @param requestPath the request path to use as the {@code instance} URI
   * @param e the exception that triggered the denial
   * @return a populated {@link ProblemDetail} with status 403 Forbidden
   */
  public static ProblemDetail build(String requestPath, FeatureFlagAccessDeniedException e) {
    ProblemDetail problemDetail = ProblemDetail.forStatus(HttpStatus.FORBIDDEN);
    problemDetail.setType(
        URI.create(
            "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"));
    problemDetail.setTitle("Feature flag access denied");
    problemDetail.setDetail(e.getMessage());
    problemDetail.setInstance(URI.create(requestPath));
    return problemDetail;
  }

  private ProblemDetailBuilder() {}
}
