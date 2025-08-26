package org.aktin.broker.auth.otp.controller;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import org.aktin.broker.auth.otp.repository.User;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserDTO implements Serializable {

  private static final long serialVersionUID = 1L;

  public String username;
  public String algorithm;
  public boolean active;
  public long createdAt;
  public boolean hasOTP;

  public UserDTO() {
  }

  public static UserDTO of(User u) {
    UserDTO dto = new UserDTO();
    dto.username = u.username;
    dto.algorithm = u.algorithm;
    dto.active = u.active;
    dto.createdAt = u.createdAt;
    dto.hasOTP = u.token.isPresent();
    return dto;
  }
}
