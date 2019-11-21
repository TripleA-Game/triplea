package org.triplea.http.client.web.socket;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.TimeUnit;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.triplea.java.Interruptibles;
import org.triplea.java.Interruptibles.Result;
import org.triplea.java.timer.ScheduledTimer;
import org.triplea.java.timer.Timers;

/**
 * Component to manage a websocket connection. Responsible for:
 *
 * <ul>
 *   <li>initiating the connection (blocking)
 *   <li>sending message requests after the connection to server has been established (async)
 *   <li>triggering listener callbacks when messages are received from server
 *   <li>closing the websocket connection (async)
 * </ul>
 */
class WebSocketConnection {
  private static final int DEFAULT_CONNECT_TIMEOUT_MILLIS = 5000;
  private final Collection<WebSocketConnectionListener> listeners = new HashSet<>();

  private boolean closed = false;

  @Getter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  @Setter(
      value = AccessLevel.PACKAGE,
      onMethod_ = {@VisibleForTesting})
  private WebSocketClient client;

  private final ScheduledTimer pingSender;

  WebSocketConnection(final URI serverUri) {
    client =
        new WebSocketClient(serverUri) {
          @Override
          public void onOpen(final ServerHandshake serverHandshake) {}

          @Override
          public void onMessage(final String message) {
            listeners.forEach(listener -> listener.messageReceived(message));
          }

          @Override
          public void onClose(final int code, final String reason, final boolean remote) {
            pingSender.cancel();
            listeners.forEach(listener -> listener.connectionClosed(reason));
          }

          @Override
          public void onError(final Exception exception) {
            listeners.forEach(listener -> listener.handleError(exception));
          }
        };
    pingSender =
        Timers.fixedRateTimer("websocket-ping-sender")
            .period(45, TimeUnit.SECONDS)
            .delay(45, TimeUnit.SECONDS)
            .task(
                () -> {
                  if (client.isOpen()) {
                    client.sendPing();
                  }
                });
  }

  void addListener(final WebSocketConnectionListener listener) {
    Preconditions.checkState(!closed);
    listeners.add(listener);
  }

  /** Does an async close of the current websocket connection. */
  void close() {
    closed = true;
    client.close();
  }

  /**
   * Initiates a websocket connection. Must be called before {@link #sendMessage(String)}. This
   * method blocks until the connection has either successfully been established, or the connection
   * failed.
   *
   * @throws CouldNotConnect If the connection fails
   */
  void connect() {
    Preconditions.checkState(!client.isOpen());
    Preconditions.checkState(!closed);
    final Result<Boolean> connectionAttempt =
        Interruptibles.awaitResult(
            () -> client.connectBlocking(DEFAULT_CONNECT_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS));
    if (!connectionAttempt.completed || !connectionAttempt.result.orElse(false)) {
      throw new CouldNotConnect(client.getURI());
    }
    pingSender.start();
  }

  /**
   * Sends a message asynchronously.
   *
   * @throws IllegalStateException If the connection hasn't been opened yet. {@link #connect()}
   *     needs to be called first.
   */
  void sendMessage(final String message) {
    Preconditions.checkState(!closed);
    Preconditions.checkState(client.isOpen());
    client.send(message);
  }

  /** Exception indicating connection to server failed. */
  @VisibleForTesting
  static final class CouldNotConnect extends RuntimeException {
    private static final long serialVersionUID = -5403199291005160495L;

    private static final String ERROR_MESSAGE = "Error, could not connect to server at %s";

    CouldNotConnect(final URI uri) {
      super(String.format(ERROR_MESSAGE, uri));
    }
  }
}
