package org.triplea.lobby.server.db;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.sql.Timestamp;
import java.time.Instant;

import javax.annotation.Nullable;

import org.junit.jupiter.api.Test;
import org.triplea.lobby.server.User;
import org.triplea.lobby.server.config.TestLobbyConfigurations;
import org.triplea.util.Tuple;

public final class BannedUsernameControllerIntegrationTest extends AbstractModeratorServiceControllerTestCase {
  private final BannedUsernameDao controller =
      TestLobbyConfigurations.INTEGRATION_TEST.getDatabaseDao().getBannedUsernameDao();

  @Test
  void testBanUsernameForever() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = isUsernameBanned();
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  void testBanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    assertBannedUserEquals(user);
    final Tuple<Boolean, Timestamp> usernameDetails = isUsernameBanned();
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    when(controller.now()).thenReturn(banUntil.plusSeconds(1L));
    final Tuple<Boolean, Timestamp> usernameDetails2 = isUsernameBanned();
    assertFalse(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  void testUnbanUsername() {
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails = isUsernameBanned();
    assertTrue(usernameDetails.getFirst());
    assertEquals(banUntil, usernameDetails.getSecond().toInstant());
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = isUsernameBanned();
    assertFalse(usernameDetails2.getFirst());
    assertNull(usernameDetails2.getSecond());
  }

  @Test
  void testBanUsernameInThePast() {
    banUsernameForSeconds(-10L);
    final Tuple<Boolean, Timestamp> usernameDetails = isUsernameBanned();
    assertFalse(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
  }

  @Test
  void testBanUsernameUpdate() {
    banUsernameForSeconds(Long.MAX_VALUE);
    final Tuple<Boolean, Timestamp> usernameDetails = isUsernameBanned();
    assertTrue(usernameDetails.getFirst());
    assertNull(usernameDetails.getSecond());
    final Instant banUntil = banUsernameForSeconds(100L);
    final Tuple<Boolean, Timestamp> usernameDetails2 = isUsernameBanned();
    assertTrue(usernameDetails2.getFirst());
    assertEquals(banUntil, usernameDetails2.getSecond().toInstant());
  }

  @Test
  void testBanUsernameUpdatesBannedUserAndModerator() {
    banUsernameForSeconds(user, Long.MAX_VALUE, moderator);

    final User otherUser = newUser().withUsername(user.getUsername());
    final User otherModerator = newUser();
    banUsernameForSeconds(otherUser, Long.MAX_VALUE, otherModerator);

    assertBannedUserEquals(otherUser);
    assertModeratorEquals(otherModerator);
  }

  private @Nullable Instant banUsernameForSeconds(final long seconds) {
    return banUsernameForSeconds(user, seconds, moderator);
  }

  private @Nullable Instant banUsernameForSeconds(final User bannedUser, final long seconds, final User moderator) {
    final @Nullable Instant banEnd = (seconds == Long.MAX_VALUE) ? null : Instant.now().plusSeconds(seconds);
    controller.addBannedUsername(bannedUser, banEnd, moderator);
    return banEnd;
  }

  private Tuple<Boolean, /* @Nullable */ Timestamp> isUsernameBanned() {
    return controller.isUsernameBanned(user.getUsername());
  }

  private void assertBannedUserEquals(final User expected) {
    assertUserEquals(
        expected,
        "select username, ip, mac from banned_usernames where username=?",
        ps -> ps.setString(1, user.getUsername()),
        "unknown banned username: " + user.getUsername());
  }

  private void assertModeratorEquals(final User expected) {
    assertUserEquals(
        expected,
        "select mod_username, mod_ip, mod_mac from banned_usernames where username=?",
        ps -> ps.setString(1, user.getUsername()),
        "unknown banned username: " + user.getUsername());
  }
}
