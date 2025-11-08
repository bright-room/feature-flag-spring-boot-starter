package net.brightroom.featureflag.configuration;

import net.brightroom.featureflag.response.Mode;

class ResponseBodyProperties {

  private boolean enabled = true;
  private Mode mode = Mode.JSON;
  private JsonResponseProperties json = new JsonResponseProperties();
  private TextResponseProperties text = new TextResponseProperties();

  boolean isEnabled() {
    return enabled;
  }

  Mode mode() {
    return mode;
  }

  JsonResponseProperties json() {
    return json;
  }

  TextResponseProperties text() {
    return text;
  }

  // for property injection
  void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  // for property injection
  void setMode(Mode mode) {
    this.mode = mode;
  }

  // for property injection
  void setJson(JsonResponseProperties json) {
    this.json = json;
  }

  // for property injection
  void setText(TextResponseProperties text) {
    this.text = text;
  }

  ResponseBodyProperties() {}
}
