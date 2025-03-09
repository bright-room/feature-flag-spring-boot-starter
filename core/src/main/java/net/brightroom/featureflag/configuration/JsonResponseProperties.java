package net.brightroom.featureflag.configuration;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

class JsonResponseProperties {

  private DefaultJsonResponseProperties defaultFields = new DefaultJsonResponseProperties();
  private Map<String, String> customFields = new ConcurrentHashMap<>();

  DefaultJsonResponseProperties defaultFields() {
    return defaultFields;
  }

  Map<String, String> customFields() {
    return customFields;
  }

  // for property injection
  void setDefaultFields(DefaultJsonResponseProperties defaultFields) {
    this.defaultFields = defaultFields;
  }

  // for property injection
  void setCustomFields(Map<String, String> customFields) {
    this.customFields = customFields;
  }

  JsonResponseProperties() {}
}
