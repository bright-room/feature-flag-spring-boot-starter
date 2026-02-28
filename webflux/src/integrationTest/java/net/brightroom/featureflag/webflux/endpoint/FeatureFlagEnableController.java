package net.brightroom.featureflag.webflux.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@FeatureFlag("enable-class-level-feature")
public class FeatureFlagEnableController {

  @GetMapping("/test/enabled")
  Mono<String> testEnabled() {
    return Mono.just("Allowed");
  }
}
