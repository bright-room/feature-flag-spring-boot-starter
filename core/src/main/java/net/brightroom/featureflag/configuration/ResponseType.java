package net.brightroom.featureflag.configuration;

/**
 * Represents the type of response to be returned by the system.
 *
 * <p>This enumeration is used to define how responses are structured or formatted, allowing
 * different formats such as plain text, JSON, or view-based responses.
 *
 * <p>- PLAIN_TEXT: Denotes a plain text response. - JSON: Denotes a JSON-formatted response. -
 * VIEW: Denotes a view-based response, typically used in web-based applications.
 */
public enum ResponseType {
  /** Plain text response. */
  PLAIN_TEXT,

  /** JSON response. */
  JSON,

  /** View response. */
  VIEW
}
