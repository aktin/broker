package org.aktin.broker.auth.cred;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class Credentials implements Serializable {
	private static final long serialVersionUID = 1L;

	public String username;
	public String password;
}
