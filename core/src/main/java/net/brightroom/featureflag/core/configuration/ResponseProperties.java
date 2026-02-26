package net.brightroom.featureflag.core.configuration;

import java.util.Map;

public class ResponseProperties {

  Integer statusCode = 405;
  ResponseType type = ResponseType.PLAIN_TEXT;
  Map<String, String> body = Map.of("error", "This feature is not available");
  String message = "This feature is not available";
  ViewProperties view = new ViewProperties();

  public Integer statusCode() {
    return statusCode;
  }

  public ResponseType type() {
    return type;
  }

  public Map<String, String> body() {
    return body;
  }

  public String message() {
    return message;
  }

  public ViewProperties view() {
    return view;
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

  // for property injection
  void setView(ViewProperties view) {
    this.view = view;
  }

  ResponseProperties() {}
}
