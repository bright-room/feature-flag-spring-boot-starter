package net.brightroom.featureflag.presentation.endpoint;

import net.brightroom.featureflag.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
class DummyController {

  @GetMapping("/exist-endpoint")
  DummyResponse existEndpoint() {
    return new DummyResponse("kukv", "12345678");
  }

  @FeatureFlag(feature = "new-api")
  @GetMapping("/new-endpoint")
  DummyResponse newEndpoint() {
    return new DummyResponse("kukv", "09876543");
  }

  @FeatureFlag(feature = "beta-feature")
  @GetMapping("/beta-endpoint")
  DummyResponse betaEndpoint() {
    return new DummyResponse("kukv", "00000000");
  }

  DummyController() {}
}
