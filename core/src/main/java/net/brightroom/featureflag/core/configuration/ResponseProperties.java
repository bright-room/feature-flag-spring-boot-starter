package net.brightroom.featureflag.core.configuration;

/**
 * Properties related to the response configuration.
 *
 * <p>This class encapsulates configuration properties for the response format and type, allowing
 * customization of how responses are generated and returned by the system.
 */
public class ResponseProperties {

  ResponseType type = ResponseType.JSON;

  /**
   * The type of response to return.
   *
   * @return the type of response
   */
  public ResponseType type() {
    return type;
  }

  // for property binding
  void setType(ResponseType type) {
    this.type = type;
  }

  ResponseProperties() {}
}
