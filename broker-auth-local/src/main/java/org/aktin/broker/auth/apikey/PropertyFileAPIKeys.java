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
    loadProperties(in);
  }

  public void loadProperties(InputStream in) throws IOException {
    properties.load(in);
    log.info("Loaded " + properties.size() + " client API keys");
  }

  public void storeProperties(OutputStream out, Charset charset) throws IOException {
    try (OutputStreamWriter writer = new OutputStreamWriter(out, charset)) {
      properties.store(writer, "API Keys");
    }
    log.info("Saved " + properties.size() + " client API keys");
  }

  public void addApiKey(String apiKey, String clientDn) {
    boolean isNewKey = !properties.containsKey(apiKey);
    properties.setProperty(apiKey, clientDn);
    if (isNewKey) {
      log.info("Added new API key for client: " + clientDn);
    } else {
      log.info("Updated API key for client: " + clientDn);
    }
  }
  
  public Properties getProperties() {
    return properties;
  }

  @Override
  protected AuthInfo lookupAuthInfo(String token) {
    String clientDn = properties.getProperty(token);
    return clientDn != null ? createAuthInfo(token, clientDn) : null;
  }

  private AuthInfo createAuthInfo(String token, String clientDn) {
    return new AuthInfoImpl(token, clientDn, HttpBearerAuthentication.defaultRolesForClientDN(clientDn));
  }
}
