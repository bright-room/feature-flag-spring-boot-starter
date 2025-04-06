package net.brightroom.featureflag.configuration;

class ResponseProperties {

  private Integer status = 405;
  private ResponseBodyProperties body = new ResponseBodyProperties();

  Integer status() {
    return status;
  }

  ResponseBodyProperties body() {
    return body;
  }

  // for property injection
  void setStatus(Integer status) {
    this.status = status;
  }

  // for property injection
  void setBody(ResponseBodyProperties body) {
    this.body = body;
  }

  ResponseProperties() {}
}
