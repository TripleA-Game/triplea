package org.triplea.server.remote.actions;

import com.google.common.base.Preconditions;
import javax.websocket.CloseReason;
import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.triplea.http.client.remote.actions.RemoteActionsWebsocketListener;

// TODO: test-me
/**
 * This websocket is available for game hosts to 'listen' to remote action events. This might be a
 * player was banned and should be disconnected, or for moderator actions like requesting a server
 * to shutdown.
 */
@ServerEndpoint(RemoteActionsWebsocketListener.NOTIFICATIONS_WEBSOCKET_PATH)
@Slf4j
public class RemoteActionsWebSocket {
  public static final String ACTIONS_QUEUE_KEY = "remote.actions.event.queue";

  @OnOpen
  public void open(final Session session) {
    // TODO: Project#12 do filtering for banned IPs (check if filter can kick in first)
    getEventQueue(session).addSession(session);
  }

  private RemoteActionsEventQueue getEventQueue(final Session session) {
    return Preconditions.checkNotNull(
        (RemoteActionsEventQueue) session.getUserProperties().get(ACTIONS_QUEUE_KEY));
  }

  // TODO: do we need to define this method?
  @OnMessage
  public void message(final Session session, final String message) {}

  @OnClose
  public void close(final Session session, final CloseReason closeReason) {
    getEventQueue(session).removeSession(session);
  }

  /**
   * This error handler is called automatically when server processing encounters an uncaught
   * exception. We use it to notify the user that an error occurred.
   */
  @OnError
  public void handleError(final Session session, final Throwable throwable) {
    // TODO: Project#12 implement error notification
  }
}
