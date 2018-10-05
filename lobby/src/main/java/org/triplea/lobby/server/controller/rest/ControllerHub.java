package org.triplea.lobby.server.controller.rest;

import static spark.Spark.path;

import org.triplea.lobby.server.config.LobbyConfiguration;
import org.triplea.lobby.server.db.Database;

public class ControllerHub {

  public static void initializeControllers(final LobbyConfiguration configuration) {
    final Database database = new Database(configuration);
    path("/api/v0", new ModeratorActionController(database)::initializeRoutes);
  }
}
