package games.strategy.engine.framework.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.EngineVersionException;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.GameParser;
import games.strategy.triplea.Constants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.UrlStreams;

public class NewGameChooserEntry {
  private final URI url;
  private GameData gameData;
  private boolean gameDataFullyLoaded = false;
  private String gameNameAndMapNameProperty;

  static Comparator<NewGameChooserEntry> getComparator() {
    return new Comparator<NewGameChooserEntry>() {
      @Override
      public int compare(final NewGameChooserEntry o1, final NewGameChooserEntry o2) {
        return getLowerCaseComparable(o1).compareTo(getLowerCaseComparable(o2));
      }

      private String getLowerCaseComparable(final NewGameChooserEntry newGameChooserEntry) {
        return newGameChooserEntry.getGameData().getGameName().toLowerCase();
      }
    };
  }

  public NewGameChooserEntry() {
    this(defaultGameUri());
  }


  private static URI defaultGameUri() {
    final String defaultLocation = ClientSetting.SELECTED_GAME_LOCATION.value();
    try {
      return new URI(defaultLocation);
    } catch (final URISyntaxException e) {
      throw new IllegalStateException("Illegal default game location: " + defaultLocation, e);
    }
  }

  public NewGameChooserEntry(final URI uri) {
    url = uri;
    final AtomicReference<String> gameName = new AtomicReference<>();

    final Optional<InputStream> inputStream = UrlStreams.openStream(uri);
    if (!inputStream.isPresent()) {
      gameNameAndMapNameProperty = "";
      // this means the map was deleted out from under us.
      return;
    }

    try {
      try (InputStream input = inputStream.get()) {
        final boolean delayParsing = true;
        gameData = new GameParser(uri.toString()).parse(input, gameName, delayParsing);
        gameDataFullyLoaded = false;
        gameNameAndMapNameProperty = getGameName() + ":" + getMapNameProperty();
      }
    } catch (final IOException | GameParseException | SAXException | EngineVersionException e) {
      ClientLogger.logError("Error reading: " + uri + ", game name: " + gameName, e);
    }
  }

  /**
   * 'Fully' parses the currently loaded game data XML.
   * This must be done before the game is launched.
   */
  public GameData fullyParseGameData() {
    try {
      return fullyParseGameDataThrowing();
    } catch (final GameParseException e) {
      throw new RuntimeException(e);
    }
  }

  private GameData fullyParseGameDataThrowing() throws GameParseException {
    // TODO: We should be setting this in the the constructor. At this point, you have to call methods in the
    // correct order for things to work, and that is bads.
    gameData = null;

    final AtomicReference<String> gameName = new AtomicReference<>();

    final Optional<InputStream> inputStream = UrlStreams.openStream(url);
    if (!inputStream.isPresent()) {
      return gameData;
    }

    try (InputStream input = inputStream.get()) {
      gameData = new GameParser(url.toString()).parse(input, gameName, false);
      gameDataFullyLoaded = true;
      return gameData;
    } catch (final EngineVersionException e) {
      ClientLogger.logQuietly(e);
      throw new GameParseException(e.getMessage());
    } catch (final SAXParseException e) {
      final String msg =
          "Could not parse:" + url + " error at line:" + e.getLineNumber() + " column:" + e.getColumnNumber();
      ClientLogger.logError(msg, e);
      throw new GameParseException(e.getMessage());
    } catch (final Exception e) {
      final String msg = "Could not parse:" + url;
      ClientLogger.logError(msg, e);
      throw new GameParseException(e.getMessage());
    }
  }

  public boolean isGameDataLoaded() {
    return gameDataFullyLoaded;
  }

  public String getGameName() {
    return gameData.getGameName();
  }

  // the user may have selected a map skin instead of this map folder, so don't use this for anything except our
  // equals/hashcode below
  private String getMapNameProperty() {
    final String mapName = (String) gameData.getProperties().get(Constants.MAP_NAME);
    if (mapName == null || mapName.trim().length() == 0) {
      throw new IllegalStateException("Map name property not set on game");
    }
    return mapName;
  }

  @Override
  public String toString() {
    return getGameName();
  }

  public GameData getGameData() {
    return gameData;
  }

  public URI getUri() {
    return url;
  }

  /**
   * Returns the location of the game file.
   *
   * <p>
   * The "location" is actually a URI in string form.
   * </p>
   *
   * @return The location of the game file; never {@code null}.
   */
  public String getLocation() {
    return url.toString();
  }

  @Override
  public int hashCode() {
    return gameNameAndMapNameProperty.hashCode();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    final NewGameChooserEntry other = (NewGameChooserEntry) obj;
    if (gameData == null && other.gameData != null) {
      return false;
    } else {
      if (other.gameData == null) {
        return false;
      }
    }
    return this.gameNameAndMapNameProperty.equals(other.gameNameAndMapNameProperty);
  }
}
