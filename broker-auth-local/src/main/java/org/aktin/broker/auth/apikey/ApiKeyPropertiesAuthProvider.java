package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.function.BiConsumer;
import org.aktin.broker.server.auth.AbstractAuthProvider;

/**
 * Provides authentication services using API keys stored in a properties file. This class extends {@link AbstractAuthProvider} and manages the
 * lifecycle of API keys, including loading, adding, updating, and persisting them.
 *
 * @author akombeiz@ukaachen.de
 */
public class ApiKeyPropertiesAuthProvider extends AbstractAuthProvider {

  private PropertyFileAPIKeys keys;

  /**
   * Constructs a new ApiKeyPropertiesAuthProvider with lazy initialization. The API keys will be loaded later when getInstance() is called.
   */
  public ApiKeyPropertiesAuthProvider() {
    keys = null;
  }

  /**
   * Constructs a new ApiKeyPropertiesAuthProvider and immediately loads API keys from the given input stream.
   *
   * @param in The input stream containing the API keys properties.
   * @throws IOException If an I/O error occurs while reading from the input stream.
   */
  public ApiKeyPropertiesAuthProvider(InputStream in) throws IOException {
    this.keys = new PropertyFileAPIKeys(in);
  }

  /**
   * Retrieves or initializes the PropertyFileAPIKeys instance.
   *
   * @return The PropertyFileAPIKeys instance.
   * @throws IOException If an I/O error occurs while loading the properties file.
   */
  @Override
  public PropertyFileAPIKeys getInstance() throws IOException {
    if (this.keys == null) {
      try (InputStream in = Files.newInputStream(getPropertiesPath())) {
        this.keys = new PropertyFileAPIKeys(in);
      }
    }
    return keys;
  }

  /**
   * @return The Path object representing the location of API keys properties file
   */
  private Path getPropertiesPath() {
    return path.resolve("api-keys.properties");
  }

  /**
   * Adds a new API key and updates the properties file.
   *
   * @param apiKey   The new API key to add.
   * @param clientDn The distinguished name of the client associated with the API key.
   * @throws IOException              If an I/O error occurs while updating the properties file.
   * @throws IllegalArgumentException If the API key already exists.
   * @throws IllegalStateException    If the PropertyFileAPIKeys instance is not initialized.
   */
  public void addNewApiKeyAndUpdatePropertiesFile(String apiKey, String clientDn) throws IOException {
    checkKeysInitialized();
    Properties properties = keys.getProperties();
    if (properties.containsKey(apiKey)) {
      throw new IllegalArgumentException("API key already exists: " + apiKey);
    }
    keys.putApiKey(apiKey, clientDn);
    saveProperties(keys);
  }

  /**
   * Sets the state of an existing API key and updates the properties file.
   *
   * @param apiKey The API key to update.
   * @param status The new status to set for the API key.
   * @throws IOException            If an I/O error occurs while updating the properties file.
   * @throws NoSuchElementException If the API key does not exist.
   * @throws SecurityException      If an attempt is made to modify an admin API key.
   * @throws IllegalStateException  If the PropertyFileAPIKeys instance is not initialized.
   */
  public void setStateOfApiKeyAndUpdatePropertiesFile(String apiKey, ApiKeyStatus status) throws IOException {
    checkKeysInitialized();
    Properties properties = keys.getProperties();
    if (!properties.containsKey(apiKey)) {
      throw new NoSuchElementException("API key does not exist");
    }
    String clientDn = properties.getProperty(apiKey);
    checkNotAdminKey(clientDn);
    String updatedClientDn = setStatusInClientDn(clientDn, status);
    if (!clientDn.equals(updatedClientDn)) {
      keys.putApiKey(apiKey, updatedClientDn);
      saveProperties(keys);
    }
  }

  /**
   * Checks if the given client DN belongs to an admin key.
   *
   * @param clientDn The client distinguished name to check.
   * @throws SecurityException If the client DN indicates an admin key.
   */
  private void checkNotAdminKey(String clientDn) {
    if (clientDn != null && clientDn.contains("OU=admin")) {
      throw new SecurityException("Admin API key state cannot be modified");
    }
  }

  /**
   * Updates the status in the client DN string.
   *
   * @param clientDn The original client DN.
   * @param status   The new status to set.
   * @return The updated client DN string with the new status.
   * @throws IllegalArgumentException If an unknown status is provided.
   */
  private String setStatusInClientDn(String clientDn, ApiKeyStatus status) {
    switch (status) {
      case ACTIVE:
        return clientDn.replace("," + ApiKeyStatus.INACTIVE.name(), "");
      case INACTIVE:
        return clientDn.endsWith(ApiKeyStatus.INACTIVE.name()) ? clientDn : clientDn + "," + ApiKeyStatus.INACTIVE.name();
      default:
        throw new IllegalArgumentException("Unknown status: " + status.name());
    }
  }

  /**
   * Checks if the API keys instance is initialized.
   *
   * @throws IllegalStateException If the API keys instance is not initialized.
   */
  private void checkKeysInitialized() {
    if (this.keys == null) {
      throw new IllegalStateException("API keys instance is not initialized");
    }
  }

  /**
   * Saves the current state of API keys to the properties file in a latin-1 encoding.
   *
   * @param instance The PropertyFileAPIKeys instance to save.
   * @throws IOException If an I/O error occurs while writing to the properties file.
   */
  private void saveProperties(PropertyFileAPIKeys instance) throws IOException {
    try (OutputStream out = Files.newOutputStream(getPropertiesPath())) {
      instance.storeProperties(out, StandardCharsets.ISO_8859_1);
    }
  }

  /**
   * Returns the array of endpoint classes associated with this auth provider.
   *
   * @return An array containing the ApiKeyManagementEndpoint class.
   */
  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{ApiKeyManagementEndpoint.class};
  }

  /**
   * Binds this instance to the ApiKeyPropertiesAuthProvider class so it can be injected into the associated endpoints of {@link #getEndpoints()}.
   *
   * @param binder The binder function to use for binding.
   */
  @Override
  public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
    binder.accept(this, ApiKeyPropertiesAuthProvider.class);
  }
}
