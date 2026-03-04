package net.brightroom.featureflag.webmvc;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import net.brightroom.featureflag.webmvc.configuration.FeatureFlagMvcTestAutoConfiguration;
import net.brightroom.featureflag.webmvc.endpoint.FeatureFlagRouterConfiguration;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(properties = {"feature-flags.response.type=HTML"})
@Import({FeatureFlagMvcTestAutoConfiguration.class, FeatureFlagRouterConfiguration.class})
class FeatureFlagHandlerFilterFunctionHtmlResponseIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenNoFilter() throws Exception {
    mockMvc.perform(get("/functional/stable-endpoint")).andExpect(status().isOk());
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() throws Exception {
    mockMvc.perform(get("/functional/experimental-stage-endpoint")).andExpect(status().isOk());
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    MvcResult mvcResult =
        mockMvc
            .perform(get("/functional/development-stage-endpoint"))
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
        mockMvc
            .perform(get("/functional/test/disable"))
            .andExpect(status().isForbidden())
            .andReturn();

    Document doc = Jsoup.parse(mvcResult.getResponse().getContentAsString());
    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'disable-class-level-feature' is not available", doc.select("p").text());
  }

  @Test
  void shouldAllowAccess_whenClassLevelFeatureIsEnabled() throws Exception {
    mockMvc.perform(get("/functional/test/enabled")).andExpect(status().isOk());
  }

  @Autowired
  FeatureFlagHandlerFilterFunctionHtmlResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
