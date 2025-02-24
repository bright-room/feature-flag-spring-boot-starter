package net.brightroom.featureflag.presentation.endpoint;

import net.brightroom.featureflag.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
class DummyController {

  @GetMapping("/exist-endpoint")
  Mono<DummyResponse> existEndpoint() {
    return Mono.just(new DummyResponse("kukv", "12345678"));
  }

  @FeatureFlag(feature = "new-api")
  @GetMapping("/new-endpoint")
  Mono<DummyResponse> newEndpoint() {
    return Mono.just(new DummyResponse("kukv", "12345678"));
  }

  @FeatureFlag(feature = "beta-feature")
  @GetMapping("/beta-endpoint")
  Mono<DummyResponse> betaEndpoint() {
    return Mono.just(new DummyResponse("kukv", "12345678"));
  }

  DummyController() {}
}
