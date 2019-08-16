package games.strategy.engine.chat;

import games.strategy.engine.message.MessageContext;
import games.strategy.engine.message.RemoteName;
import games.strategy.net.IConnectionChangeListener;
import games.strategy.net.INode;
import games.strategy.net.Messengers;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Level;
import lombok.extern.java.Log;
import org.triplea.util.Tuple;

/** Default implementation of {@link IChatController}. */
@Log
public class ChatController implements IChatController {
  private static final String CHAT_REMOTE = "_ChatRmt";
  private static final String CHAT_CHANNEL = "_ChatCtrl";
  private final Messengers messengers;
  private final Predicate<INode> isModerator;
  private final String chatName;
  private final Map<INode, Tag> chatters = new HashMap<>();
  private final Object mutex = new Object();
  private final String chatChannel;
  private long version;
  private final ScheduledExecutorService pingThread = Executors.newScheduledThreadPool(1);
  private final IConnectionChangeListener connectionChangeListener =
      new IConnectionChangeListener() {
        @Override
        public void connectionAdded(final INode to) {}

        @Override
        public void connectionRemoved(final INode to) {
          synchronized (mutex) {
            if (chatters.containsKey(to)) {
              leaveChatInternal(to);
            }
          }
        }
      };

  public ChatController(
      final String name, final Messengers messengers, final Predicate<INode> isModerator) {
    chatName = name;
    this.messengers = messengers;
    this.isModerator = isModerator;
    chatChannel = getChatChannelName(name);
    messengers.registerRemote(this, getChatControllerRemoteName(name));
    messengers.addConnectionChangeListener(connectionChangeListener);
    startPinger();
  }

  public static RemoteName getChatControllerRemoteName(final String chatName) {
    return new RemoteName(CHAT_REMOTE + chatName, IChatController.class);
  }

  public static String getChatChannelName(final String chatName) {
    return CHAT_CHANNEL + chatName;
  }

  @SuppressWarnings("FutureReturnValueIgnored") // false positive; see
  // https://github.com/google/error-prone/issues/883
  private void startPinger() {
    pingThread.scheduleAtFixedRate(
        () -> {
          try {
            getChatBroadcaster().ping();
          } catch (final Exception e) {
            log.log(Level.SEVERE, "Error pinging", e);
          }
        },
        180,
        60,
        TimeUnit.SECONDS);
  }

  // clean up
  public void deactivate() {
    pingThread.shutdown();
    synchronized (mutex) {
      final IChatChannel chatter = getChatBroadcaster();
      for (final INode node : chatters.keySet()) {
        version++;
        chatter.speakerRemoved(node, version);
      }
      messengers.unregisterRemote(getChatControllerRemoteName(chatName));
    }
    messengers.removeConnectionChangeListener(connectionChangeListener);
  }

  private IChatChannel getChatBroadcaster() {
    return (IChatChannel)
        messengers.getChannelBroadcaster(new RemoteName(chatChannel, IChatChannel.class));
  }

  // a player has joined
  @Override
  public Tuple<Map<INode, Tag>, Long> joinChat() {
    final INode node = MessageContext.getSender();
    log.info("Chatter:" + node + " is joining chat:" + chatName);
    final Tag tag = isModerator.test(node) ? Tag.MODERATOR : Tag.NONE;
    synchronized (mutex) {
      chatters.put(node, tag);
      version++;
      getChatBroadcaster().speakerAdded(node, tag, version);
      final Map<INode, Tag> copy = new HashMap<>(chatters);
      return Tuple.of(copy, version);
    }
  }

  // a player has left
  @Override
  public void leaveChat() {
    leaveChatInternal(MessageContext.getSender());
  }

  private void leaveChatInternal(final INode node) {
    final long version;
    synchronized (mutex) {
      chatters.remove(node);
      this.version++;
      version = this.version;
    }
    getChatBroadcaster().speakerRemoved(node, version);
    log.info("Chatter:" + node + " has left chat:" + chatName);
  }
}
