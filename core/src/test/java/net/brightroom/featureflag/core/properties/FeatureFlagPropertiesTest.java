package net.brightroom.featureflag.core.properties;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FeatureFlagPropertiesTest {

  private FeatureFlagProperties newProperties() {
    return new FeatureFlagProperties();
  }

  // --- schedules() ---

  @Test
  void schedules_returnsEmptyMap_whenNoFeaturesConfigured() {
    FeatureFlagProperties properties = newProperties();

    assertTrue(properties.schedules().isEmpty());
  }

  @Test
  void schedules_excludesFeatures_whenScheduleIsNull() {
    FeatureFlagProperties properties = newProperties();
    FeatureConfiguration config = new FeatureConfiguration();
    // no schedule set → schedule() returns null
    properties.setFeatures(Map.of("no-schedule-feature", config));

    assertTrue(properties.schedules().isEmpty());
  }

  @Test
  void schedules_includesFeature_whenScheduleIsConfigured() {
    FeatureFlagProperties properties = newProperties();
    ScheduleConfiguration scheduleConfig = new ScheduleConfiguration();
    scheduleConfig.setStart(LocalDateTime.of(2026, 6, 15, 10, 0));
    scheduleConfig.setEnd(LocalDateTime.of(2026, 6, 15, 18, 0));

    FeatureConfiguration config = new FeatureConfiguration();
    config.setSchedule(scheduleConfig);
    properties.setFeatures(Map.of("scheduled-feature", config));

    var schedules = properties.schedules();
    assertEquals(1, schedules.size());
    assertEquals(LocalDateTime.of(2026, 6, 15, 10, 0), schedules.get("scheduled-feature").start());
    assertEquals(LocalDateTime.of(2026, 6, 15, 18, 0), schedules.get("scheduled-feature").end());
  }

  @Test
  void schedules_mapsMultipleFeatures_whenMultipleSchedulesConfigured() {
    FeatureFlagProperties properties = newProperties();

    ScheduleConfiguration schedule1 = new ScheduleConfiguration();
    schedule1.setStart(LocalDateTime.of(2026, 1, 1, 0, 0));
    FeatureConfiguration config1 = new FeatureConfiguration();
    config1.setSchedule(schedule1);

    ScheduleConfiguration schedule2 = new ScheduleConfiguration();
    schedule2.setEnd(LocalDateTime.of(2026, 12, 31, 23, 59));
    FeatureConfiguration config2 = new FeatureConfiguration();
    config2.setSchedule(schedule2);

    FeatureConfiguration configNoSchedule = new FeatureConfiguration();

    properties.setFeatures(
        Map.of("feature-a", config1, "feature-b", config2, "feature-c", configNoSchedule));

    var schedules = properties.schedules();
    assertEquals(2, schedules.size());
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), schedules.get("feature-a").start());
    assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), schedules.get("feature-b").end());
  }
}
