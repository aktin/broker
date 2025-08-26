package org.aktin.broker.auth.otp.controller;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "credentials")
@XmlAccessorType(XmlAccessType.FIELD)
public class CredentialsDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  public String username;
  public String password;
}
