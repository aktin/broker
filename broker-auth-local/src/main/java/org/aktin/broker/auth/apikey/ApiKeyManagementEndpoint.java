package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;

@Authenticated
@RequireAdmin
@Path("api-keys")
public class ApiKeyManagementEndpoint {

  private static final Logger log = Logger.getLogger(ApiKeyManagementEndpoint.class.getName());

  @Inject
  ApiKeyPropertiesAuthProvider authProvider;

  @GET
  public Response getApiKeys() {
    try {
      PropertyFileAPIKeys apiKeys = authProvider.getInstance();
      Properties props = apiKeys.getProperties();
      return Response.ok(convertPropertiesToString(props)).build();
    } catch (IOException e) {
      log.severe("Error retrieving API keys: " + e.getMessage());
      return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An error occurred while retrieving API keys");
    }
  }

  private String convertPropertiesToString(Properties props) {
    StringBuilder response = new StringBuilder();
    for (String name : props.stringPropertyNames()) {
      response.append(name).append("=").append(props.getProperty(name)).append("\n");
    }
    return response.toString();
  }

  private Response createErrorResponse(Response.Status status, String message) {
    return Response.status(status)
        .entity(message)
        .build();
  }

  @PUT
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response putApiKey(@FormParam("apiKey") String apiKey, @FormParam("clientDn") String clientDn) {
    if (apiKey == null || clientDn == null) {
      return createErrorResponse(Response.Status.BAD_REQUEST, "API key and client DN are required");
    }
    try {
      authProvider.storeApiKeyAndUpdatePropertiesFile(apiKey, clientDn);
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      return createErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
    } catch (IOException e) {
      log.severe("Error storing API key: " + e.getMessage());
      return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An error occurred while storing the API key");
    }
  }

  @PUT
  @Path("activate")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response activateApiKey(@FormParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.ACTIVE);
  }

  @PUT
  @Path("deactivate")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response deactivateApiKey(@FormParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.INACTIVE);
  }

  private Response setApiKeyStatus(String apiKey, ApiKeyStatus status) {
    if (apiKey == null) {
      return createErrorResponse(Response.Status.BAD_REQUEST, "API key is required");
    }
    try {
      authProvider.setStateOfApiKeyAndUpdatePropertiesFile(apiKey, status);
      return Response.ok().build();
    } catch (IllegalArgumentException e) {
      return createErrorResponse(Response.Status.BAD_REQUEST, e.getMessage());
    } catch (IOException e) {
      log.severe("Error updating API key status: " + e.getMessage());
      return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An error occurred while updating the API key status");
    }
  }
}
