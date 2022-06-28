package games.strategy.triplea.ai.pro.logging;

import games.strategy.engine.framework.GameShutdownRegistry;
import games.strategy.triplea.ui.menubar.DebugMenu;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugAction;
import games.strategy.triplea.ui.menubar.debug.AiPlayerDebugOption;
import games.strategy.ui.Util;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.SwingUtilities;

/** Class to manage log window display. */
public final class ProLogUi {
  private static boolean registered = false;
  private static ProLogWindow settingsWindow = null;
  private static String currentName = "";
  private static int currentRound = 0;

  private ProLogUi() {}

  public static void registerDebugMenu() {
    if (!registered) {
      DebugMenu.registerDebugOptions("Hard AI", ProLogUi.buildDebugOptions());
      DebugMenu.registerFrameVisitor(
          tripleAFrame -> {
            Util.ensureOnEventDispatchThread();
            if (settingsWindow == null) {
              settingsWindow = new ProLogWindow(tripleAFrame);
              GameShutdownRegistry.registerShutdownAction(ProLogUi::clearCachedInstances);
            }
          });
      registered = true;
    }
  }

  private static List<AiPlayerDebugOption> buildDebugOptions() {
    ProLogger.info("Initialized Hard AI");
    return List.of(
        AiPlayerDebugOption.builder()
            .title("Show Logs")
            .actionListener(ProLogUi::showSettingsWindow)
            .mnemonic(KeyEvent.VK_X)
            .build());
  }

  public static void clearCachedInstances() {
    if (settingsWindow != null) {
      settingsWindow.dispose();
    }
    registered = false;
    settingsWindow = null;
  }

  public static void showSettingsWindow(AiPlayerDebugAction aiPlayerDebugAction) {
    if (settingsWindow == null) {
      return;
    }
    ProLogger.info("Showing Hard AI settings window");
    settingsWindow.setVisible(true);
  }

  static void notifyAiLogMessage(final String message) {
    SwingUtilities.invokeLater(
        () -> {
          if (settingsWindow != null) {
            settingsWindow.addMessage(message);
          }
        });
  }

  public static void notifyStartOfRound(final int round, final String name) {
    if (settingsWindow == null) {
      return;
    }
    if (round != currentRound || !name.equals(currentName)) {
      currentRound = round;
      currentName = name;
      settingsWindow.notifyNewRound(round, name);
    }
  }
}
