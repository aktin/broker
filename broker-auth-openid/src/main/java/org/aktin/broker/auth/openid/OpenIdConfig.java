package org.aktin.broker.auth.openid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import lombok.Data;

@Data
public class OpenIdConfig {

  private String auth_host;
  private String clientId;
  private String clientSecret;

  public OpenIdConfig(InputStream in) {
    Properties prop = new Properties();
    try {
      prop.load(in);
      setAuth_host(prop.getProperty("openid.server"));
      setClientId(prop.getProperty("openid.client.id"));
      setClientSecret(prop.getProperty("openid.client.secret"));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
