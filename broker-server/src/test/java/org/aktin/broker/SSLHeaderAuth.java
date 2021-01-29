package org.aktin.broker;

import javax.annotation.Priority;
import javax.ws.rs.Priorities;
import javax.ws.rs.ext.Provider;

import org.aktin.broker.auth.AuthFilterSSLHeaders;
import org.aktin.broker.rest.Authenticated;

@Authenticated
@Provider
@Priority(Priorities.AUTHENTICATION)
public class SSLHeaderAuth extends AuthFilterSSLHeaders{

}
