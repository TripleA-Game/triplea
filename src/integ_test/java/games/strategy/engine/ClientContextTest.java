package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import static org.hamcrest.core.Is.is;

import org.junit.Test;

public class ClientContextTest {

  @Test
  public void verifyClientContext() {
    assertThat(ClientContext.aiSettings(), notNullValue());
    assertThat(ClientContext.battleCalcSettings(), notNullValue());
    assertThat(ClientContext.battleOptionsSettings(), notNullValue());
    assertThat(ClientContext.downloadCoordinator(), notNullValue());
    assertThat(ClientContext.engineVersion(), notNullValue());
    assertThat(ClientContext.folderSettings(), notNullValue());
    assertThat(ClientContext.gameEnginePropertyReader(), notNullValue());
    assertThat(ClientContext.mapDownloadController(), notNullValue());
    assertThat(ClientContext.scrollSettings(), notNullValue());

    assertThat(ClientContext.getMapDownloadList(), notNullValue());
    assertThat(ClientContext.getMapDownloadList().isEmpty(), is(false));
  }

}
