package org.aktin.broker.rest;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.ws.rs.NameBinding;
import javax.ws.rs.container.ContainerRequestFilter;

/**
 * Qualifier indicating that a REST method requires admin privileges.
 * To implement admin authentication, provide a class which implements {@link ContainerRequestFilter}
 * with the following annotations:
 *<pre>
 *  {@literal @}RequireAdmin
 *  {@literal @}Provider
 *  {@literal @}Priority(Priorities.AUTHENTICATION)
 *</pre>
 * @author R.W.Majeed
 *
 */
@NameBinding
@Retention(RUNTIME)
@Target({ TYPE, METHOD })
public @interface RequireAdmin {

}
