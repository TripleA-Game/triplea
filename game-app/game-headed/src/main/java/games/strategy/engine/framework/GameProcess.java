package games.strategy.engine.framework;

import static games.strategy.engine.framework.CliProperties.LOBBY_GAME_COMMENTS;
import static games.strategy.engine.framework.CliProperties.LOBBY_URI;
import static games.strategy.engine.framework.CliProperties.SERVER_PASSWORD;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_GAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_NAME;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_PORT;
import static games.strategy.engine.framework.CliProperties.TRIPLEA_SERVER;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.triplea.game.client.HeadedApplicationContext;

public class GameProcess {

  private static String getMainClassName() {
    return new HeadedApplicationContext().getMainClass().getName();
  }

  /** Spawns a new process to host a network game. */
  public static void hostGame(
      final int port,
      final String playerName,
      final String comments,
      final char[] password,
      final URI lobbyUri) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    commands.add("-D" + TRIPLEA_SERVER + "=true");
    commands.add("-D" + TRIPLEA_PORT + "=" + port);
    commands.add("-D" + TRIPLEA_NAME + "=" + playerName);
    commands.add("-D" + LOBBY_URI + "=" + lobbyUri);
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
    if (password != null && password.length > 0) {
      commands.add("-D" + SERVER_PASSWORD + "=" + String.valueOf(password));
    }
    final String fileName = System.getProperty(TRIPLEA_GAME, "");
    if (fileName.length() > 0) {
      commands.add("-D" + TRIPLEA_GAME + "=" + fileName);
    }
    commands.add(getMainClassName());
    ProcessRunnerUtil.exec(commands);
  }
}
