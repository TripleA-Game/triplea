package games.strategy.engine.config.client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import games.strategy.engine.config.PropertyFileReader;
import games.strategy.engine.config.client.backup.BackupPropertyFetcher;
import games.strategy.engine.config.client.remote.LobbyServerPropertiesFetcher;
import games.strategy.engine.lobby.client.login.LobbyServerProperties;
import games.strategy.util.Version;

@RunWith(MockitoJUnitRunner.class)
public class GameEnginePropertyReaderTest {


  @Mock
  private PropertyFileReader mockPropertyFileReader;
  @Mock
  private LobbyServerPropertiesFetcher mockLobbyServerPropertiesFetcher;
  @Mock
  private BackupPropertyFetcher mockBackupPropertyFetcher;

  private GameEnginePropertyReader testObj;



  /**
   * Sets up a test object with mocked dependencies.
   */
  @Before
  public void setup() {
    testObj = new GameEnginePropertyReader(
        mockPropertyFileReader,
        mockLobbyServerPropertiesFetcher,
        mockBackupPropertyFetcher);
  }

  @Test
  public void engineVersion() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.ENGINE_VERSION))
        .thenReturn("1.0.1.3");

    assertThat(testObj.getEngineVersion(), is(new Version(1, 0, 1, 3)));
  }

  @Test
  public void mapListingSource() {
    final String value = "something_test";
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.MAP_LISTING_SOURCE_FILE))
        .thenReturn(value);

    assertThat(testObj.getMapListingSource(), is(value));
  }

  @Test
  public void lobbyServerProperties() throws Exception {
    givenSuccessfullyParsedPropertiesUrl();

    when(mockLobbyServerPropertiesFetcher.downloadAndParseRemoteFile(TestData.fakePropUrl, TestData.fakeVersion))
        .thenReturn(TestData.fakeProps);

    final LobbyServerProperties result = testObj.fetchLobbyServerProperties();

    assertThat(result, sameInstance(TestData.fakeProps));
  }

  private void givenSuccessfullyParsedPropertiesUrl() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.LOBBY_PROP_FILE_URL))
        .thenReturn(TestData.fakePropUrl);

    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.ENGINE_VERSION))
        .thenReturn(TestData.fakeVersionString);
  }

  @Test
  public void lobbyServerPropertiesUsingBackupCase() throws Exception {
    givenSuccessfullyParsedPropertiesUrl();

    when(mockLobbyServerPropertiesFetcher.downloadAndParseRemoteFile(TestData.fakePropUrl, TestData.fakeVersion))
        .thenThrow(new IOException("simulated io exception"));

    givenSuccessfullyParsedBackupValues();


    final LobbyServerProperties result = testObj.fetchLobbyServerProperties();

    assertThat(result, sameInstance(TestData.fakeProps));
  }

  private void givenSuccessfullyParsedBackupValues() {
    when(mockPropertyFileReader.readProperty(GameEnginePropertyReader.PropertyKeys.LOBBY_BACKUP_HOST_ADDRESS))
        .thenReturn(TestData.backupAddress);

    when(mockBackupPropertyFetcher.parseBackupValuesFromEngineConfig(TestData.backupAddress))
        .thenReturn(TestData.fakeProps);
  }

  private interface TestData {
    String fakeVersionString = "12.12.12.12";
    Version fakeVersion = new Version(fakeVersionString);
    String fakePropUrl = "http://fake";
    String backupAddress = "backup";
    LobbyServerProperties fakeProps = new LobbyServerProperties("host", 123);
  }

}
