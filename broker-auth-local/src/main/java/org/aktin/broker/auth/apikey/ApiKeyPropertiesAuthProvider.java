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

public class ApiKeyPropertiesAuthProvider extends AbstractAuthProvider {

  private PropertyFileAPIKeys keys;

  public ApiKeyPropertiesAuthProvider() {
    keys = null; // lazy init, load later in getInstance by using supplied path
  }

  public ApiKeyPropertiesAuthProvider(InputStream in) throws IOException {
    this.keys = new PropertyFileAPIKeys(in);
  }

  @Override
  public PropertyFileAPIKeys getInstance() throws IOException {
    if (this.keys == null) {
      try (InputStream in = Files.newInputStream(getPropertiesPath())) {
        this.keys = new PropertyFileAPIKeys(in);
      }
    }
    return keys;
  }

  private Path getPropertiesPath() {
    return path.resolve("api-keys.properties");
  }

  public void addNewApiKeyAndUpdatePropertiesFile(String apiKey, String clientDn) throws IOException {
    checkKeysInitialized();
    Properties properties = keys.getProperties();
    if (properties.containsKey(apiKey)) {
      throw new IllegalArgumentException("API key already exists: " + apiKey);
    }
    keys.putApiKey(apiKey, clientDn);
    saveProperties(keys);
  }

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

  private void checkNotAdminKey(String clientDn) {
    if (clientDn != null && clientDn.contains("OU=admin")) {
      throw new SecurityException("Admin API key state cannot be modified");
    }
  }

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

  private void checkKeysInitialized() {
    if (this.keys == null) {
      throw new IllegalStateException("API keys instance is not initialized");
    }
  }

  private void saveProperties(PropertyFileAPIKeys instance) throws IOException {
    try (OutputStream out = Files.newOutputStream(getPropertiesPath())) {
      instance.storeProperties(out, StandardCharsets.ISO_8859_1);
    }
  }

  @Override
  public Class<?>[] getEndpoints() {
    return new Class<?>[]{ApiKeyManagementEndpoint.class};
  }

  @Override
  public void bindSingletons(BiConsumer<Object, Class<?>> binder) {
    binder.accept(this, ApiKeyPropertiesAuthProvider.class);
  }
}
