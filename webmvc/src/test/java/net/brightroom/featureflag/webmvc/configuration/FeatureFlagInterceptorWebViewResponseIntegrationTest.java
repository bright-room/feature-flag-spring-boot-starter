package net.brightroom.featureflag.webmvc.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@WebMvcTest(
    properties = {
      "feature-flags.response.type=VIEW",
      "feature-flags.response.view.forward-to=/access-denied"
    },
    controllers = {
      NoFeatureFlagViewController.class,
      FeatureFlagEnableViewController.class,
      FeatureFlagDisableViewController.class,
      FeatureFlagMethodLevelViewController.class,
    })
@Import(FeatureFlagMvcTestAutoConfiguration.class)
class FeatureFlagInterceptorWebViewResponseIntegrationTest {

  MockMvc mockMvc;

  @Test
  void shouldAllowAccess_whenNoAnnotated() throws Exception {
    MvcResult mvcResult = mockMvc.perform(get("/stable")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = mvcResult.getResponse();
    String htmlContent = response.getContentAsString();

    Document doc = Jsoup.parse(htmlContent);

    assertEquals("Stable page", doc.title());

    Elements h1Elements = doc.select("h1");
    assertEquals(1, h1Elements.size());
    assertEquals("stable-page", h1Elements.text());
  }

  @Test
  void shouldAllowAccess_whenFeatureIsEnabled() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/experimental-stage")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = mvcResult.getResponse();
    String htmlContent = response.getContentAsString();

    Document doc = Jsoup.parse(htmlContent);

    assertEquals("Experimental stage page", doc.title());

    Elements h1Elements = doc.select("h1");
    assertEquals(1, h1Elements.size());
    assertEquals("experimental-stage", h1Elements.text());
  }

  @Test
  void shouldBlockAccess_whenFeatureIsDisabled() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/development-stage")).andExpect(status().isForbidden()).andReturn();

    String htmlContent = mvcResult.getResponse().getContentAsString();
    Document doc = Jsoup.parse(htmlContent);

    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'development-stage-endpoint' is not available", doc.select("p").text());
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureNoAnnotated() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/test/no-annotation")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = mvcResult.getResponse();
    String htmlContent = response.getContentAsString();

    Document doc = Jsoup.parse(htmlContent);

    assertEquals("No annotation page", doc.title());

    Elements h1Elements = doc.select("h1");
    assertEquals(1, h1Elements.size());
    assertEquals("no-annotation", h1Elements.text());
  }

  @Test
  void shouldBlockAccess_whenClassLevelFeatureIsDisabled() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/test/disable")).andExpect(status().isForbidden()).andReturn();

    String htmlContent = mvcResult.getResponse().getContentAsString();
    Document doc = Jsoup.parse(htmlContent);

    assertEquals("Access Denied", doc.title());
    assertEquals("403 - Access Denied", doc.select("h1").text());
    assertEquals("Feature 'disable-class-level-feature' is not available", doc.select("p").text());
  }

  @Test
  void shouldAllowAccess_whenNoFeatureFlagAnnotation() throws Exception {
    MvcResult mvcResult =
        mockMvc.perform(get("/test/enabled")).andExpect(status().isOk()).andReturn();

    MockHttpServletResponse response = mvcResult.getResponse();
    String htmlContent = response.getContentAsString();

    Document doc = Jsoup.parse(htmlContent);

    assertEquals("Enable page", doc.title());

    Elements h1Elements = doc.select("h1");
    assertEquals(1, h1Elements.size());
    assertEquals("enable", h1Elements.text());
  }

  @Autowired
  FeatureFlagInterceptorWebViewResponseIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }
}
