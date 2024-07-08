package org.aktin.broker.auth.apikey;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "apiKeyCred")
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
