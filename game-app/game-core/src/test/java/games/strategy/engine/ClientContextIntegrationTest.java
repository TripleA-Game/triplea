package games.strategy.engine;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;

import games.strategy.engine.framework.map.listing.MapListingFetcher;
import games.strategy.triplea.settings.AbstractClientSettingTestCase;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.triplea.http.client.maps.listing.MapDownloadListing;

class ClientContextIntegrationTest extends AbstractClientSettingTestCase {

  @Test
  void downloadListOfAvailableMaps() {
    final List<MapDownloadListing> list = MapListingFetcher.getMapDownloadList();

    assertThat(list, notNullValue());
    assertThat(list, not(empty()));
  }
}
