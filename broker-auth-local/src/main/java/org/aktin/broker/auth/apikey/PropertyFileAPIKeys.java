package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.logging.Logger;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.HttpBearerAuthentication;

/**
 * Manages API keys stored in a property file for HTTP Bearer Authentication.
 */
public class PropertyFileAPIKeys extends HttpBearerAuthentication {

  private static final Logger log = Logger.getLogger(PropertyFileAPIKeys.class.getName());
  private final Properties properties;

  /**
   * Constructs a new PropertyFileAPIKeys instance by loading API keys from an input stream.
   *
   * @param in The input stream containing the API key properties.
   * @throws IOException If an I/O error occurs while reading from the input stream.
   */
  public PropertyFileAPIKeys(InputStream in) throws IOException {
    this.properties = new Properties();
    properties.load(in);
    log.info("Loaded " + properties.size() + " client API keys");
  }

  /**
   * @return The Properties object with the API keys.
   */
  public Properties getProperties() {
    return properties;
  }

  /**
   * Adds or updates an API key in the properties.
   *
   * @param apiKey   The API key to add or update.
   * @param clientDn The client's Distinguished Name.
   * @param status   The status of the API key (ACTIVE or INACTIVE).
   */
  public void putApiKey(String apiKey, String clientDn, ApiKeyStatus status) {
    properties.setProperty(apiKey, clientDn + "," + status.toString());
    log.info("Put API key for client: " + clientDn + " with status: " + status);
  }

  /**
   * Stores the current API keys to an output stream.
   *
   * @param out     The output stream to write the properties to.
   * @param charset The character set to use for writing.
   * @throws IOException If an I/O error occurs while writing to the output stream.
   */
  public void storeProperties(OutputStream out, Charset charset) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
      properties.store(writer, "API Keys");
    }
    log.info("Saved " + properties.size() + " client API keys");
  }

  /**
   * Looks up authentication information for a given token.
   * <p>
   * This method is called during the authentication process to validate API keys. It checks if the token exists and is active.
   *
   * @param token The API key token to look up.
   * @return AuthInfo object if the token is valid and active, null otherwise.
   */
  @Override
  protected AuthInfo lookupAuthInfo(String token) {
    String property = properties.getProperty(token);
    if (property != null) {
      String[] parts = property.split(",");
      if (parts.length == 4 && ApiKeyStatus.ACTIVE.toString().equals(parts[3])) {
        return createAuthInfo(token, parts[0] + "," + parts[1] + "," + parts[2]);
      }
    }
    return null;
  }

  /**
   * Creates an AuthInfo object for a given token and client DN.
   *
   * @param token    The API key token.
   * @param clientDn The client's Distinguished Name.
   * @return A new AuthInfo object.
   */
  private AuthInfo createAuthInfo(String token, String clientDn) {
    return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
  }
}