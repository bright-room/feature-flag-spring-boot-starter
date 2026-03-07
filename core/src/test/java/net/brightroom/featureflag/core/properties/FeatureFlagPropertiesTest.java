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

  // --- conditions() ---

  @Test
  void conditions_returnsEmptyMap_whenNoFeaturesConfigured() {
    FeatureFlagProperties properties = newProperties();

    assertTrue(properties.conditions().isEmpty());
  }

  @Test
  void conditions_excludesFeatures_whenConditionIsEmpty() {
    FeatureFlagProperties properties = newProperties();
    FeatureProperties config = new FeatureProperties();
    // no condition set → condition() returns ""
    properties.setFeatures(Map.of("no-condition-feature", config));

    assertTrue(properties.conditions().isEmpty());
  }

  @Test
  void conditions_includesFeature_whenConditionIsConfigured() {
    FeatureFlagProperties properties = newProperties();
    FeatureProperties config = new FeatureProperties();
    config.setCondition("headers['X-Beta'] != null");
    properties.setFeatures(Map.of("conditional-feature", config));

    var conditions = properties.conditions();
    assertEquals(1, conditions.size());
    assertEquals("headers['X-Beta'] != null", conditions.get("conditional-feature"));
  }

  @Test
  void conditions_mapsMultipleFeatures_whenMultipleConditionsConfigured() {
    FeatureFlagProperties properties = newProperties();

    FeatureProperties config1 = new FeatureProperties();
    config1.setCondition("headers['X-Beta'] != null");

    FeatureProperties config2 = new FeatureProperties();
    config2.setCondition("params['variant'] == 'B'");

    FeatureProperties configNoCondition = new FeatureProperties();

    properties.setFeatures(
        Map.of("feature-a", config1, "feature-b", config2, "feature-c", configNoCondition));

    var conditions = properties.conditions();
    assertEquals(2, conditions.size());
    assertEquals("headers['X-Beta'] != null", conditions.get("feature-a"));
    assertEquals("params['variant'] == 'B'", conditions.get("feature-b"));
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
    FeatureProperties config = new FeatureProperties();
    // no schedule set → schedule() returns null
    properties.setFeatures(Map.of("no-schedule-feature", config));

    assertTrue(properties.schedules().isEmpty());
  }

  @Test
  void schedules_includesFeature_whenScheduleIsConfigured() {
    FeatureFlagProperties properties = newProperties();
    ScheduleProperties scheduleConfig = new ScheduleProperties();
    scheduleConfig.setStart(LocalDateTime.of(2026, 6, 15, 10, 0));
    scheduleConfig.setEnd(LocalDateTime.of(2026, 6, 15, 18, 0));

    FeatureProperties config = new FeatureProperties();
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

    ScheduleProperties schedule1 = new ScheduleProperties();
    schedule1.setStart(LocalDateTime.of(2026, 1, 1, 0, 0));
    FeatureProperties config1 = new FeatureProperties();
    config1.setSchedule(schedule1);

    ScheduleProperties schedule2 = new ScheduleProperties();
    schedule2.setEnd(LocalDateTime.of(2026, 12, 31, 23, 59));
    FeatureProperties config2 = new FeatureProperties();
    config2.setSchedule(schedule2);

    FeatureProperties configNoSchedule = new FeatureProperties();

    properties.setFeatures(
        Map.of("feature-a", config1, "feature-b", config2, "feature-c", configNoSchedule));

    var schedules = properties.schedules();
    assertEquals(2, schedules.size());
    assertEquals(LocalDateTime.of(2026, 1, 1, 0, 0), schedules.get("feature-a").start());
    assertEquals(LocalDateTime.of(2026, 12, 31, 23, 59), schedules.get("feature-b").end());
  }
}
