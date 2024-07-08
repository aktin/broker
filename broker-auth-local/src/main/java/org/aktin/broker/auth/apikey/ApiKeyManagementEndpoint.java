package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
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
      return Response.status(Status.INTERNAL_SERVER_ERROR).build();
    }
  }

  private String convertPropertiesToString(Properties props) {
    StringBuilder response = new StringBuilder();
    for (String name : props.stringPropertyNames()) {
      response.append(name).append("=").append(props.getProperty(name)).append("\n");
    }
    return response.toString();
  }

  /*
  201 - Apikey created
  400 - When apiKey or clientdn are not provided
  409 - When the API key already exists
  500 - When there is an error in saving the properties file or API keys instance is not initialized.
 */
  @POST
  @Consumes(MediaType.APPLICATION_XML)
  public Response postApiKey(ApiKeyDTO apiKeyDTO) {
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

  @PUT
  @Path("{apiKey}/activate")
  public Response activateApiKey(@PathParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.ACTIVE);
  }

  @PUT
  @Path("{apiKey}/deactivate")
  public Response deactivateApiKey(@PathParam("apiKey") String apiKey) {
    return setApiKeyStatus(apiKey, ApiKeyStatus.INACTIVE);
  }

  /*
  200 - status was changed or apikey was already in that state
  404 - When the API key does not exist.
  403 - When attempting to modify an admin API key
  400 - When ApiKeyStatus is unknown, or apiKey or status are not provided
  500 - When there is an error in saving the properties file or API keys instance is not initialized.
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
