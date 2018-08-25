package games.strategy.net;

import static com.google.common.base.Preconditions.checkNotNull;

import java.time.Instant;
import java.util.Set;

import javax.annotation.Nullable;

/**
 * A server messenger. Additional methods for accepting new connections.
 */
public interface IServerMessenger extends IMessenger {
  void setAcceptNewConnections(boolean accept);

  boolean isAcceptNewConnections();

  void setLoginValidator(ILoginValidator loginValidator);

  ILoginValidator getLoginValidator();

  /**
   * Add a listener for change in connection status.
   */
  void addConnectionChangeListener(IConnectionChangeListener listener);

  /**
   * Remove a listener for change in connection status.
   */
  void removeConnectionChangeListener(IConnectionChangeListener listener);

  /**
   * Remove the node from the network.
   */
  void removeConnection(INode node);

  /**
   * Get a list of nodes.
   */
  Set<INode> getNodes();

  void notifyIpMiniBanningOfPlayer(String ip, Instant expires);

  void notifyMacMiniBanningOfPlayer(String mac, Instant expires);

  void notifyUsernameMiniBanningOfPlayer(String username, Instant expires);

  /**
   * Returns the hashed MAC address for the user with the specified name or {@code null} if unknown.
   */
  @Nullable
  String getPlayerMac(String name);

  void notifyUsernameMutingOfPlayer(String username, Instant muteExpires);

  void notifyMacMutingOfPlayer(String mac, Instant muteExpires);

  boolean isUsernameMiniBanned(String username);

  boolean isIpMiniBanned(String ip);

  boolean isMacMiniBanned(String mac);

  /**
   * Returns the real username for the specified (possibly unique) username.
   *
   * <p>
   * Node usernames may contain a " (n)" suffix when the same user is logged in multiple times. This method removes such
   * a suffix yielding the original (real) username.
   * </p>
   */
  static String getRealName(final String name) {
    checkNotNull(name);

    final int spaceIndex = name.indexOf(' ');
    return (spaceIndex != -1) ? name.substring(0, spaceIndex) : name;
  }
}
