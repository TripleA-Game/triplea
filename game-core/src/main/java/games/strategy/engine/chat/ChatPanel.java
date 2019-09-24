package games.strategy.engine.chat;

import games.strategy.engine.chat.Chat.ChatSoundProfile;
import games.strategy.net.Messengers;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Optional;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import org.triplea.game.chat.ChatModel;
import org.triplea.java.Interruptibles;
import org.triplea.swing.SwingAction;

/**
 * A Chat window.
 *
 * <p>Multiple chat panels can be connected to the same Chat.
 *
 * <p>We can change the chat we are connected to using the setChat(...) method.
 */
public class ChatPanel extends JPanel implements ChatModel {
  private static final long serialVersionUID = -6177517517279779486L;
  private static final int DIVIDER_SIZE = 5;
  private ChatPlayerPanel chatPlayerPanel;
  private ChatMessagePanel chatMessagePanel;

  public ChatPanel(final Chat chat) {
    setSize(300, 200);
    chatPlayerPanel = new ChatPlayerPanel(chat);
    chatMessagePanel = new ChatMessagePanel(chat);
    final Container content = this;
    content.setLayout(new BorderLayout());
    final JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
    split.setLeftComponent(chatMessagePanel);
    split.setRightComponent(chatPlayerPanel);
    split.setOneTouchExpandable(false);
    split.setDividerSize(DIVIDER_SIZE);
    split.setResizeWeight(1);
    content.add(split, BorderLayout.CENTER);
  }

  /**
   * Creates a Chat object instance on the current thread based on the provided arguments and calls
   * the ChatPanel constructor with it on the EDT. This is to allow for easy off-EDT initialisation
   * of this ChatPanel. Note that if this method is being called on the EDT It will still work, but
   * the UI might freeze for a long time.
   */
  public static ChatPanel newChatPanel(
      final Messengers messengers, final String chatName, final ChatSoundProfile chatSoundProfile) {
    final Chat chat = new Chat(messengers, chatName, chatSoundProfile);
    return Interruptibles.awaitResult(
            () -> SwingAction.invokeAndWaitResult(() -> new ChatPanel(chat)))
        .result
        .orElseThrow(() -> new IllegalStateException("Error during Chat Panel creation"));
  }

  @Override
  public String getAllText() {
    return chatMessagePanel.getAllText();
  }

  @Override
  public void setChat(final Chat chat) {
    chatMessagePanel.setChat(chat);
    chatPlayerPanel.setChat(chat);
  }

  @Override
  public Chat getChat() {
    return chatMessagePanel.getChat();
  }

  public void setPlayerRenderer(final DefaultListCellRenderer renderer) {
    chatPlayerPanel.setPlayerRenderer(renderer);
    // gets remaining width from parent component, so setting the width is not really necessary
    chatMessagePanel.setPreferredSize(
        new Dimension(30, chatMessagePanel.getPreferredSize().height));
  }

  public ChatMessagePanel getChatMessagePanel() {
    return chatMessagePanel;
  }

  @Override
  public Optional<Component> getViewComponent() {
    return Optional.of(this);
  }
}
