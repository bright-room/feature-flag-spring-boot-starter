package net.brightroom.featureflag.core.resolution;

import java.net.URI;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/** Utility for building RFC 7807 {@link ProblemDetail} responses for feature flag access denial. */
public final class ProblemDetailBuilder {

  private ProblemDetailBuilder() {}

  /**
   * Builds a {@link ProblemDetail} for a denied feature flag access.
   *
   * @param requestPath the path of the denied request
   * @param e the exception that triggered the denial
   * @return a populated {@link ProblemDetail} with status 403
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
}
