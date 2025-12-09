package net.brightroom.featureflag.configuration;

import java.util.Map;

class ResponseProperties {

  Integer statusCode = 405;
  ResponseType type = ResponseType.PlainText;
  Map<String, String> body = Map.of("error", "This feature is not available");
  String message = "This feature is not available";

  Integer statusCode() {
    return statusCode;
  }

  ResponseType type() {
    return type;
  }

  Map<String, String> body() {
    return body;
  }

  String message() {
    return message;
  }

  // for property injection
  void setStatusCode(Integer statusCode) {
    this.statusCode = statusCode;
  }

  // for property injection
  void setType(ResponseType type) {
    this.type = type;
  }

  // for property injection
  void setBody(Map<String, String> body) {
    this.body = body;
  }

  // for property injection
  void setMessage(String message) {
    this.message = message;
  }

  ResponseProperties() {}
}
