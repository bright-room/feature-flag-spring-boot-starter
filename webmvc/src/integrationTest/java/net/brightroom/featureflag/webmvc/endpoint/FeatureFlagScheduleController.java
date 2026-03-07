package net.brightroom.featureflag.webmvc.endpoint;

import net.brightroom.featureflag.core.annotation.FeatureFlag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FeatureFlagScheduleController {

  @GetMapping("/schedule/active")
  @FeatureFlag("active-scheduled-feature")
  public String activeSchedule() {
    return "Allowed";
  }

  @GetMapping("/schedule/inactive")
  @FeatureFlag("inactive-scheduled-feature")
  public String inactiveSchedule() {
    return "Allowed";
  }

  @GetMapping("/schedule/timezone")
  @FeatureFlag("timezone-scheduled-feature")
  public String timezoneSchedule() {
    return "Allowed";
  }
}
