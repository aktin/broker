package org.aktin.broker.admin.standalone;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.aktin.broker.auth.CascadedAuthProvider;
import org.aktin.broker.auth.cred2.CredentialTokenAuthProvider;
import org.aktin.broker.server.auth.AuthProvider;

public class Cred2TestConfiguration implements Configuration {

  private AuthProvider authProvider;

  public Cred2TestConfiguration() throws IOException {
    authProvider = useDevAuthentication();
  }

  private static AuthProvider useDevAuthentication() {
    List<AuthProvider> auths = new ArrayList<>();
    auths.add(new CredentialTokenAuthProvider());
    return new CascadedAuthProvider(auths);
  }

  public static void main(String[] args) throws Exception {
    int port = (args.length > 0) ? Integer.parseInt(args[0]) : 8080;

    // define password for test instance
    System.setProperty("org.eclipse.jetty.util.log.class", "org.eclipse.jetty.util.log.JavaUtilLog");
    System.setProperty("aktin.broker.password", "test");
    Class.forName("org.hsqldb.jdbcDriver");

    // start server
    Cred2TestConfiguration config = new Cred2TestConfiguration();
    HttpServer http = new HttpServer(config);
    try {
      http.start(new InetSocketAddress(port));
      System.out.println("Broker service at: " + http.getBrokerServiceURI());
      http.join();
    } finally {
      http.destroy();
    }
  }

  @Override
  public AuthProvider getAuthProvider() {
    return authProvider;
  }

  @Override
  public Path getBasePath() {
    return Paths.get("target");
  }

  @Override
  public String getAggregatorDataPath() {
    return "target/aggregator-data";
  }

  @Override
  public String getBrokerDataPath() {
    return "target/broker-data";
  }

  @Override
  public String getTempDownloadPath() {
    return "target/download-temp";
  }

  @Override
  public int getPort() {
    return 8080;
  }

  @Override
  public long getWebsocketIdleTimeoutMillis() {
    return 30000;
  }

  @Override
  public Class<? extends DataSource> getJdbcDataSourceClass() {
    return DefaultConfiguration.getDefaultHsqlDataSource();
  }

  @Override
  public String getJdbcUrl() {
    return DefaultConfiguration.getDefaultHsqlJdbcUrl(getBasePath());
  }
}
