package net.brightroom.featureflag.presentation.endpoint;

class DummyResponse {

  String username;
  String password;

  DummyResponse(String username, String password) {
    this.username = username;
    this.password = password;
  }

  @Override
  public String toString() {
    return "DummyResponse{"
        + "username='"
        + username
        + '\''
        + ", password='"
        + password
        + '\''
        + '}';
  }

  DummyResponse() {}
}
