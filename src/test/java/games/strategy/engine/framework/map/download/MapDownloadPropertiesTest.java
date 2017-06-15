package games.strategy.engine.framework.map.download;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.config.GameEnginePropertyReader;

/**
 * Basic test of map listing source, make sure that the test object requests a specific property key,
 * we fake a return value, then verify that we get the same faked value back when calling 'getMapListDownloadSite()'.
 */
@RunWith(MockitoJUnitRunner.class)
public class MapDownloadPropertiesTest {

  private static final String SAMPLE_VALUE = "http://this is a test value.txt";

  @Mock
  private GameEnginePropertyReader mockReader;

  @Test
  public void mapListDownloadSitePropertyIsReadFromPropertyFile() {
    when(mockReader.readMapListingDownloadUrl()).thenReturn(SAMPLE_VALUE);
    final MapListingSource testObj = new MapListingSource(mockReader);
    assertThat(testObj.getMapListDownloadSite(), is(SAMPLE_VALUE));
  }
}
