package org.triplea.lobby.server.db.data;

import javax.annotation.Nullable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.jdbi.v3.core.mapper.RowMapper;
import org.triplea.java.Postconditions;

/** Maps ResultSet data when querying for a users API key. */
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Getter
@EqualsAndHashCode
@ToString
public class ApiKeyUserData {

  public static final String ROLE_COLUMN = "role";
  public static final String USERNAME_COLUMN = "username";
  public static final String USER_ID_COLUMN = "id";

  private @Nullable Integer userId;
  private @Nullable String username;
  private String role;

  /** Returns a JDBI row mapper used to convert a ResultSet into an instance of this bean object. */
  public static RowMapper<ApiKeyUserData> buildResultMapper() {
    return (rs, ctx) -> {
      final ApiKeyUserData userData =
          ApiKeyUserData.builder()
              .userId(rs.getInt(USER_ID_COLUMN) == 0 ? null : rs.getInt(USER_ID_COLUMN))
              .role(rs.getString(ROLE_COLUMN))
              .username(rs.getString(USERNAME_COLUMN))
              .build();

      Postconditions.assertState(userData.role != null);

      if (userData.role.equals(UserRole.HOST)) {
        Postconditions.assertState(userData.username == null);
        Postconditions.assertState(userData.userId == null);
      } else if (userData.role.equals(UserRole.ANONYMOUS)) {
        Postconditions.assertState(userData.userId == null);
        Postconditions.assertState(userData.username != null);
      } else {
        Postconditions.assertState(userData.userId != null);
        Postconditions.assertState(userData.username != null);
      }
      return userData;
    };
  }
}
