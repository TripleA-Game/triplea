package org.triplea.server.lobby.chat.moderation;

import javax.annotation.Nonnull;
import lombok.Builder;
import org.triplea.domain.data.PlayerChatId;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.IpAddressParser;
import org.triplea.http.client.lobby.chat.messages.server.ChatServerEnvelopeFactory;
import org.triplea.http.client.lobby.moderator.BanDurationFormatter;
import org.triplea.http.client.lobby.moderator.BanPlayerRequest;
import org.triplea.lobby.server.db.dao.api.key.LobbyApiKeyDaoWrapper;
import org.triplea.lobby.server.db.dao.api.key.PlayerIdLookup;
import org.triplea.server.http.web.socket.MessageBroadcaster;
import org.triplea.server.lobby.chat.event.processing.Chatters;
import org.triplea.server.remote.actions.RemoteActionsEventQueue;

@Builder
class ModeratorChatService {

  @Nonnull private final LobbyApiKeyDaoWrapper lobbyApiKeyDaoWrapper;
  @Nonnull private final ModeratorActionPersistence moderatorActionPersistence;
  @Nonnull private final Chatters chatters;
  @Nonnull private final MessageBroadcaster messageBroadcaster;
  @Nonnull private final RemoteActionsEventQueue remoteActionsEventQueue;

  /**
   * Bans a player for a given duration. Banned players are disconnected from chat, players in chat
   * are notified of the ban, and an audit log entry is recorded. The banned player is notified of
   * the ban through the chatter websocket disconnect message.
   */
  void banPlayer(final int moderatorUserId, final BanPlayerRequest banPlayerRequest) {
    final PlayerIdLookup playerIdLookup =
        lookupUpPlayer(PlayerChatId.of(banPlayerRequest.getPlayerChatId()));

    lookupUpPlayer(PlayerChatId.of(banPlayerRequest.getPlayerChatId()));

    chatters.disconnectPlayerSessions(
        playerIdLookup.getPlayerName(), playerBannedMessage(banPlayerRequest));
    messageBroadcaster.accept(
        chatters.fetchOpenSessions(),
        ChatServerEnvelopeFactory.newEventMessage(
            playerBannedNotification(playerIdLookup.getPlayerName(), banPlayerRequest)));

    remoteActionsEventQueue.addPlayerBannedEvent(
        IpAddressParser.fromString(playerIdLookup.getIp()));
    moderatorActionPersistence.recordBan(moderatorUserId, playerIdLookup, banPlayerRequest);
  }

  private PlayerIdLookup lookupUpPlayer(final PlayerChatId playerChatId) {
    return lobbyApiKeyDaoWrapper
        .lookupPlayerByChatId(playerChatId)
        .orElseThrow(
            () -> new IllegalArgumentException("Unable to find playerChatId: " + playerChatId));
  }

  private static String playerBannedMessage(final BanPlayerRequest banPlayerRequest) {
    return String.format(
        "You have been banned for %s for violating lobby rules",
        BanDurationFormatter.formatBanMinutes(banPlayerRequest.getBanMinutes()));
  }

  private static String playerBannedNotification(
      final PlayerName bannedPlayerName, final BanPlayerRequest banPlayerRequest) {
    return String.format(
        "%s violated lobby rules and was banned for %s",
        bannedPlayerName, BanDurationFormatter.formatBanMinutes(banPlayerRequest.getBanMinutes()));
  }

  /**
   * Does a simple disconnect of a given player from chat, records an audit log entry, and notifies
   * chatters of the disconnect.
   */
  void disconnectPlayer(final int moderatorId, final PlayerChatId playerChatId) {
    final PlayerIdLookup playerIdLookup = lookupUpPlayer(playerChatId);
    chatters.disconnectPlayerSessions(playerIdLookup.getPlayerName(), "Disconnected by moderator");
    messageBroadcaster.accept(
        chatters.fetchOpenSessions(),
        ChatServerEnvelopeFactory.newEventMessage(
            playerIdLookup.getPlayerName() + " was disconnected by moderator"));
    moderatorActionPersistence.recordPlayerDisconnect(moderatorId, playerIdLookup);
  }
}
