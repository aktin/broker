package org.aktin.broker.auth.apikey;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
}
