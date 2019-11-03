package org.triplea.server.lobby.chat;

import java.util.function.Function;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.ChatParticipant;
import org.triplea.lobby.server.db.data.UserRole;
import org.triplea.lobby.server.db.data.UserWithRoleRecord;

public class ChatParticipantAdapter implements Function<UserWithRoleRecord, ChatParticipant> {
  @Override
  public ChatParticipant apply(final UserWithRoleRecord userWithRoleRecord) {
    return ChatParticipant.builder()
        .playerName(PlayerName.of(userWithRoleRecord.getUsername()))
        .isModerator(
            userWithRoleRecord.getRole().equals(UserRole.ADMIN)
                || userWithRoleRecord.getRole().equals(UserRole.MODERATOR))
        .build();
  }
}
