package net.brightroom.featureflag.core.configuration;

import java.util.HashMap;
import java.util.Map;

public class ViewProperties {

  String forwardTo = "/access-denied";
  Map<String, String> attributes = new HashMap<>();

  public String forwardTo() {
    return forwardTo;
  }

  public Map<String, String> attributes() {
    return attributes;
  }

  // for property injection
  void setForwardTo(String forwardTo) {
    this.forwardTo = forwardTo;
  }

  // for property injection
  void setAttributes(Map<String, String> attributes) {
    this.attributes = attributes;
  }

  ViewProperties() {}
}
