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

public class PropertyFileAPIKeys extends HttpBearerAuthentication {

  private static final Logger log = Logger.getLogger(PropertyFileAPIKeys.class.getName());
  private final Properties properties;

  public PropertyFileAPIKeys(InputStream in) throws IOException {
    this.properties = new Properties();
    properties.load(in);
    log.info("Loaded " + properties.size() + " client API keys");
  }

  public Properties getProperties() {
    return properties;
  }

  public void addApiKey(String apiKey, String clientDn, ApiKeyStatus status) {
    properties.setProperty(apiKey, clientDn + "," + status.toString());
    log.info("Added API key for client: " + clientDn + " with status: " + status);
  }

  public void storeProperties(OutputStream out, Charset charset) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
      properties.store(writer, "API Keys");
    }
    log.info("Saved " + properties.size() + " client API keys");
  }

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

  private AuthInfo createAuthInfo(String token, String clientDn) {
    return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
  }
}
