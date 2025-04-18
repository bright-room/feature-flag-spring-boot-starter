package net.brightroom.featureflag.response;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.HashMap;
import java.util.Map;

public class JsonBodyAccessDeniedResponseBuilder implements AccessDeniedResponse.Builder {

  Integer status;
  Map<String, String> body;

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
    body.put(key, value);
    return this;
  }

  @Override
  public AccessDeniedResponse build() {
    try {
      ObjectMapper mapper = new ObjectMapper();
      String json = mapper.writeValueAsString(body);

      return new AccessDeniedResponse(status, json);
    } catch (JsonProcessingException e) {
      throw new RuntimeException("Unable to serialize body", e);
    }
  }

  JsonBodyAccessDeniedResponseBuilder() {
    this.body = new HashMap<>();
  }
}
