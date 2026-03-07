package net.brightroom.featureflag.webmvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagScheduleController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Verifies schedule-based feature flag control through the full MVC stack:
 *
 * <ul>
 *   <li>Property configuration → {@code InMemoryScheduleProvider} auto-wiring → interceptor → HTTP
 *       response
 *   <li>Active schedule (start in the past) → 200 OK
 *   <li>Inactive schedule (start in the future) → 403 Forbidden
 *   <li>Timezone-aware schedule → correctly evaluated in the configured timezone
 * </ul>
 */
@WebMvcTest(controllers = FeatureFlagScheduleController.class)
@Import(FeatureFlagMvcTestAutoConfiguration.class)
@TestPropertySource(
    properties = {
      // active-scheduled-feature: start far in the past → always active
      "feature-flags.features.active-scheduled-feature.enabled=true",
      "feature-flags.features.active-scheduled-feature.schedule.start=2020-01-01T00:00:00",
      // inactive-scheduled-feature: start far in the future → always inactive
      "feature-flags.features.inactive-scheduled-feature.enabled=true",
      "feature-flags.features.inactive-scheduled-feature.schedule.start=2099-01-01T00:00:00",
      // timezone-scheduled-feature: start far in the past with timezone → always active
      "feature-flags.features.timezone-scheduled-feature.enabled=true",
      "feature-flags.features.timezone-scheduled-feature.schedule.start=2020-01-01T00:00:00",
      "feature-flags.features.timezone-scheduled-feature.schedule.timezone=Asia/Tokyo",
    })
class FeatureFlagInterceptorScheduleIntegrationTest {

  MockMvc mockMvc;

  @Autowired
  FeatureFlagInterceptorScheduleIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void shouldAllowAccess_whenScheduleIsActive() throws Exception {
    mockMvc
        .perform(get("/schedule/active"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }

  @Test
  void shouldBlockAccess_whenScheduleIsInactive() throws Exception {
    mockMvc
        .perform(get("/schedule/inactive"))
        .andExpect(status().isForbidden())
        .andExpect(
            content()
                .json(
                    """
                    {
                      "detail" : "Feature 'inactive-scheduled-feature' is not available",
                      "instance" : "/schedule/inactive",
                      "status" : 403,
                      "title" : "Feature flag access denied",
                      "type" : "https://github.com/bright-room/feature-flag-spring-boot-starter#response-types"
                    }
                    """));
  }

  @Test
  void shouldAllowAccess_whenScheduleIsActiveWithTimezone() throws Exception {
    mockMvc
        .perform(get("/schedule/timezone"))
        .andExpect(status().isOk())
        .andExpect(content().string("Allowed"));
  }
}
