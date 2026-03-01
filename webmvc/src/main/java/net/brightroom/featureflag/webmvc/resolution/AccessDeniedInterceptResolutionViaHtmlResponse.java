package net.brightroom.featureflag.webmvc.resolution;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import net.brightroom.featureflag.core.resolution.HtmlResponseBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

/**
 * An {@link AccessDeniedInterceptResolution} implementation that returns a fixed HTML response.
 *
 * <p>This implementation returns a {@code ResponseEntity<String>} with {@code text/html} content
 * type, which Spring MVC writes using {@code StringHttpMessageConverter}. The converter can only
 * write the response when the client's {@code Accept} header includes {@code text/html} or {@code
 * text/*}. If the client sends {@code Accept: application/json} only, Spring MVC will return {@code
 * 406 Not Acceptable} instead of the intended HTML response.
 *
 * <p>For full control over the denied response regardless of the {@code Accept} header, define a
 * custom {@code @ControllerAdvice} that handles {@link
 * net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException}.
 */
class AccessDeniedInterceptResolutionViaHtmlResponse implements AccessDeniedInterceptResolution {

  @Override
  public ResponseEntity<?> resolution(
      @SuppressWarnings("unused") HttpServletRequest request, FeatureFlagAccessDeniedException e) {
    String html = HtmlResponseBuilder.buildHtml(e);

    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .contentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8))
        .body(html);
  }

  AccessDeniedInterceptResolutionViaHtmlResponse() {}
}
