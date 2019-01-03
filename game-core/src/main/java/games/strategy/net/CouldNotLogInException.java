package games.strategy.net;

import java.io.IOException;

/** Thrown when a ClientMessenger could not log in to a ServerMessenger. */
public final class CouldNotLogInException extends IOException {
  private static final long serialVersionUID = -7266754722803615270L;

  public CouldNotLogInException() {}

  public CouldNotLogInException(final String message) {
    super(message);
  }
}
