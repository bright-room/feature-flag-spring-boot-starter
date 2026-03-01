package net.brightroom.featureflag.webflux.endpoint;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
public class NoFeatureFlagController {

  @GetMapping("/test/no-annotation")
  Mono<String> noAnnotation() {
    return Mono.just("No Annotation");
  }

  public NoFeatureFlagController() {}
}
