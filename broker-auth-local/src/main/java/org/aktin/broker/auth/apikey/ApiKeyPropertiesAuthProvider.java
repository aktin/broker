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

public class ApiKeyPropertiesAuthProvider extends AbstractAuthProvider {

  private static final int API_KEY_LENGTH = 12;
  private static final Pattern CLIENT_DN_PATTERN = Pattern.compile("CN=[^,]+,O=[^,]+,L=[^,]+");

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

  public void storeApiKeyAndUpdatePropertiesFile(String apiKey, String clientDn) throws IOException {
    validateApiKey(apiKey);
    validateClientDn(clientDn);
    if (this.keys != null) {
      Properties properties = keys.getProperties();
      if (properties.containsKey(apiKey)) {
        String property = properties.getProperty(apiKey);
        String[] parts = property.split(",");
        ApiKeyStatus oldStatus = ApiKeyStatus.fromString(parts[3]);
        keys.putApiKey(apiKey, clientDn, oldStatus);
      } else {
        keys.putApiKey(apiKey, clientDn, ApiKeyStatus.ACTIVE);
      }
      saveProperties(keys);
    } else {
      throw new IllegalStateException("API keys instance is not initialized");
    }
  }

  private void validateApiKey(String apiKey) {
    if (apiKey == null || apiKey.length() != API_KEY_LENGTH) {
      throw new IllegalArgumentException("API key must be exactly " + API_KEY_LENGTH + " characters long");
    }
    if (!apiKey.matches("^[a-zA-Z0-9]{" + API_KEY_LENGTH + "}$")) {
      throw new IllegalArgumentException("API key must contain only alphanumeric characters");
    }
  }

  private void validateClientDn(String clientDn) {
    if (clientDn == null || !CLIENT_DN_PATTERN.matcher(clientDn).matches()) {
      throw new IllegalArgumentException("Client DN must follow the format: CN=X,O=Y,L=Z");
    }
  }

  private void saveProperties(PropertyFileAPIKeys instance) throws IOException {
    try (OutputStream out = Files.newOutputStream(getPropertiesPath())) {
      instance.storeProperties(out, StandardCharsets.ISO_8859_1);
    }
  }

  public void setStateOfApiKeyAndUpdatePropertiesFile(String apiKey, ApiKeyStatus status) throws IOException {
    if (this.keys != null) {
      Properties properties = keys.getProperties();
      if (properties.containsKey(apiKey)) {
        String property = properties.getProperty(apiKey);
        String[] parts = property.split(",");
        String clientDn = parts[0] + "," + parts[1] + "," + parts[2];
        keys.putApiKey(apiKey, clientDn, status);
        saveProperties(keys);
      } else {
        throw new IllegalArgumentException("API key not found: " + apiKey);
      }
    } else {
      throw new IllegalStateException("API keys instance is not initialized");
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
