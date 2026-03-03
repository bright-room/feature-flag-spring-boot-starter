package net.brightroom.featureflag.webflux.rollout;

import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.junit.jupiter.api.Test;
import reactor.test.StepVerifier;

class DefaultReactiveRolloutStrategyTest {

  private final DefaultReactiveRolloutStrategy strategy = new DefaultReactiveRolloutStrategy();

  @Test
  void isInRollout_returnsTrue_whenRolloutIs100() {
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    StepVerifier.create(strategy.isInRollout("my-feature", context, 100))
        .expectNext(true)
        .verifyComplete();
  }

  @Test
  void isInRollout_returnsFalse_whenRolloutIs0() {
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    StepVerifier.create(strategy.isInRollout("my-feature", context, 0))
        .expectNext(false)
        .verifyComplete();
  }

  @Test
  void isInRollout_isDeterministic_forSameInput() {
    FeatureFlagContext context = new FeatureFlagContext("user-1");
    Boolean first = strategy.isInRollout("my-feature", context, 50).block();
    Boolean second = strategy.isInRollout("my-feature", context, 50).block();
    org.assertj.core.api.Assertions.assertThat(first).isEqualTo(second);
  }
}
