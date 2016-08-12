package games.strategy.engine.framework.ui.background;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.swing.SwingUtilities;

public class BackgroundTaskRunner {
  public static void runInBackground(final String waitMessage, final Runnable r) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> runInBackground(waitMessage, r));
    }
    final WaitDialog window = new WaitDialog(null, waitMessage);
    final AtomicBoolean doneWait = new AtomicBoolean(false);
    final Thread t = new Thread(() -> {
      try {
        r.run();
      } finally {
        SwingUtilities.invokeLater(() -> {
          doneWait.set(true);
          window.setVisible(false);
          window.dispose();
        });
      }
    });
    t.start();
    if (!doneWait.get()) {
      window.pack();
      window.setVisible(true);
    }
  }
}
