package net.brightroom.featureflag.response;

class TextBodyAccessDeniedResponseBuilder implements AccessDeniedResponse.Builder {

  Integer status;
  String body;

  @Override
  public AccessDeniedResponse.Builder status(Integer status) {
    this.status = status;
    return this;
  }

  @Override
  public AccessDeniedResponse.Builder append(String value) {
    this.body = value;
    return this;
  }

  @Override
  public AccessDeniedResponse.Builder append(String key, String value) {
    throw new UnsupportedOperationException("AccessDeniedResponseBuilder should never be called");
  }

  @Override
  public AccessDeniedResponse build() {
    return new AccessDeniedResponse(status, body);
  }

  TextBodyAccessDeniedResponseBuilder() {
    this.body = "";
  }
}
