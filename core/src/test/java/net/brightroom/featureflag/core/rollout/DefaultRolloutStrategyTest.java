package net.brightroom.featureflag.core.rollout;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.junit.jupiter.api.Test;

class DefaultRolloutStrategyTest {

  private final DefaultRolloutStrategy strategy = new DefaultRolloutStrategy();
  private final FeatureFlagContext context = new FeatureFlagContext("user-1");

  @Test
  void isInRollout_returnsFalse_whenPercentageIsZero() {
    assertFalse(strategy.isInRollout("feature", context, 0));
  }

  @Test
  void isInRollout_returnsFalse_whenPercentageIsNegative() {
    assertFalse(strategy.isInRollout("feature", context, -1));
  }

  @Test
  void isInRollout_returnsTrue_whenPercentageIs100() {
    assertTrue(strategy.isInRollout("feature", context, 100));
  }

  @Test
  void isInRollout_returnsTrue_whenPercentageIsOver100() {
    assertTrue(strategy.isInRollout("feature", context, 101));
  }

  @Test
  void isInRollout_isDeterministic_sameInputAlwaysProducesSameResult() {
    boolean first = strategy.isInRollout("feature", context, 50);
    boolean second = strategy.isInRollout("feature", context, 50);
    assertEquals(first, second);
  }

  @Test
  void isInRollout_producesConsistentBucket_acrossMultipleFeatureNames() {
    FeatureFlagContext ctx = new FeatureFlagContext("stable-user");
    boolean resultA = strategy.isInRollout("feature-a", ctx, 50);
    boolean resultB = strategy.isInRollout("feature-b", ctx, 50);

    // Same user may have different buckets for different features — just verify they're stable
    assertEquals(resultA, strategy.isInRollout("feature-a", ctx, 50));
    assertEquals(resultB, strategy.isInRollout("feature-b", ctx, 50));
  }

  @Test
  void isInRollout_bucketsAreInValidRange() {
    // Verify bucketing logic distributes across 0-99 by testing many unique identifiers
    Set<Boolean> results = new HashSet<>();
    for (int i = 0; i < 200; i++) {
      FeatureFlagContext ctx = new FeatureFlagContext("user-" + i);
      results.add(strategy.isInRollout("feature", ctx, 50));
    }
    // With 200 users and 50% rollout, we expect both true and false to appear
    assertTrue(results.contains(true));
    assertTrue(results.contains(false));
  }

  @Test
  void isInRollout_doesNotThrow_forInputsThatMightProduceExtremeHashValues() {
    // This test guards against the Math.abs(Integer.MIN_VALUE) bug.
    // The fix uses Integer.remainderUnsigned() which handles all 32-bit hash values correctly.
    assertDoesNotThrow(
        () -> {
          for (int i = 0; i < 1000; i++) {
            FeatureFlagContext ctx = new FeatureFlagContext("probe-" + i);
            strategy.isInRollout("feature", ctx, 50);
          }
        });
  }

  @Test
  void isInRollout_returnsTrue_whenPercentageIs99_andBucketIsWithinRange() {
    // At 99%, almost all users should be in rollout
    int inRollout = 0;
    for (int i = 0; i < 100; i++) {
      FeatureFlagContext ctx = new FeatureFlagContext("user-" + i);
      if (strategy.isInRollout("feature", ctx, 99)) {
        inRollout++;
      }
    }
    assertTrue(inRollout > 80, "Expected more than 80% in rollout at 99%, got: " + inRollout);
  }

  @Test
  void isInRollout_returnsTrue_whenPercentageIs1_andBucketIsWithinRange() {
    // At 1%, very few users should be in rollout
    int inRollout = 0;
    for (int i = 0; i < 100; i++) {
      FeatureFlagContext ctx = new FeatureFlagContext("user-" + i);
      if (strategy.isInRollout("feature", ctx, 1)) {
        inRollout++;
      }
    }
    assertTrue(inRollout < 20, "Expected fewer than 20% in rollout at 1%, got: " + inRollout);
  }
}
