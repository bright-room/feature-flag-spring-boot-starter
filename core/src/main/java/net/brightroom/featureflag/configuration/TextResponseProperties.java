package net.brightroom.featureflag.configuration;

class TextResponseProperties {

  private String message = "The requested feature is not available.";

  String message() {
    return message;
  }

  // for property injection
  void setMessage(String message) {
    this.message = message;
  }

  TextResponseProperties() {}
}
