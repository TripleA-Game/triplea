package org.triplea.client.ui.javafx;

import java.net.URL;
import java.util.Locale;
import java.util.ResourceBundle;

import javafx.fxml.FXMLLoader;

/**
 * Enum with relative Paths to fxml-related resources.
 */
enum FxmlManager {
  ROOT_CONTAINER("./fxml/TripleAMain.fxml"),

  DOWNLOAD_PANE("./fxml/TripleADownload.fxml"),

  MAIN_MANU_PANE("./fxml/TripleAMainMenu.fxml"),

  SETTINGS_PANE("./fxml/TripleASettings.fxml"),

  LANG_CLASS_BASENAME("org.triplea.client.ui.javafx.lang.TripleA"),

  STYLESHEET_MAIN("/org/triplea/client/ui/javafx/css/main.css"),

  RELATIVE_FONT_PATH("./css/fonts/1942-report.ttf"),

  ICON_LOCATION("/games/strategy/engine/framework/ta_icon.png");

  private final String value;

  FxmlManager(final String value) {
    this.value = value;
  }

  @Override
  public String toString() {
    return value;
  }

  /**
   * Simplified way of getting an {@link FXMLLoader} with the default settings for TripleA.
   *
   * @param location The FXML File to load
   * @return An FXMLLoader object
   */
  static FXMLLoader getLoader(final URL location) {
    final FXMLLoader loader = new FXMLLoader();
    loader.setLocation(location);
    // TODO load locale based on user setting
    loader.setResources(ResourceBundle.getBundle(LANG_CLASS_BASENAME.toString(), new Locale("en", "US")));
    return loader;
  }
}
