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

  public void putApiKey(String apiKey, String clientDn) {
    properties.setProperty(apiKey, clientDn);
    log.info("Put API key for client: " + clientDn);
  }

  public void storeProperties(OutputStream out, Charset charset) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
      properties.store(writer, "API Keys");
    }
    log.info("Saved " + properties.size() + " client API keys");
  }

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

  private AuthInfo createAuthInfo(String token, String clientDn) {
    return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
  }
}
