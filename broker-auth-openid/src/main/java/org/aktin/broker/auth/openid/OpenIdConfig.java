package org.aktin.broker.auth.openid;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;
import lombok.Data;

@Data
public class OpenIdConfig {

  private String jwks_uri;
  private String auth_host;
  private String siteNameClaim;
  private List<String> allowedAlgorithms;

  public OpenIdConfig(InputStream in) {
    Properties prop = new Properties();
    try {
      prop.load(in);
      setJwks_uri(prop.getProperty("openid.jwks_uri"));
      setAuth_host(prop.getProperty("openid.server"));
      setSiteNameClaim(prop.getProperty("openid.claim.site-name"));
      setAllowedAlgorithms(extractAlgorithmsList(prop.getProperty("openid.algorithms")));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private List<String> extractAlgorithmsList(String algorithmsString) {
    List<String> algorithmsList = Arrays.asList(algorithmsString.split(","));
    return algorithmsList.stream().map(String::trim).collect(Collectors.toList());
  }
}
