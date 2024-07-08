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
 * Implements API key authentication using a property file as the storage mechanism. This class extends {@link HttpBearerAuthentication} to provide
 * bearer token authentication with API keys stored in a properties file.
 *
 * @author akombeiz@ukaachen.de
 */
public class PropertyFileAPIKeys extends HttpBearerAuthentication {

  private static final Logger log = Logger.getLogger(PropertyFileAPIKeys.class.getName());
  private final Properties properties;

  /**
   * Constructs a new PropertyFileAPIKeys instance by loading API keys from an input stream.
   *
   * @param in The input stream containing the properties file with API keys.
   * @throws IOException If an I/O error occurs while reading from the input stream.
   */
  public PropertyFileAPIKeys(InputStream in) throws IOException {
    this.properties = new Properties();
    properties.load(in);
    log.info("Loaded " + properties.size() + " client API keys");
  }

  /**
   * @return The Properties object with API keys.
   */
  public Properties getProperties() {
    return properties;
  }

  /**
   * Adds or updates an API key for a client.
   *
   * @param apiKey   The API key to add or update.
   * @param clientDn The distinguished name of the client associated with the API key.
   */
  public void putApiKey(String apiKey, String clientDn) {
    properties.setProperty(apiKey, clientDn);
    log.info("Put API key for client: " + clientDn);
  }

  /**
   * Stores the current API keys to an output stream using the specified character set.
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
   * Looks up authentication information for a given token. If the token is not found in the properties or is marked as INACTIVE, null is returned.
   * INACTIVE will always appear at the end of the Client Distinguished Name.
   *
   * @param token The API key token to authenticate.
   * @return An {@link AuthInfo} object if authentication succeeds, null otherwise.
   */
  @Override
  protected AuthInfo lookupAuthInfo(String token) {
    String clientDn = properties.getProperty(token);
    if (clientDn != null) {
      String[] parts = clientDn.split(",");
      if (!ApiKeyStatus.INACTIVE.name().equals(parts[parts.length - 1])) {
        return createAuthInfo(token, clientDn);
      }
    }
    return null;
  }

  /**
   * Creates an {@link AuthInfo} object for a given token and client DN.
   *
   * @param token    The API key token.
   * @param clientDn The distinguished name of the client.
   * @return A new {@link AuthInfo} object.
   */
  private AuthInfo createAuthInfo(String token, String clientDn) {
    return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
  }
}