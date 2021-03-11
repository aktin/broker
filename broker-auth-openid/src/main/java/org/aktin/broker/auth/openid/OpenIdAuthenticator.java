package org.aktin.broker.auth.openid;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import javax.ws.rs.core.HttpHeaders;
import org.aktin.broker.server.auth.AuthInfo;
import org.aktin.broker.server.auth.AuthInfoImpl;
import org.aktin.broker.server.auth.AuthRole;
import org.aktin.broker.server.auth.HeaderAuthentication;
import org.aktin.broker.server.auth.HttpBearerAuthentication;
import org.jose4j.jwa.AlgorithmConstraints.ConstraintType;
import org.jose4j.jwk.HttpsJwks;
import org.jose4j.jwt.JwtClaims;
import org.jose4j.jwt.consumer.InvalidJwtException;
import org.jose4j.jwt.consumer.JwtConsumer;
import org.jose4j.jwt.consumer.JwtConsumerBuilder;
import org.jose4j.keys.resolvers.HttpsJwksVerificationKeyResolver;

public class OpenIdAuthenticator implements HeaderAuthentication {

  public static final String KEY_JWT_USERNAME = "clientId";
  private final OpenIdConfig config;

  public OpenIdAuthenticator(OpenIdConfig config) {
    this.config = config;
  }

  @Override
  public AuthInfo authenticateByHeaders(Function<String, String> getHeader) {
    Objects.requireNonNull(this.config);
    String accessTokenSerialized = HttpBearerAuthentication.extractBearerToken(getHeader.apply(
        HttpHeaders.AUTHORIZATION));

    try {
      JwtClaims jwtClaims = verifyToken(accessTokenSerialized);
      String userId = jwtClaims.getClaimValueAsString(KEY_JWT_USERNAME);
      String siteName = jwtClaims.getClaimValueAsString(config.getSiteNameClaim());

      // Currently, the definition is that only diz-clients get a site name claim. This might change.
      Set<AuthRole> roles = new HashSet<>();
      if (siteName != null && !siteName.isEmpty()) {
		roles.add(AuthRole.NODE_READ);
		roles.add(AuthRole.NODE_WRITE);
      }else {
    	  // no site claim -> admin access
    	  siteName = userId; // use userId as common name
    	  roles.add(AuthRole.ADMIN_READ);
    	  roles.add(AuthRole.NODE_WRITE);
      }
      return new AuthInfoImpl(userId, "CN=" + siteName, roles);
    } catch (IllegalAccessException e) {
      return null;
    }
  }

	/**
	 * Take an access token and check its viability.
	 * @param accessTokenSerialized the serialized access token as received in the Auth header
	 * @return the set of claims contained in the token
	 */
  private JwtClaims verifyToken(String accessTokenSerialized)
      throws IllegalAccessException {
    try {
      HttpsJwks httpsJwks = new HttpsJwks(config.getJwks_uri());
      HttpsJwksVerificationKeyResolver httpsJwksKeyResolver = new HttpsJwksVerificationKeyResolver(httpsJwks);

      JwtConsumer jwtConsumer = new JwtConsumerBuilder()
          .setRequireExpirationTime()
          .setAllowedClockSkewInSeconds(10)
          .setRequireSubject()
          .setExpectedIssuer(config.getAuth_host())
          .setSkipDefaultAudienceValidation() // TODO: take audience requirement into config?
          .setVerificationKeyResolver(httpsJwksKeyResolver)
          .setJwsAlgorithmConstraints(
              ConstraintType.PERMIT, config.getAllowedAlgorithms().toArray(new String[0]))
          .build();

      return jwtConsumer.processToClaims(accessTokenSerialized);
    } catch (InvalidJwtException e) {
      throw new IllegalAccessException();
    }
  }

}
