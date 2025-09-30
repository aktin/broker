package org.aktin.broker.auth.otp.controller;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Represents the status of an authenticated session.
 */
@XmlRootElement(name = "status")
@XmlAccessorType(XmlAccessType.FIELD)
public class StatusDTO implements Serializable {

  private static final long serialVersionUID = 2L;

  public String username;
  public long issued;
  public long expiresAt;
}
