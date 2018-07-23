package games.strategy.engine.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Connection;
import java.sql.ResultSet;
import java.util.UUID;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.mindrot.jbcrypt.BCrypt;

import games.strategy.engine.lobby.server.login.RsaAuthenticator;
import games.strategy.engine.lobby.server.userDB.DBUser;
import games.strategy.test.Integration;
import games.strategy.util.Md5Crypt;
import games.strategy.util.Util;

@Integration
public final class UserControllerIntegrationTest extends AbstractControllerTestCase {
  private final UserController controller = new UserController(database);

  @Test
  public void testCreate() throws Exception {
    final int startCount = getUserCount();

    createUserWithMd5CryptHash();
    assertEquals(getUserCount(), startCount + 1);

    createUserWithBCryptHash();
    assertEquals(getUserCount(), startCount + 2);
  }

  @Test
  public void testGet() {
    final DBUser user = createUserWithMd5CryptHash();
    assertEquals(user, controller.getUserByName(user.getName()));
  }

  @Test
  public void testDoesUserExist() {
    assertTrue(controller.doesUserExist(createUserWithMd5CryptHash().getName()));
    assertTrue(controller.doesUserExist(createUserWithBCryptHash().getName()));
  }

  @Test
  public void testCreateDupe() {
    assertThrows(Exception.class,
        () -> controller.createUser(createUserWithMd5CryptHash(),
            new HashedPassword(md5Crypt(Util.createUniqueTimeStamp()))),
        "Should not be allowed to create a dupe user");
  }

  @Test
  public void testLogin() {
    final String password = md5Crypt(Util.createUniqueTimeStamp());
    final DBUser user = createUserWithHash(password, Function.identity());
    controller.updateUser(user, new HashedPassword(bcrypt(obfuscate(password))));
    assertTrue(controller.login(user.getName(), new HashedPassword(password)));
    assertTrue(controller.login(user.getName(), new HashedPassword(obfuscate(password))));
  }

  @Test
  public void testUpdate() throws Exception {
    final DBUser user = createUserWithMd5CryptHash();
    assertTrue(controller.doesUserExist(user.getName()));
    final String password2 = md5Crypt("foo");
    final String email2 = "foo@foo.foo";
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(bcrypt(obfuscate(Util.createUniqueTimeStamp()))));
    controller.updateUser(
        new DBUser(new DBUser.UserName(user.getName()), new DBUser.UserEmail(email2)),
        new HashedPassword(password2));
    try (Connection con = database.newConnection()) {
      final String sql = " select * from ta_users where username = '" + user.getName() + "'";
      final ResultSet rs = con.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      assertEquals(email2, rs.getString("email"));
      assertEquals(password2, rs.getString("password"));
      assertNull(rs.getString("bcrypt_password"));
    }
  }

  private DBUser createUserWithMd5CryptHash() {
    return createUserWithHash(Util.createUniqueTimeStamp(), UserControllerIntegrationTest::md5Crypt);
  }

  private DBUser createUserWithBCryptHash() {
    return createUserWithHash(Util.createUniqueTimeStamp(), UserControllerIntegrationTest::bcrypt);
  }

  private DBUser createUserWithHash(final @Nullable String password, final Function<String, String> hashingMethod) {
    final String name = UUID.randomUUID().toString().substring(0, 20);
    final DBUser user = new DBUser(
        new DBUser.UserName(name),
        new DBUser.UserEmail(name + "@none.none"));
    controller.createUser(user, new HashedPassword(hashingMethod.apply(password)));
    return user;
  }

  private int getUserCount() throws Exception {
    try (Connection dbConnection = database.newConnection()) {
      final String sql = "select count(*) from TA_USERS";
      final ResultSet rs = dbConnection.createStatement().executeQuery(sql);
      assertTrue(rs.next());
      return rs.getInt(1);
    }
  }

  private static String bcrypt(final String string) {
    return BCrypt.hashpw(string, BCrypt.gensalt());
  }

  private static String md5Crypt(final String value) {
    return Md5Crypt.hashPassword(value, Md5Crypt.newSalt());
  }

  private static String obfuscate(final String string) {
    return RsaAuthenticator.hashPasswordWithSalt(string);
  }
}
