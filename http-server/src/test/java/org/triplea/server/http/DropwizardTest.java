package org.triplea.server.http;

import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.GLOBAL;

import com.github.database.rider.core.api.dataset.DataSet;
import com.github.database.rider.junit5.DBUnitExtension;
import io.dropwizard.testing.DropwizardTestSupport;
import java.net.URI;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.triplea.test.common.Integration;

/** Core configuration for a test that will start a dropwizard server and initialize database. */
@Integration
@DataSet(cleanBefore = true, value = "integration.yml")
@ExtendWith(DBUnitExtension.class)
@ExtendWith(DropwizardTest.DropwizardServerExtension.class)
abstract class DropwizardTest {
  final URI localhost = URI.create("http://localhost:8080");

  /**
   * Extension to start a drop wizard server before all tests and then shuts it down afterwards.
   * Note, if a server is already running, then that server is used.
   */
  public static class DropwizardServerExtension
      implements BeforeAllCallback, ExtensionContext.Store.CloseableResource {
    private static boolean started = false;

    private static final DropwizardTestSupport<AppConfig> support =
        new DropwizardTestSupport<>(ServerApplication.class, "configuration-prerelease.yml");

    @Override
    public void beforeAll(final ExtensionContext context) {
      if (!started) {
        started = true;
        try {
          support.before();
        } catch (final RuntimeException e) {
          // ignore, server is already started
        }
        context.getRoot().getStore(GLOBAL).put("dropwizard-startup", this);
      }
    }

    @Override
    public void close() {
      support.after();
    }
  }
}
