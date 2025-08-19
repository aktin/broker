package org.aktin.broker.auth.cred2.http;

import java.io.Serializable;
import java.time.Instant;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.aktin.broker.auth.cred2.repository.User;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  public String username;
  public String algorithm;
  public boolean active;
  public long createdAt;

  public UserDTO() {
  }

  public static UserDTO of(User u) {
    UserDTO dto = new UserDTO();
    dto.username = u.username;
    dto.algorithm = u.algorithm;
    dto.active = u.active;
    dto.createdAt = u.createdAt;
    return dto;
  }
}
