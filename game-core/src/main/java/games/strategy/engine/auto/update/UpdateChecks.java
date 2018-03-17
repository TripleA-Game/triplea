package games.strategy.engine.auto.update;

import static games.strategy.engine.framework.CliProperties.DO_NOT_CHECK_FOR_UPDATES;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_CLIENT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import games.strategy.engine.framework.map.download.MapDownloadController;

/**
 * Runs background update checks and would prompt user if anything needs to be updated.
 * This class and related ones will control the frequency of how often we prompt the user.
 */
public class UpdateChecks {

  public static void launch() {
    new Thread(UpdateChecks::checkForUpdates).start();
  }


  private static void checkForUpdates() {
    if (!shouldRun()) {
      return;
    }

    // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.trim().length() > 0) {
      return;
    }

    TutorialMapCheck.checkForTutorialMap();
    EngineVersionCheck.checkForLatestEngineVersionOut();

    if (UpdatedMapsCheck.shouldRunMapUpdateCheck()) {
      MapDownloadController.checkDownloadedMapsAreLatest();
    }
  }

  private static boolean shouldRun() {
    return !System.getProperty(TRIPLEA_SERVER, "false").equalsIgnoreCase("true")
        && !System.getProperty(TRIPLEA_CLIENT, "false").equalsIgnoreCase("true")
        && !System.getProperty(DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true");
  }
}
