package net.brightroom.featureflag.response;

public class AccessDeniedResponse {

  private Integer status;
  private String body;

  AccessDeniedResponse(Integer status, String body) {
    this.status = status;
    this.body = body;
  }

  public Integer status() {
    return status;
  }

  public String body() {
    return body;
  }

  @Override
  public String toString() {
    return "AccessDeniedResponse{" + "status=" + status + ", body='" + body + '\'' + '}';
  }

  public interface Builder {
    Builder status(Integer status);

    Builder append(String value);

    Builder append(String key, String value);

    AccessDeniedResponse build();
  }

  public static Builder newBuilder(boolean isReturnedBody, Mode mode) {
    if (!isReturnedBody) {
      return new EmptyBodyAccessDeniedResponseBuilder();
    }

    return switch (mode) {
      case JSON -> new JsonBodyAccessDeniedResponseBuilder();
      case TEXT -> new TextBodyAccessDeniedResponseBuilder();
    };
  }

  AccessDeniedResponse() {}
}
