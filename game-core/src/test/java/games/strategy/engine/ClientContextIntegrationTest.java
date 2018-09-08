package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;

import org.junit.jupiter.api.Test;
import org.triplea.test.common.Integration;

import games.strategy.triplea.settings.AbstractClientSettingTestCase;

@Integration
public class ClientContextIntegrationTest extends AbstractClientSettingTestCase {

  @Test
  public void verifyClientContext() {
    assertThat(ClientContext.downloadCoordinator(), notNullValue());
    assertThat(ClientContext.engineVersion(), notNullValue());
    assertThat(ClientContext.mapDownloadController(), notNullValue());

    assertThat(ClientContext.getMapDownloadList(), notNullValue());
    assertThat(ClientContext.getMapDownloadList().isEmpty(), is(false));
  }
}
