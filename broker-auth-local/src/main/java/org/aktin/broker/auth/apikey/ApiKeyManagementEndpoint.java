package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.aktin.broker.rest.Authenticated;
import org.aktin.broker.rest.RequireAdmin;

/**
 * RESTful endpoint for managing API keys. This class provides operations to retrieve, create, activate, and deactivate API keys. Access to these
 * operations requires authentication and admin privileges.
 *
 * @author akombeiz@ukaachen.de
 */
@Authenticated
@RequireAdmin
@Path("api-keys")
public class ApiKeyManagementEndpoint {

  private static final Logger log = Logger.getLogger(ApiKeyManagementEndpoint.class.getName());

  @Inject
  ApiKeyPropertiesAuthProvider authProvider;

  /**
   * Retrieves all API keys.
   *
   * @return A Response with {@code 200} containing all API keys as a string, or {@code 500} if retrieval fails.
   */
  @GET
  public Response getApiKeys() {
    try {
      PropertyFileAPIKeys apiKeys = authProvider.getInstance();
      Properties props = apiKeys.getProperties();
      return Response.ok(convertPropertiesToString(props)).build();
    } catch (IOException e) {
      log.severe("Error retrieving API keys: " + e.getMessage());
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Converts Properties to a string representation.
   *
   * @param props The Properties object to convert.
   * @return A string representation of the properties.
   */
  private String convertPropertiesToString(Properties props) {
    StringBuilder response = new StringBuilder();
    for (String name : props.stringPropertyNames()) {
      response.append(name).append("=").append(props.getProperty(name)).append("\n");
    }
    return response.toString();
  }

  /**
   * Creates a new API key.
   *
   * @param apiKeyDTO The DTO containing the API key and client DN.
   * @return A Response indicating the result of the operation: <ul>
   * <li>{@code 201} - API key created successfully</li>
   * <li>{@code 400} - API key or client DN are not provided</li>
   * <li>{@code 409} - API key already exists</li>
   * <li>{@code 500} - When there is an error in saving the properties file or API keys instance is not initialized</li>
   * </ul>
   */
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  public Response createApiKey(ApiKeyDTO apiKeyDTO) {
    String apiKey = apiKeyDTO.getApiKey();
    String clientDn = apiKeyDTO.getClientDn();
    if (apiKey == null || clientDn == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      authProvider.addNewApiKeyAndUpdatePropertiesFile(apiKey, clientDn);
      return Response.status(Status.CREATED).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Status.CONFLICT).build();
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  /**
   * Activates an API key.
   *
   * @param apiKey The API key to activate.
   * @return A Response indicating the result of the operation.
   */
  @POST
  @Path("{apiKey}/activate")
  public Response activateApiKey(@PathParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.ACTIVE);
  }

  /**
   * Deactivates an API key.
   *
   * @param apiKey The API key to deactivate.
   * @return A Response indicating the result of the operation.
   */
  @POST
  @Path("{apiKey}/deactivate")
  public Response deactivateApiKey(@PathParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.INACTIVE);
  }

  /**
   * Sets the status of an API key.
   *
   * @param apiKey The API key to update.
   * @param status The new status for the API key.
   * @return A Response indicating the result of the operation: <ul>
   * <li>{@code 200} - Status was changed or API key was already in that state</li>
   * <li>{@code 404} - API key does not exist</li>
   * <li>{@code 403} - When attempting to modify an admin API key</li>
   * <li>{@code 400} - When ApiKeyStatus is unknown, or apiKey or status are not provided</li>
   * <li>{@code 500} - When there is an error in saving the properties file or API keys instance is not initialized</li>
   * </ul>
   */
  private Response setApiKeyStatus(String apiKey, ApiKeyStatus status) {
    if (apiKey == null || status == null) {
      return Response.status(Status.BAD_REQUEST).build();
    }
    try {
      authProvider.setStateOfApiKeyAndUpdatePropertiesFile(apiKey, status);
      return Response.ok().build();
    } catch (NoSuchElementException e) {
      return Response.status(Status.NOT_FOUND).build();
    } catch (SecurityException e) {
      return Response.status(Status.FORBIDDEN).build();
    } catch (IllegalArgumentException e) {
      return Response.status(Status.BAD_REQUEST).build();
    } catch (IOException e) {
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }
}
