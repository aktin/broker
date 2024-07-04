package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.stream.Collectors;
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

/**
 * REST endpoint for managing API keys. This class provides endpoints for retrieving, adding, and modifying API keys. It requires authentication and
 * admin privileges to access its methods.
 */
@Authenticated
@RequireAdmin
@Path("api-keys")
public class ApiKeyManagementEndpoint {

  private static final Logger log = Logger.getLogger(ApiKeyManagementEndpoint.class.getName());

  @Inject
  private ApiKeyPropertiesAuthProvider authProvider;

  /**
   * Retrieves all API keys.
   *
   * @return A Response containing a string representation of all API keys, or an error response if retrieval fails.
   */
  @GET
  public Response getApiKeys() {
    try {
      String apiKeysString = getApiKeysAsString();
      return Response.ok(apiKeysString).build();
    } catch (IOException e) {
      log.severe("Error retrieving API keys: " + e.getMessage());
      return createErrorResponse(Response.Status.INTERNAL_SERVER_ERROR, "An error occurred while retrieving API keys");
    }
  }

  /**
   * Converts the API keys to a string representation.
   *
   * @return A string containing all API keys and their properties.
   * @throws IOException If there's an error retrieving the API keys.
   */
  private String getApiKeysAsString() throws IOException {
    PropertyFileAPIKeys apiKeys = authProvider.getInstance();
    Properties props = apiKeys.getProperties();
    return props.stringPropertyNames().stream()
        .map(name -> name + "=" + props.getProperty(name))
        .collect(Collectors.joining("\n"));
  }

  /**
   * Adds or updates an API key.
   *
   * @param apiKey   The API key to add or update.
   * @param clientDn The client DN associated with the API key.
   * @return A Response indicating success or failure of the operation.
   */
  @PUT
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response putApiKey(@FormParam("apiKey") String apiKey, @FormParam("clientDn") String clientDn) {
    if (apiKey == null || clientDn == null) {
      return createErrorResponse(Response.Status.BAD_REQUEST, "API key and client DN are required");
    }
    return updateApiKey(apiKey, clientDn);
  }

  /**
   * Updates an API key in the auth provider.
   *
   * @param apiKey   The API key to update.
   * @param clientDn The client DN associated with the API key.
   * @return A Response indicating success or failure of the update operation.
   */
  private Response updateApiKey(String apiKey, String clientDn) {
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

  /**
   * Activates an API key.
   *
   * @param apiKey The API key to activate.
   * @return A Response indicating success or failure of the activation.
   */
  @PUT
  @Path("activate")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response activateApiKey(@FormParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.ACTIVE);
  }

  /**
   * Deactivates an API key.
   *
   * @param apiKey The API key to deactivate.
   * @return A Response indicating success or failure of the deactivation.
   */
  @PUT
  @Path("deactivate")
  @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
  public Response deactivateApiKey(@FormParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.INACTIVE);
  }

  /**
   * Sets the status of an API key.
   *
   * @param apiKey The API key to modify.
   * @param status The new status for the API key.
   * @return A Response indicating success or failure of the status change.
   */
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

  /**
   * Creates an error response with the given status and message.
   *
   * @param status  The HTTP status code for the error.
   * @param message The error message.
   * @return A Response object representing the error.
   */
  private Response createErrorResponse(Response.Status status, String message) {
    return Response.status(status)
        .entity(message)
        .build();
  }
}