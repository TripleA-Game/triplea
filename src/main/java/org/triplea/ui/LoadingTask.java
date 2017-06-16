package org.triplea.ui;

import static com.google.common.base.Preconditions.checkNotNull;
import java.util.function.Function;

import javafx.application.Platform;

public class LoadingTask<T> {

  private Function<LoadingTask<T>, T> function;
  private TripleA triplea;

  public LoadingTask(TripleA triplea, Function<LoadingTask<T>, T> function) {
    this.triplea = checkNotNull(triplea);
    this.function = checkNotNull(function);
  }

  public T run() {
    if (Platform.isFxApplicationThread()) {
      throw new IllegalStateException("This method must not be called on the FX Application Thread!");
    }
    try {
      Platform.runLater(() -> triplea.displayLoadingScreen(true));
      return function.apply(this);
    } finally {
      Platform.runLater(() -> {
        triplea.displayLoadingScreen(false);
        triplea.setLoadingMessage("");
      });
    }
  }

  public void setLoadingMesage(String message) {
    Platform.runLater(() -> triplea.setLoadingMessage(message));
  }
}
