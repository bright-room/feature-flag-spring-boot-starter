package net.brightroom.featureflag.core.configuration;

/**
 * Represents the type of response to be returned by the system.
 *
 * <p>This enumeration is used to define how responses are structured or formatted, allowing
 * different formats such as plain text, JSON, or HTML responses.
 *
 * <ul>
 *   <li>PLAIN_TEXT: Denotes a plain text response.
 *   <li>JSON: Denotes a JSON-formatted response.
 *   <li>HTML: Denotes a simple fixed HTML response.
 * </ul>
 */
public enum ResponseType {
  /** Plain text response. */
  PLAIN_TEXT,

  /** JSON response. */
  JSON,

  /** HTML response. */
  HTML
}
