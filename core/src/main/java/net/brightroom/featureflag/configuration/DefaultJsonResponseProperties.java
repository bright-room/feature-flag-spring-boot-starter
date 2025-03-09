package net.brightroom.featureflag.configuration;

class DefaultJsonResponseProperties {

  private boolean enabled = true;
  private String title = "Feature Not Available";
  private String detail = "The requested feature is not available";

  boolean isEnabled() {
    return enabled;
  }

  String title() {
    return title;
  }

  String detail() {
    return detail;
  }

  // for property injection
  void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  // for property injection
  void setTitle(String title) {
    this.title = title;
  }

  // for property injection
  void setDetail(String detail) {
    this.detail = detail;
  }

  DefaultJsonResponseProperties() {}
}
