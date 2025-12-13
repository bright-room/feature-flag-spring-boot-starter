package net.brightroom.featureflag.configuration;

import java.util.HashMap;
import java.util.Map;

class ViewProperties {

  String forwardTo = "/access-denied";
  Map<String, String> attributes = new HashMap<>();

  String forwardTo() {
    return forwardTo;
  }

  Map<String, String> attributes() {
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
