package org.aktin.broker.request;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import javax.inject.Qualifier;

/**
 * Qualifier indicating that the request status has changed.
 * Used for CDI event handling.
 *
 * @author R.W.Majeed
 *
 */
@Qualifier
//@Target({METHOD, FIELD, PARAMETER, TYPE})
@Retention(RUNTIME)
public @interface StatusChanged {
}
