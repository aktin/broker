package org.aktin.broker.rest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;

/**
 * Qualifier indicating that a type or method requires
 * a valid client certificate.
 * 
 * TODO additional qualifier type AllowClientCert for optional client cert.
 * 
 * @author R.W.Majeed
 *
 */
@NameBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface Authenticated {

}
