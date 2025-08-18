package org.aktin.broker.auth.cred2.utils;

import java.util.Objects;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.aktin.broker.auth.cred2.auth.TokenManager;

public class TokenPruneService implements AutoCloseable {

  private static final Logger log = Logger.getLogger(TokenPruneService.class.getName());

  private static final String PROPERTY_PRUNE_PERIOD = "aktin.broker.token.prune";
  private static final long DEFAULT_PRUNE_PERIOD_SECONDS = 360L;

  private final TokenManager manager;
  private final long prunePeriod;
  private ScheduledExecutorService executor;

  public TokenPruneService(TokenManager manager) {
    this(manager, Long.getLong(PROPERTY_PRUNE_PERIOD, DEFAULT_PRUNE_PERIOD_SECONDS));
  }

  public TokenPruneService(TokenManager manager, long prunePeriod) {
    this.manager = Objects.requireNonNull(manager);
    if (prunePeriod <= 0) {
      throw new IllegalArgumentException("Token prune period must be > 0");
    }
    this.prunePeriod = prunePeriod;
  }

  public synchronized TokenPruneService start() {
    if (executor != null) {
      return this;
    }
    executor = Executors.newSingleThreadScheduledExecutor(r -> {
      Thread t = new Thread(r, "token-pruner");
      t.setDaemon(true);
      return t;
    });
    executor.scheduleAtFixedRate(this::prune, prunePeriod, prunePeriod, TimeUnit.SECONDS);
    log.info(String.format("TokenPruneService started: every %s seconds", prunePeriod));
    return this;
  }

  private void prune() {
    try {
      manager.pruneInvalid();
    } catch (Throwable t) {
      log.log(Level.SEVERE, "Token prune failed", t);
    }
  }

  @Override
  public void close() {
    if (executor != null) {
      executor.shutdown();
      executor = null;
      log.info("TokenPruneService closed");
    }
  }
}
