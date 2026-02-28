package net.brightroom.featureflag.webflux.configuration;

import java.nio.charset.StandardCharsets;
import net.brightroom.featureflag.core.exception.FeatureFlagAccessDeniedException;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

class AccessDeniedWebFilterResolutionViaHtmlResponse implements AccessDeniedWebFilterResolution {

  @Override
  public Mono<Void> resolve(ServerWebExchange exchange, FeatureFlagAccessDeniedException e) {
    ServerHttpResponse response = exchange.getResponse();
    response.setStatusCode(HttpStatus.FORBIDDEN);
    response
        .getHeaders()
        .setContentType(new MediaType(MediaType.TEXT_HTML, StandardCharsets.UTF_8));

    String html = HtmlResponseBuilder.buildHtml(e);
    byte[] body = html.getBytes(StandardCharsets.UTF_8);
    DataBuffer buffer = response.bufferFactory().wrap(body);
    return response.writeWith(Mono.just(buffer));
  }

  AccessDeniedWebFilterResolutionViaHtmlResponse() {}
}
