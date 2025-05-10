package net.brightroom.featureflag.response;

class EmptyBodyAccessDeniedResponseBuilder implements AccessDeniedResponse.Builder {

  Integer status;

  @Override
  public AccessDeniedResponse.Builder status(Integer status) {
    this.status = status;
    return this;
  }

  @Override
  public AccessDeniedResponse.Builder append(String value) {
    throw new UnsupportedOperationException("AccessDeniedResponseBuilder should never be called");
  }

  @Override
  public AccessDeniedResponse.Builder append(String key, String value) {
    throw new UnsupportedOperationException("AccessDeniedResponseBuilder should never be called");
  }

  @Override
  public AccessDeniedResponse build() {
    return new AccessDeniedResponse(status, "");
  }

  EmptyBodyAccessDeniedResponseBuilder() {}
}
