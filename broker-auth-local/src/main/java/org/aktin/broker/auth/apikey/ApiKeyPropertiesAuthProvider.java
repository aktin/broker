package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import org.aktin.broker.server.auth.AbstractAuthProvider;

/**
 * Provides authentication services for API keys stored in a properties file.
 */
public class ApiKeyPropertiesAuthProvider extends AbstractAuthProvider {

  private static final int API_KEY_LENGTH = 12;
  private static final Pattern CLIENT_DN_PATTERN = Pattern.compile("CN=[^,]+,O=[^,]+,L=[^,]+");
  private static final String ADMIN_IDENTIFIER = "OU=admin";

  private PropertyFileAPIKeys keys;

  /**
   * Default constructor. Initializes the provider with lazy loading of API keys.
   */
  public ApiKeyPropertiesAuthProvider() {
    keys = null;
  }

  /**
   * Constructor that immediately loads API keys from the provided input stream.
   *
   * @param in The input stream containing API key data
   * @throws IOException If there's an error reading from the input stream
   */
  public ApiKeyPropertiesAuthProvider(InputStream in) throws IOException {
    this.keys = new PropertyFileAPIKeys(in);
  }

  /**
   * Retrieves the instance of PropertyFileAPIKeys, initializing it if necessary.
   *
   * @return The PropertyFileAPIKeys instance
   * @throws IOException If there's an error reading the properties file
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
   * @return The Path object representing the properties file location
   */
  private Path getPropertiesPath() {
    return path.resolve("api-keys.properties");
  }

  /**
   * Stores a new API key or updates an existing one, then saves the changes.
   *
   * @param apiKey   The API key to store or update
   * @param clientDn The client's Distinguished Name
   * @throws IOException              If there's an error saving the properties file
   * @throws IllegalArgumentException If the API key or client DN is invalid
   * @throws IllegalStateException    If the instance of PropertyFileAPIKeys is not initialized
   */
  public void storeApiKeyAndUpdatePropertiesFile(String apiKey, String clientDn) throws IOException {
    ensureKeysInitialized();
    validateApiKey(apiKey);
    validateClientDn(clientDn);
    Properties properties = keys.getProperties();
    if (properties.containsKey(apiKey)) {
      updateClientDnOfExistingKey(apiKey, clientDn, properties);
    } else {
      addNewKey(apiKey, clientDn);
    }
    saveProperties(keys);
  }

  /**
   * Updates an existing API key with a new client DN. The state of the API key is unaffected by the update.
   *
   * @param apiKey     The API key to update
   * @param clientDn   The new client DN
   * @param properties The properties containing the existing key
   * @throws IllegalArgumentException If attempting to modify an admin key
   */
  private void updateClientDnOfExistingKey(String apiKey, String clientDn, Properties properties) {
    String property = properties.getProperty(apiKey);
    String[] parts = property.split(",");
    String existingClientDn = parts[0] + "," + parts[1] + "," + parts[2];
    ApiKeyStatus oldStatus = ApiKeyStatus.fromString(parts[3]);
    if (isAdminKey(existingClientDn)) {
      throw new IllegalArgumentException("API key cannot be modified");
    }
    keys.putApiKey(apiKey, clientDn, oldStatus);
  }

  /**
   * Adds a new API key. The default state for new API keys is "active".
   *
   * @param apiKey   The new API key to add
   * @param clientDn The client DN for the new key
   */
  private void addNewKey(String apiKey, String clientDn) {
    keys.putApiKey(apiKey, clientDn, ApiKeyStatus.ACTIVE);
  }

  /**
   * Updates the status of an existing API key, then saves the changes.
   *
   * @param apiKey The API key to update
   * @param status The new status for the key
   * @throws IOException              If there's an error saving the properties file
   * @throws IllegalArgumentException If the API key is not found
   * @throws IllegalStateException    If the instance of PropertyFileAPIKeys is not initialized
   */
  public void setStateOfApiKeyAndUpdatePropertiesFile(String apiKey, ApiKeyStatus status) throws IOException {
    ensureKeysInitialized();
    Properties properties = keys.getProperties();
    if (properties.containsKey(apiKey)) {
      updateKeyStatus(apiKey, status, properties);
      saveProperties(keys);
    } else {
      throw new IllegalArgumentException("API key not found: " + apiKey);
    }
  }

  /**
   * Updates the status of an API key.
   *
   * @param apiKey     The API key to update
   * @param status     The new status
   * @param properties The properties containing the key
   * @throws IllegalArgumentException If attempting to modify an admin key
   */
  private void updateKeyStatus(String apiKey, ApiKeyStatus status, Properties properties) {
    String property = properties.getProperty(apiKey);
    String[] parts = property.split(",");
    String clientDn = parts[0] + "," + parts[1] + "," + parts[2];
    if (isAdminKey(clientDn)) {
      throw new IllegalArgumentException("API key state cannot be modified");
    }
    keys.putApiKey(apiKey, clientDn, status);
  }

  /**
   * Ensures that the instance of PropertyFileAPIKeys has been initialized.
   *
   * @throws IllegalStateException If the keys are not initialized
   */
  private void ensureKeysInitialized() {
    if (this.keys == null) {
      throw new IllegalStateException("API keys instance is not initialized");
    }
  }

  /**
   * Validates the format of an API key to project-internal constraints.
   *
   * @param apiKey The API key to validate
   * @throws IllegalArgumentException If the API key is invalid
   */
  private void validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.length() != API_KEY_LENGTH) {
      throw new IllegalArgumentException("API key must be exactly " + API_KEY_LENGTH + " characters long");
    }
    if (!apiKey.matches("^[a-zA-Z0-9]{" + API_KEY_LENGTH + "}$")) {
      throw new IllegalArgumentException("API key must contain only alphanumeric characters");
    }
  }

  /**
   * Validates the format of a client DN to project-internal constraints.
   *
   * @param clientDn The client DN to validate
   * @throws IllegalArgumentException If the client DN is invalid
   */
  private void validateClientDn(String clientDn) {
    if (clientDn == null || !CLIENT_DN_PATTERN.matcher(clientDn).matches()) {
      throw new IllegalArgumentException("Client DN must follow the format: CN=X,O=Y,L=Z");
    }
  }

  /**
   * Checks if a given client DN belongs to an admin.
   *
   * @param clientDn The client DN to check
   * @return true if the client DN belongs to an admin, false otherwise
   */
  private boolean isAdminKey(String clientDn) {
    return clientDn != null && clientDn.contains(ADMIN_IDENTIFIER);
  }

  /**
   * Saves the current state of the API keys to the properties file.
   *
   * @param instance The PropertyFileAPIKeys instance to save
   * @throws IOException If there's an error writing to the properties file
   */
  private void saveProperties(PropertyFileAPIKeys instance) throws IOException {
    try (OutputStream out = Files.newOutputStream(getPropertiesPath())) {
      instance.storeProperties(out, StandardCharsets.ISO_8859_1);
    }
  }

  /**
   * Returns the array of endpoint classes associated with this provider.
   *
   * @return An array containing the ApiKeyManagementEndpoint class
   */
  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{ApiKeyManagementEndpoint.class};
  }

  /**
   * Binds this instance to the provided binder (including the set endpoint classes in {@link #getEndpoints()})
   *
   * @param binder The binder to use for binding this instance
   */
  @Override
  public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
    binder.accept(this, ApiKeyPropertiesAuthProvider.class);
  }
}
