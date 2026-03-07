package net.brightroom.featureflag.core.evaluation;

import net.brightroom.featureflag.core.condition.ConditionVariables;
import net.brightroom.featureflag.core.context.FeatureFlagContext;
import org.jspecify.annotations.Nullable;

/**
 * Immutable context object passed through the {@link FeatureFlagEvaluationPipeline}.
 *
 * <p>Contains all inputs required by the default evaluation steps. Callers are responsible for
 * resolving the rollout percentage (merging provider value and annotation fallback) before building
 * this context.
 *
 * @param featureName the name of the feature flag being evaluated
 * @param condition the SpEL condition expression; empty string means no condition check
 * @param rolloutPercentage the resolved rollout percentage (0–100)
 * @param variables the request context variables used for SpEL condition evaluation
 * @param flagContext the context used for rollout bucketing; {@code null} means fail-open (skip
 *     rollout check)
 */
public record EvaluationContext(
    String featureName,
    String condition,
    int rolloutPercentage,
    ConditionVariables variables,
    @Nullable FeatureFlagContext flagContext) {}
