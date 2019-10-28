package org.triplea.server.user.account.login.authorizer.legacy;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.domain.data.PlayerName;
import org.triplea.lobby.server.db.dao.UserJdbiDao;

@ExtendWith(MockitoExtension.class)
class LegacyPasswordCheckTest {

  private static final String DB_LEGACY_PASSWORD = "$1$GTV9OtVd$WKt1JeqIasr4GlAJylq2A/";
  private static final String PLAINTEXT_PASSWORD = "legacy";

  private static final PlayerName PLAYER_NAME = PlayerName.of("user");

  @Mock private UserJdbiDao userJdbiDao;

  private LegacyPasswordCheck legacyPasswordCheck;

  @BeforeEach
  void setup() {
    legacyPasswordCheck = LegacyPasswordCheck.builder().userJdbiDao(userJdbiDao).build();
  }

  @Test
  void noLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue())).thenReturn(Optional.empty());

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, PLAINTEXT_PASSWORD);

    assertThat(result, is(false));
  }

  @Test
  void badLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue()))
        .thenReturn(Optional.of(DB_LEGACY_PASSWORD));

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, "incorrect");

    assertThat(result, is(false));
  }

  @Test
  void validLegacyPassword() {
    when(userJdbiDao.getLegacyPassword(PLAYER_NAME.getValue()))
        .thenReturn(Optional.of(DB_LEGACY_PASSWORD));

    final boolean result = legacyPasswordCheck.test(PLAYER_NAME, PLAINTEXT_PASSWORD);

    assertThat(result, is(true));
  }
}
