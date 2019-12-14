package org.triplea.http.client.remote.actions;

import java.net.InetAddress;
import java.util.function.Consumer;
import javax.annotation.Nonnull;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RemoteActionListeners {
  /** Note, the string received when this listener is triggered will be an empty string. */
  @Nonnull private final Consumer<String> shutdownListener;

  /** When this listener is triggered, it will receive the IP address of the banned player. */
  @Nonnull private final Consumer<InetAddress> bannedPlayerListener;
}
