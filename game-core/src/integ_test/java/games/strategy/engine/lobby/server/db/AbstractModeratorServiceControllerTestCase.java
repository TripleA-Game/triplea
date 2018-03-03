package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import games.strategy.engine.lobby.server.TestUserUtils;
import games.strategy.engine.lobby.server.User;
import games.strategy.util.function.ThrowingConsumer;

/**
 * Superclass for fixtures that test a moderator service controller.
 */
public abstract class AbstractModeratorServiceControllerTestCase {
  protected final User user = newUser();
  protected final User moderator = newUser();

  protected AbstractModeratorServiceControllerTestCase() {}

  /**
   * Creates a new unique user.
   */
  protected static User newUser() {
    return TestUserUtils.newUser();
  }

  /**
   * Asserts the user returned from the specified query is equal to the expected user.
   *
   * @param expected The expected user.
   * @param userQuerySql The SQL used to query for the user. It is expected that this query returns the user's name
   *        in the first column, the user's IP address in the second column, and the user's hashed MAC address in the
   *        third column.
   * @param preparedStatementInitializer Callback to initialize the parameters in the prepared statement used to query
   *        for the user.
   * @param unknownUserMessage The failure message to be used when the requested user does not exist.
   */
  protected static void assertUserEquals(
      final User expected,
      final String userQuerySql,
      final ThrowingConsumer<PreparedStatement, SQLException> preparedStatementInitializer,
      final String unknownUserMessage) {
    try (Connection conn = Database.getPostgresConnection();
        PreparedStatement ps = conn.prepareStatement(userQuerySql)) {
      preparedStatementInitializer.accept(ps);
      try (ResultSet rs = ps.executeQuery()) {
        if (rs.next()) {
          assertEquals(expected.getUsername(), rs.getString(1));
          assertEquals(expected.getInetAddress(), InetAddress.getByName(rs.getString(2)));
          assertEquals(expected.getHashedMacAddress(), rs.getString(3));
        } else {
          fail(unknownUserMessage);
        }
      }
    } catch (final UnknownHostException e) {
      fail("malformed user IP address", e);
    } catch (final SQLException e) {
      fail("user query failed", e);
    }
  }
}
