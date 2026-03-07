package net.brightroom.featureflag.actuator.endpoint;

import java.time.LocalDateTime;
import java.time.ZoneId;
import org.jspecify.annotations.Nullable;

/**
 * Response fragment representing the schedule configuration of a single feature flag.
 *
 * <p>Included in {@link FeatureFlagEndpointResponse} when a schedule is configured for the feature.
 *
 * @param start the schedule start time, or {@code null} if no start restriction is configured
 * @param end the schedule end time, or {@code null} if no end restriction is configured
 * @param timezone the timezone used to evaluate start/end times, or {@code null} if the system
 *     default timezone is used
 * @param active whether the schedule is currently active at the time the response was generated
 */
public record ScheduleEndpointResponse(
    @Nullable LocalDateTime start,
    @Nullable LocalDateTime end,
    @Nullable ZoneId timezone,
    boolean active) {}
