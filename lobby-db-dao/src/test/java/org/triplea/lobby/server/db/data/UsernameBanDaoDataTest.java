package org.triplea.lobby.server.db.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Calendar;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class UsernameBanDaoDataTest {
  private static final Instant NOW = Instant.now();
  private static final Timestamp timestamp = Timestamp.from(NOW);
  private static final String USERNAME = "Why does the furner laugh?";

  @Mock private ResultSet resultSet;

  @Test
  void buildResultMapper() throws Exception {
    when(resultSet.getTimestamp(eq(UsernameBanDaoData.DATE_CREATED_COLUMN), any(Calendar.class)))
        .thenReturn(timestamp);
    when(resultSet.getString(UsernameBanDaoData.USERNAME_COLUMN)).thenReturn(USERNAME);

    final UsernameBanDaoData result = UsernameBanDaoData.buildResultMapper().map(resultSet, null);

    assertThat(result.getUsername(), is(USERNAME));
    assertThat(result.getDateCreated(), is(NOW));
  }
}
