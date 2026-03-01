package net.brightroom.featureflag.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagDisableController;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagMethodLevelController;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
    properties = {"feature-flags.response.type=HTML"},
    controllers = {
      FeatureFlagDisableController.class,
      FeatureFlagMethodLevelController.class,
    })
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorHtmlResponseIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(get("/development-stage-endpoint"))
            .andExpect(status().isForbidden())
            .andReturn();

    Document doc = Jsoup.parse(mvcResult.getResponse().getContentAsString());
    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'development-stage-endpoint' is not available", doc.select("p").text());
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/test/disable")).andExpect(status().isForbidden()).andReturn();

    Document doc = Jsoup.parse(mvcResult.getResponse().getContentAsString());
    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'disable-class-level-feature' is not available", doc.select("p").text());
  }

  @Autowired
  FeatureFlagInterceptorHtmlResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
