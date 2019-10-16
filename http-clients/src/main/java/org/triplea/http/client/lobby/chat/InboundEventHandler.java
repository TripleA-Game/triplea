package org.triplea.http.client.lobby.chat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
import lombok.extern.java.Log;
import org.triplea.domain.data.PlayerName;
import org.triplea.http.client.lobby.chat.events.server.ChatMessage;
import org.triplea.http.client.lobby.chat.events.server.PlayerSlapped;
import org.triplea.http.client.lobby.chat.events.server.ServerEventEnvelope;
import org.triplea.http.client.lobby.chat.events.server.StatusUpdate;

/** Interprets and handles routing of inbound messages to the appropriate callback listener. */
@Log
class InboundEventHandler {

  private final Collection<Consumer<StatusUpdate>> playerStatusListeners = new ArrayList<>();
  private final Collection<Consumer<PlayerName>> playerLeftListeners = new ArrayList<>();
  private final Collection<Consumer<ChatParticipant>> playerJoinedListeners = new ArrayList<>();
  private final Collection<Consumer<PlayerSlapped>> playerSlappedListeners = new ArrayList<>();
  private final Collection<Consumer<ChatMessage>> messageListeners = new ArrayList<>();
  private final Collection<Consumer<Collection<ChatParticipant>>> connectedListeners =
      new ArrayList<>();

  void addPlayerStatusListener(final Consumer<StatusUpdate> playerStatusListener) {
    playerStatusListeners.add(playerStatusListener);
  }

  void addPlayerLeftListener(final Consumer<PlayerName> playerLeftListener) {
    playerLeftListeners.add(playerLeftListener);
  }

  void addPlayerJoinedListener(final Consumer<ChatParticipant> playerJoinedListener) {
    playerJoinedListeners.add(playerJoinedListener);
  }

  void addPlayerSlappedListener(final Consumer<PlayerSlapped> playerSlappedListener) {
    playerSlappedListeners.add(playerSlappedListener);
  }

  void addMessageListener(final Consumer<ChatMessage> messageListener) {
    messageListeners.add(messageListener);
  }

  void addConnectedListener(final Consumer<Collection<ChatParticipant>> connectedListener) {
    connectedListeners.add(connectedListener);
  }

  void handleServerMessage(final ServerEventEnvelope inboundMessage) {
    switch (inboundMessage.getMessageType()) {
      case PLAYER_LISTING:
        connectedListeners.forEach(
            connectedListener ->
                connectedListener.accept(inboundMessage.toPlayerListing().getChatters()));
        break;
      case STATUS_CHANGED:
        playerStatusListeners.forEach(
            statusListener -> statusListener.accept(inboundMessage.toPlayerStatusChange()));
        break;
      case PLAYER_LEFT:
        playerLeftListeners.forEach(
            playerLeftListener ->
                playerLeftListener.accept(inboundMessage.toPlayerLeft().getPlayerName()));
        break;
      case PLAYER_JOINED:
        playerJoinedListeners.forEach(
            playerJoinedListener ->
                playerJoinedListener.accept(inboundMessage.toPlayerJoined().getChatParticipant()));
        break;
      case PLAYER_SLAPPED:
        playerSlappedListeners.forEach(
            playerSlappedListener ->
                playerSlappedListener.accept(inboundMessage.toPlayerSlapped()));
        break;
      case CHAT_MESSAGE:
        messageListeners.forEach(
            chatMessageListener -> chatMessageListener.accept(inboundMessage.toChatMessage()));
        break;
      case SERVER_ERROR:
        log.severe(inboundMessage.toErrorMessage());
        break;
      default:
        log.info("Unrecognized server message type: " + inboundMessage.getMessageType());
    }
  }
}
