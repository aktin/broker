package org.aktin.broker.auth.cred2.http;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Status implements Serializable {

  private static final long serialVersionUID = 1L;

  public String username;
  public long issued;
  public long expiresAt;
}
