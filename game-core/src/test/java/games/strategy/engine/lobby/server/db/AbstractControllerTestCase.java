package games.strategy.engine.lobby.server.db;

import games.strategy.engine.config.lobby.TestLobbyPropertyReaders;
import games.strategy.test.Integration;

/**
 * Superclass for fixtures that test a DAO implementation.
 */
@Integration
public abstract class AbstractControllerTestCase {
  protected final Database database = new Database(TestLobbyPropertyReaders.INTEGRATION_TEST);

  protected AbstractControllerTestCase() {}
}
