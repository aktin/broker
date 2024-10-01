package org.aktin.broker.auth.apikey;

import javax.xml.bind.annotation.XmlRootElement;

/**
 * Data Transfer Object (DTO) for API key information. This class is used to transfer API key and client DN information between client and server in a
 * XML format.
 *
 * @author akombeiz@ukaachen.de
 */
@XmlRootElement(name = "ApiKeyCred")
public class ApiKeyDTO {

  private String apiKey;
  private String clientDn;

  public ApiKeyDTO() {
  }

  public String getApiKey() {
    return apiKey;
  }

  public void setApiKey(String apiKey) {
    this.apiKey = apiKey;
  }

  public String getClientDn() {
    return clientDn;
  }

  public void setClientDn(String clientDn) {
    this.clientDn = clientDn;
  }
}
