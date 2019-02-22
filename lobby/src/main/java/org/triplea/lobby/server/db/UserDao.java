package org.triplea.lobby.server.db;

import games.strategy.engine.lobby.server.userDB.DBUser;

/**
 * Data access object for the users table.
 */
public interface UserDao {

  /**
   * Returns null if the user does not exist.
   */
  HashedPassword getPassword(String username);

  boolean doesUserExist(String username);

  void updateUser(DBUser user, HashedPassword password);

  void createUser(DBUser user, HashedPassword password);

  boolean login(String username, HashedPassword password);

  DBUser getUserByName(String username);
}
