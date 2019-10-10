package games.strategy.engine.chat;

import games.strategy.engine.message.IRemote;
import java.util.Collection;
import org.triplea.http.client.lobby.chat.ChatParticipant;

/**
 * A central controller of who is in the chat.
 *
 * <p>When joining you get a list of all the players currently in the chat and their statuses.
 */
public interface IChatController extends IRemote {
  /** Join the chat, returns the chatters currently in the chat. */
  Collection<ChatParticipant> joinChat();

  /** Leave the chat, and ask that everyone stops bothering me. */
  void leaveChat();

  void setStatus(String newStatus);

  /** A tag associated with a chat participant indicating the participant's role. */
  // TODO: rename to Role upon next lobby-incompatible release
  enum Tag {
    MODERATOR,
    NONE
  }
}
