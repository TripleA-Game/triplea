package games.strategy.engine.chat;

import com.google.common.base.Ascii;
import games.strategy.engine.chat.Chat.ChatSoundProfile;
import games.strategy.engine.lobby.PlayerName;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import games.strategy.sound.ClipPlayer;
import games.strategy.sound.SoundPath;
import java.awt.Component;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.TimeManager;

/** Headless version of ChatPanel. */
public class HeadlessChat implements IChatListener, ChatModel {
  // roughly 1000 chat messages
  private static final int MAX_LENGTH = 1000 * 200;
  private Chat chat;
  private boolean showTime = true;
  private StringBuilder allText = new StringBuilder();
  private final ChatFloodControl floodControl = new ChatFloodControl();

  public HeadlessChat(
      final Messengers messengers, final String chatName, final ChatSoundProfile chatSoundProfile) {
    final Chat chat = new Chat(messengers, chatName, chatSoundProfile);
    setChat(chat);
  }

  @Override
  public String toString() {
    return allText.toString();
  }

  @Override
  public String getAllText() {
    return allText.toString();
  }

  @Override
  public Chat getChat() {
    return chat;
  }

  @Override
  public void setShowChatTime(final boolean showTime) {
    this.showTime = showTime;
  }

  @Override
  public void updatePlayerList(final Collection<INode> players) {}

  @Override
  public void setChat(final Chat chat) {
    if (this.chat != null) {
      this.chat.removeChatListener(this);
    }
    this.chat = chat;
    if (this.chat != null) {
      this.chat.addChatListener(this);
      synchronized (this.chat.getMutex()) {
        allText = new StringBuilder();
        for (final ChatMessage message : this.chat.getChatHistory()) {
          addChatMessage(message.getMessage(), message.getFrom().getValue());
        }
      }
    } else {
      updatePlayerList(Collections.emptyList());
    }
  }

  /** thread safe. */
  @Override
  public void addMessage(final String message, final PlayerName from) {
    addMessageWithSound(message, from, SoundPath.CLIP_CHAT_MESSAGE);
  }

  /** thread safe. */
  @Override
  public void addMessageWithSound(final String message, final PlayerName from, final String sound) {
    // TODO: I don't really think we need a new thread for this...
    new Thread(
            () -> {
              if (!floodControl.allow(from, System.currentTimeMillis())) {
                if (from.equals(chat.getLocalNode().getPlayerName())) {
                  addChatMessage("MESSAGE LIMIT EXCEEDED, TRY AGAIN LATER", "ADMIN_FLOOD_CONTROL");
                }
                return;
              }
              addChatMessage(message, from.getValue());
              ClipPlayer.play(sound);
            })
        .start();
  }

  private void addChatMessage(final String originalMessage, final String from) {
    final String message = Ascii.truncate(originalMessage, 200, "...");
    final String time = "(" + TimeManager.getLocalizedTime() + ")";
    final String prefix = showTime ? time + " " + from + ": " : from + ": ";
    final String fullMessage = prefix + " " + message + "\n";
    final String currentAllText = allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      allText = new StringBuilder(currentAllText.substring(MAX_LENGTH / 2));
    }
    allText.append(fullMessage);
  }

  @Override
  public void addStatusMessage(final String message) {
    final String fullMessage = "--- " + message + " ---\n";
    final String currentAllText = allText.toString();
    if (currentAllText.length() > MAX_LENGTH) {
      allText = new StringBuilder(currentAllText.substring(MAX_LENGTH / 2));
    }
    allText.append(fullMessage);
  }

  @Override
  public Optional<Component> getViewComponent() {
    return Optional.empty();
  }
}
