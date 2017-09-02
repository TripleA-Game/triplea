package games.strategy.engine.framework;

import java.awt.AWTEvent;
import java.awt.Container;
import java.awt.EventQueue;
import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;

import org.triplea.client.ui.javafx.TripleA;

import games.strategy.debug.ClientLogger;
import games.strategy.debug.ErrorConsole;
import games.strategy.debug.LoggingConfiguration;
import games.strategy.engine.ClientContext;
import games.strategy.engine.ClientFileSystemHelper;
import games.strategy.engine.chat.Chat;
import games.strategy.engine.framework.lookandfeel.LookAndFeel;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.map.download.MapDownloadController;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.mc.SetupPanelModel;
import games.strategy.engine.framework.startup.ui.ClientSetupPanel;
import games.strategy.engine.framework.startup.ui.ISetupPanel;
import games.strategy.engine.framework.startup.ui.MainPanel;
import games.strategy.engine.framework.startup.ui.ServerSetupPanel;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.engine.framework.systemcheck.LocalSystemChecker;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.net.Messengers;
import games.strategy.triplea.ai.proAI.ProAI;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.ui.ProgressWindow;
import games.strategy.ui.SwingAction;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CountDownLatchHandler;
import games.strategy.util.EventThreadJOptionPane;
import games.strategy.util.ThreadUtil;
import games.strategy.util.Version;

/**
 * GameRunner - The entrance class with the main method.
 * In this class commonly used constants are getting defined and the Game is being launched
 */
public class GameRunner {

  public static final String TRIPLEA_HEADLESS = "triplea.headless";
  public static final String TRIPLEA_GAME_HOST_CONSOLE_PROPERTY = "triplea.game.host.console";
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM = 21600;
  public static final int LOBBY_RECONNECTION_REFRESH_SECONDS_DEFAULT = 2 * LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM;
  public static final String NO_REMOTE_REQUESTS_ALLOWED = "noRemoteRequestsAllowed";

  // not arguments:
  public static final int PORT = 3300;
  // do not include this in the getProperties list. they are only for loading an old savegame.
  public static final String OLD_EXTENSION = ".old";
  // argument options below:
  public static final String TRIPLEA_GAME_PROPERTY = "triplea.game";
  public static final String TRIPLEA_MAP_DOWNLOAD_PROPERTY = "triplea.map.download";
  public static final String TRIPLEA_SERVER_PROPERTY = "triplea.server";
  public static final String TRIPLEA_CLIENT_PROPERTY = "triplea.client";
  public static final String TRIPLEA_HOST_PROPERTY = "triplea.host";
  public static final String TRIPLEA_PORT_PROPERTY = "triplea.port";
  public static final String TRIPLEA_NAME_PROPERTY = "triplea.name";
  public static final String TRIPLEA_SERVER_PASSWORD_PROPERTY = "triplea.server.password";
  public static final String TRIPLEA_STARTED = "triplea.started";
  public static final String LOBBY_HOST = "triplea.lobby.host";
  public static final String LOBBY_GAME_COMMENTS = "triplea.lobby.game.comments";
  public static final String LOBBY_GAME_HOSTED_BY = "triplea.lobby.game.hostedBy";
  public static final String LOBBY_GAME_SUPPORT_EMAIL = "triplea.lobby.game.supportEmail";
  public static final String LOBBY_GAME_SUPPORT_PASSWORD = "triplea.lobby.game.supportPassword";
  public static final String LOBBY_GAME_RECONNECTION = "triplea.lobby.game.reconnection";
  public static final String TRIPLEA_ENGINE_VERSION_BIN = "triplea.engine.version.bin";
  private static final String TRIPLEA_DO_NOT_CHECK_FOR_UPDATES = "triplea.doNotCheckForUpdates";
  public static final String TRIPLEA_LOBBY_PORT_PROPERTY = "triplea.lobby.port";

  public static final String TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME = "triplea.server.startGameSyncWaitTime";
  public static final String TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME = "triplea.server.observerJoinWaitTime";
  public static final int MINIMUM_CLIENT_GAMEDATA_LOAD_GRACE_TIME = 20;

  public static final String MAP_FOLDER = "mapFolder";


  private static final GameSelectorModel gameSelectorModel = new GameSelectorModel();
  private static final SetupPanelModel setupPanelModel = new SetupPanelModel(gameSelectorModel);
  private static JFrame mainFrame;

  private static final String[] COMMAND_LINE_ARGS =
      {TRIPLEA_GAME_PROPERTY, TRIPLEA_MAP_DOWNLOAD_PROPERTY, TRIPLEA_SERVER_PROPERTY, TRIPLEA_CLIENT_PROPERTY,
          TRIPLEA_HOST_PROPERTY, TRIPLEA_PORT_PROPERTY, TRIPLEA_NAME_PROPERTY, TRIPLEA_SERVER_PASSWORD_PROPERTY,
          TRIPLEA_STARTED, TRIPLEA_LOBBY_PORT_PROPERTY, LOBBY_HOST, LOBBY_GAME_COMMENTS, LOBBY_GAME_HOSTED_BY,
          TRIPLEA_ENGINE_VERSION_BIN, TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, MAP_FOLDER};


  /**
   * Launches the "main" TripleA gui enabled game client.
   * No args will launch a client, additional args can be supplied to specify additional behavior.
   * Warning: game engine code invokes this method to spawn new game clients.
   */
  public static void main(final String[] args) {
    LoggingConfiguration.initialize();

    if (!ClientContext.gameEnginePropertyReader().useJavaFxUi()) {
      ErrorConsole.getConsole();
    }
    if (!ArgParser.handleCommandLineArgs(args, COMMAND_LINE_ARGS)) {
      usage();
      return;
    }

    LookAndFeel.setupLookAndFeel();

    if (HttpProxy.isUsingSystemProxy()) {
      HttpProxy.updateSystemProxy();
    }

    final String version = System.getProperty(TRIPLEA_ENGINE_VERSION_BIN);
    final Version engineVersion = ClientContext.engineVersion();
    if (version != null && version.length() > 0) {
      final Version testVersion;
      try {
        testVersion = new Version(version);
        // if successful we don't do anything
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + version);
        if (!engineVersion.equals(testVersion, false)) {
          System.out.println("Current Engine version in use: " + engineVersion);
        }
      } catch (final Exception e) {
        System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, engineVersion.toString());
        System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + engineVersion);
      }
    } else {
      System.getProperties().setProperty(TRIPLEA_ENGINE_VERSION_BIN, engineVersion.toString());
      System.out.println(TRIPLEA_ENGINE_VERSION_BIN + ":" + engineVersion);
    }

    if (ClientContext.gameEnginePropertyReader().useJavaFxUi()) {
      TripleA.launch(args);
    } else {
      SwingUtilities.invokeLater(() -> {
        setupPanelModel.showSelectType();
        mainFrame = newMainFrame();
      });

      showMainFrame();
      new Thread(GameRunner::setupLogging).start();
      new Thread(GameRunner::checkLocalSystem).start();
      new Thread(GameRunner::checkForUpdates).start();
    }
  }

  private static JFrame newMainFrame() {
    final JFrame frame = new JFrame("TripleA");

    frame.add(new MainPanel(setupPanelModel));
    frame.pack();

    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    frame.setIconImage(getGameIcon(frame));
    frame.setLocationRelativeTo(null);

    return frame;
  }

  public static FileDialog newFileDialog() {
    return new FileDialog(mainFrame);
  }

  public static Optional<File> showFileChooser(final FileFilter fileFilter) {
    final JFileChooser fileChooser = new JFileChooser();
    fileChooser.setFileFilter(fileFilter);
    final int returnCode = fileChooser.showOpenDialog(mainFrame);

    if (returnCode == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    }
    return Optional.empty();
  }


  public static Optional<File> showSaveGameFileChooser() {
    // Non-Mac platforms should use the normal Swing JFileChooser
    final JFileChooser fileChooser = SaveGameFileChooser.getInstance();
    final int rVal = fileChooser.showOpenDialog(mainFrame);
    if (rVal == JFileChooser.APPROVE_OPTION) {
      return Optional.of(fileChooser.getSelectedFile());
    }
    return Optional.empty();
  }

  public static ProgressWindow newProgressWindow(final String title) {
    return new ProgressWindow(mainFrame, title);
  }

  public static WaitDialog newWaitDialog(final String message) {
    return new WaitDialog(mainFrame, message);
  }

  /**
   * Strong type for dialog titles. Keeps clear which data is for message body and title, avoids parameter swapping
   * problem and makes refactoring easier.
   */
  public static class Title {
    public String value;

    private Title(final String value) {
      this.value = value;
    }

    public static Title of(final String value) {
      return new Title(value);
    }
  }

  public static int showConfirmDialog(final String message, final Title title, final int optionType,
      final int messageType) {
    return JOptionPane.showConfirmDialog(mainFrame, message, title.value, optionType, messageType);
  }


  public static void showMessageDialog(final String message, final Title title, final int messageType) {
    JOptionPane.showMessageDialog(mainFrame, message, title.value, messageType);
  }


  public static void hideMainFrame() {
    SwingUtilities.invokeLater(() -> mainFrame.setVisible(false));
  }


  /**
   * Sets the 'main frame' to visible. In this context the main frame is the initial
   * welcome (launch lobby/single player game etc..) screen presented to GUI enabled clients.
   */
  public static void showMainFrame() {
    SwingUtilities.invokeLater(() -> {
      mainFrame.requestFocus();
      mainFrame.toFront();
      mainFrame.setVisible(true);

      SwingComponents.addWindowClosingListener(mainFrame, GameRunner::exitGameIfFinished);

      ProAI.gameOverClearCache();
      new Thread(() -> {
        gameSelectorModel.loadDefaultGame(false);
        final String fileName = System.getProperty(GameRunner.TRIPLEA_GAME_PROPERTY, "");
        if (fileName.length() > 0) {
          gameSelectorModel.load(new File(fileName), mainFrame);
        }
        final String downloadableMap = System.getProperty(GameRunner.TRIPLEA_MAP_DOWNLOAD_PROPERTY, "");
        if (!downloadableMap.isEmpty()) {
          SwingUtilities.invokeLater(() -> DownloadMapsWindow.showDownloadMapsWindowAndDownload(downloadableMap));
        }
      }).start();

      if (System.getProperty(GameRunner.TRIPLEA_SERVER_PROPERTY, "false").equals("true")) {
        setupPanelModel.showServer(mainFrame);
      } else if (System.getProperty(GameRunner.TRIPLEA_CLIENT_PROPERTY, "false").equals("true")) {
        setupPanelModel.showClient(mainFrame);
      }
    });
  }

  private static void checkLocalSystem() {
    final LocalSystemChecker localSystemChecker = new LocalSystemChecker();
    final Collection<Exception> exceptions = localSystemChecker.getExceptions();
    if (!exceptions.isEmpty()) {
      final String msg = String.format(
          "Warning!! %d system checks failed. Some game features may not be available or may not work correctly.\n%s",
          exceptions.size(), localSystemChecker.getStatusMessage());
      ClientLogger.logError(msg, exceptions);
    }
  }


  private static void usage() {
    System.out.println("\nUsage and Valid Arguments:\n"
        + "   " + TRIPLEA_GAME_PROPERTY + "=<FILE_NAME>\n"
        + "   " + TRIPLEA_GAME_HOST_CONSOLE_PROPERTY + "=<true/false>\n"
        + "   " + TRIPLEA_SERVER_PROPERTY + "=true\n"
        + "   " + TRIPLEA_PORT_PROPERTY + "=<PORT>\n"
        + "   " + TRIPLEA_NAME_PROPERTY + "=<PLAYER_NAME>\n"
        + "   " + LOBBY_HOST + "=<LOBBY_HOST>\n"
        + "   " + TRIPLEA_LOBBY_PORT_PROPERTY + "=<LOBBY_PORT>\n"
        + "   " + LOBBY_GAME_COMMENTS + "=<LOBBY_GAME_COMMENTS>\n"
        + "   " + LOBBY_GAME_HOSTED_BY + "=<LOBBY_GAME_HOSTED_BY>\n"
        + "   " + LOBBY_GAME_SUPPORT_EMAIL + "=<youremail@emailprovider.com>\n"
        + "   " + LOBBY_GAME_SUPPORT_PASSWORD + "=<password for remote actions, such as remote stop game>\n"
        + "   " + LOBBY_GAME_RECONNECTION + "=<seconds between refreshing lobby connection [min "
        + LOBBY_RECONNECTION_REFRESH_SECONDS_MINIMUM + "]>\n"
        + "   " + TRIPLEA_SERVER_START_GAME_SYNC_WAIT_TIME + "=<seconds to wait for all clients to start the game>\n"
        + "   " + TRIPLEA_SERVER_OBSERVER_JOIN_WAIT_TIME + "=<seconds to wait for an observer joining the game>\n"
        + "   " + MAP_FOLDER + "=mapFolder"
        + "\n"
        + "   You must start the Name and HostedBy with \"Bot\".\n"
        + "   Game Comments must have this string in it: \"automated_host\".\n"
        + "   You must include a support email for your host, so that you can be alerted by lobby admins when your "
        + "host has an error."
        + " (For example they may email you when your host is down and needs to be restarted.)\n"
        + "   Support password is a remote access password that will allow lobby admins to remotely take the "
        + "following actions: ban player, stop game, shutdown server."
        + " (Please email this password to one of the lobby moderators, or private message an admin on the "
        + "TripleaWarClub.org website forum.)\n");
  }


  private static void setupLogging() {
    Toolkit.getDefaultToolkit().getSystemEventQueue().push(new EventQueue() {
      @Override
      protected void dispatchEvent(final AWTEvent newEvent) {
        try {
          super.dispatchEvent(newEvent);
          // This ensures, that all exceptions/errors inside any swing framework (like substance) are logged correctly
        } catch (final Throwable t) {
          ClientLogger.logError(t);
          throw t;
        }
      }
    });
  }

  private static void checkForUpdates() {
    new Thread(() -> {
      // do not check if we are the old extra jar. (a jar kept for backwards compatibility only)
      if (ClientFileSystemHelper.areWeOldExtraJar()) {
        return;
      }
      if (System.getProperty(TRIPLEA_SERVER_PROPERTY, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_CLIENT_PROPERTY, "false").equalsIgnoreCase("true")) {
        return;
      }
      if (System.getProperty(TRIPLEA_DO_NOT_CHECK_FOR_UPDATES, "false").equalsIgnoreCase("true")) {
        return;
      }

      // if we are joining a game online, or hosting, or loading straight into a savegame, do not check
      final String fileName = System.getProperty(TRIPLEA_GAME_PROPERTY, "");
      if (fileName.trim().length() > 0) {
        return;
      }

      boolean busy = false;
      busy = checkForTutorialMap();
      if (!busy) {
        busy = checkForLatestEngineVersionOut();
      }
      if (!busy) {
        busy = checkForUpdatedMaps();
      }
    }, "Checking Latest TripleA Engine Version").start();
  }

  /**
   * @return true if we are out of date or this is the first time this triplea has ever been run.
   */
  private static boolean checkForLatestEngineVersionOut() {
    try {
      final boolean firstTimeThisVersion = ClientSetting.TRIPLEA_FIRST_TIME_THIS_VERSION_PROPERTY.booleanValue();
      // check at most once per 2 days (but still allow a 'first run message' for a new version of triplea)
      final LocalDateTime localDateTime = LocalDateTime.now();
      final int year = localDateTime.get(ChronoField.YEAR);
      final int day = localDateTime.get(ChronoField.DAY_OF_YEAR);
      // format year:day
      final String lastCheckTime = ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.value();
      if (!firstTimeThisVersion && lastCheckTime != null && lastCheckTime.trim().length() > 0) {
        final String[] yearDay = lastCheckTime.split(":");
        if (Integer.parseInt(yearDay[0]) >= year && Integer.parseInt(yearDay[1]) + 1 >= day) {
          return false;
        }
      }

      ClientSetting.TRIPLEA_LAST_CHECK_FOR_ENGINE_UPDATE.save(year + ":" + day);
      ClientSetting.flush();

      final EngineVersionProperties latestEngineOut = EngineVersionProperties.contactServerForEngineVersionProperties();
      if (latestEngineOut == null) {
        return false;
      }
      if (ClientContext.engineVersion()
          .isLessThan(latestEngineOut.getLatestVersionOut())) {
        SwingUtilities
            .invokeLater(() -> EventThreadJOptionPane.showMessageDialog(null, latestEngineOut.getOutOfDateComponent(),
                "Please Update TripleA", JOptionPane.INFORMATION_MESSAGE, false, new CountDownLatchHandler()));
        return true;
      }
    } catch (final Exception e) {
      ClientLogger.logError("Error while checking for engine updates", e);
    }
    return false;
  }

  private static boolean checkForTutorialMap() {
    final MapDownloadController mapDownloadController = ClientContext.mapDownloadController();
    final boolean promptToDownloadTutorialMap = mapDownloadController.shouldPromptToDownloadTutorialMap();
    mapDownloadController.preventPromptToDownloadTutorialMap();
    if (!promptToDownloadTutorialMap) {
      return false;
    }

    final StringBuilder messageBuilder = new StringBuilder();
    messageBuilder.append("<html>");
    messageBuilder.append("Would you like to download the tutorial map?<br>");
    messageBuilder.append("<br>");
    messageBuilder.append("(You can always download it later using the Download Maps<br>");
    messageBuilder.append("command if you don't want to do it now.)");
    messageBuilder.append("</html>");
    SwingComponents.promptUser("Welcome to TripleA", messageBuilder.toString(), () -> {
      DownloadMapsWindow.showDownloadMapsWindowAndDownload("Tutorial");
    });
    return true;
  }

  /**
   * @return true if we have any out of date maps.
   */
  private static boolean checkForUpdatedMaps() {
    final MapDownloadController downloadController = ClientContext.mapDownloadController();
    return downloadController.checkDownloadedMapsAreLatest();
  }


  public static Image getGameIcon(final Window frame) {
    Image img = null;
    try {
      img = frame.getToolkit().getImage(GameRunner.class.getResource("ta_icon.png"));
    } catch (final Exception ex) {
      ClientLogger.logError("ta_icon.png not loaded", ex);
    }
    final MediaTracker tracker = new MediaTracker(frame);
    tracker.addImage(img, 0);
    try {
      tracker.waitForAll();
    } catch (final InterruptedException ex) {
      ClientLogger.logQuietly(ex);
    }
    return img;
  }

  static void startGame(final String savegamePath, final String classpath) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands, classpath);
    if (savegamePath != null && savegamePath.length() > 0) {
      commands.add("-D" + GameRunner.TRIPLEA_GAME_PROPERTY + "=" + savegamePath);
    }
    // add in any existing command line items
    for (final String property : GameRunner.COMMAND_LINE_ARGS) {
      // we add game property above, and we add version bin in the populateBasicJavaArgs
      if (GameRunner.TRIPLEA_GAME_PROPERTY.equals(property)
          || GameRunner.TRIPLEA_ENGINE_VERSION_BIN.equals(property)) {
        continue;
      }
      final String value = System.getProperty(property);
      if (value != null) {
        commands.add("-D" + property + "=" + value);
      } else if (GameRunner.LOBBY_HOST.equals(property) || GameRunner.TRIPLEA_LOBBY_PORT_PROPERTY.equals(property)
          || GameRunner.LOBBY_GAME_HOSTED_BY.equals(property)) {
        // for these 3 properties, we clear them after hosting, but back them up.
        final String oldValue = System.getProperty(property + GameRunner.OLD_EXTENSION);
        if (oldValue != null) {
          commands.add("-D" + property + "=" + oldValue);
        }
      }
    }
    // classpath for main
    commands.add(GameRunner.class.getName());
    ProcessRunnerUtil.exec(commands);
  }

  public static void hostGame(final int port, final String playerName, final String comments, final String password,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands);
    commands.add("-D" + TRIPLEA_SERVER_PROPERTY + "=true");
    commands.add("-D" + TRIPLEA_PORT_PROPERTY + "=" + port);
    commands.add("-D" + TRIPLEA_NAME_PROPERTY + "=" + playerName);
    commands.add("-D" + LOBBY_HOST + "="
        + messengers.getMessenger().getRemoteServerSocketAddress().getAddress().getHostAddress());
    commands
        .add("-D" + GameRunner.TRIPLEA_LOBBY_PORT_PROPERTY + "="
            + messengers.getMessenger().getRemoteServerSocketAddress().getPort());
    commands.add("-D" + LOBBY_GAME_COMMENTS + "=" + comments);
    commands.add("-D" + LOBBY_GAME_HOSTED_BY + "=" + messengers.getMessenger().getLocalNode().getName());
    if (password != null && password.length() > 0) {
      commands.add("-D" + TRIPLEA_SERVER_PASSWORD_PROPERTY + "=" + password);
    }
    final String fileName = System.getProperty(TRIPLEA_GAME_PROPERTY, "");
    if (fileName.length() > 0) {
      commands.add("-D" + TRIPLEA_GAME_PROPERTY + "=" + fileName);
    }
    final String javaClass = GameRunner.class.getName();
    commands.add(javaClass);
    ProcessRunnerUtil.exec(commands);
  }

  public static void joinGame(final GameDescription description, final Messengers messengers, final Container parent) {
    final GameDescription.GameStatus status = description.getStatus();
    if (GameDescription.GameStatus.LAUNCHING.equals(status)) {
      return;
    }
    final Version engineVersionOfGameToJoin = new Version(description.getEngineVersion());
    String newClassPath = null;
    final boolean sameVersion =
        ClientContext.engineVersion().equals(engineVersionOfGameToJoin);
    if (!sameVersion) {
      try {
        newClassPath = findOldJar(engineVersionOfGameToJoin, false);
      } catch (final Exception e) {
        if (ClientFileSystemHelper.areWeOldExtraJar()) {
          JOptionPane.showMessageDialog(parent,
              "<html>Please run the default TripleA and try joining the online lobby for it instead. "
                  + "<br>This TripleA engine is old and kept only for backwards compatibility and can only play with "
                  + "people using the exact same version as this one. "
                  + "<br><br>Host is using a different engine than you, and cannot find correct engine: "
                  + engineVersionOfGameToJoin.toStringFull("_") + "</html>",
              "Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
        } else {
          JOptionPane.showMessageDialog(parent,
              "Host is using a different engine than you, and cannot find correct engine: "
                  + engineVersionOfGameToJoin.toStringFull("_"),
              "Correct TripleA Engine Not Found", JOptionPane.WARNING_MESSAGE);
        }
        return;
      }
      // ask user if we really want to do this?
      final String messageString = "<html>This TripleA engine is version "
          + ClientContext.engineVersion()
          + " and you are trying to join a game made with version " + engineVersionOfGameToJoin.toString()
          + "<br>However, this TripleA can only play with engines that are the exact same version as itself (x_x_x_x)."
          + "<br><br>TripleA now comes with older engines included with it, and has found the engine used by the host. "
          + "This is a new feature and is in 'beta' stage."
          + "<br>It will attempt to run a new instance of TripleA using the older engine jar file, and this instance "
          + "will join the host's game."
          + "<br>Your current instance will not be closed. Please report any bugs or issues."
          + "<br><br>Do you wish to continue?</html>";
      final int answer = JOptionPane.showConfirmDialog(null, messageString, "Run old jar to join hosted game?",
          JOptionPane.YES_NO_OPTION);
      if (answer != JOptionPane.YES_OPTION) {
        return;
      }
    }
    joinGame(description.getPort(), description.getHostedBy().getAddress().getHostAddress(), newClassPath, messengers);
  }

  // newClassPath can be null
  private static void joinGame(final int port, final String hostAddressIp, final String newClassPath,
      final Messengers messengers) {
    final List<String> commands = new ArrayList<>();
    ProcessRunnerUtil.populateBasicJavaArgs(commands, newClassPath);
    final String prefix = "-D";
    commands.add(prefix + TRIPLEA_CLIENT_PROPERTY + "=true");
    commands.add(prefix + TRIPLEA_PORT_PROPERTY + "=" + port);
    commands.add(prefix + TRIPLEA_HOST_PROPERTY + "=" + hostAddressIp);
    commands.add(prefix + TRIPLEA_NAME_PROPERTY + "=" + messengers.getMessenger().getLocalNode().getName());
    commands.add(GameRunner.class.getName());
    ProcessRunnerUtil.exec(commands);
  }

  static String findOldJar(final Version oldVersionNeeded, final boolean ignoreMicro) throws IOException {
    if (ClientContext.engineVersion().equals(oldVersionNeeded, ignoreMicro)) {
      return System.getProperty("java.class.path");
    }
    // first, see if the default/main triplea can run it
    if (ClientFileSystemHelper.areWeOldExtraJar()) {
      final String version = System.getProperty(GameRunner.TRIPLEA_ENGINE_VERSION_BIN);
      if (version != null && version.length() > 0) {
        Version defaultVersion = null;
        try {
          defaultVersion = new Version(version);
        } catch (final Exception e) {
          // nothing, just continue
        }
        if (defaultVersion != null) {
          if (defaultVersion.equals(oldVersionNeeded, ignoreMicro)) {
            final String jarName = "triplea.jar";
            // windows is in 'bin' folder, mac is in 'Java' folder.
            File binFolder = new File(ClientFileSystemHelper.getRootFolder(), "bin/");
            if (!binFolder.exists()) {
              binFolder = new File(ClientFileSystemHelper.getRootFolder(), "Java/");
            }
            if (binFolder.exists()) {
              final File[] files = binFolder.listFiles();
              if (files == null) {
                throw new IOException("Cannot find 'bin' engine jars folder");
              }
              File ourBinJar = null;
              for (final File f : Arrays.asList(files)) {
                if (!f.exists()) {
                  continue;
                }
                final String jarPath = f.getCanonicalPath();
                if (jarPath.contains(jarName)) {
                  ourBinJar = f;
                  break;
                }
              }
              if (ourBinJar == null) {
                throw new IOException(
                    "Cannot find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
              }
              final String newClassPath = ourBinJar.getCanonicalPath();
              if (newClassPath.length() <= 0) {
                throw new IOException(
                    "Cannot find 'bin' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
              }
              return newClassPath;
            } else {
              System.err.println("Cannot find 'bin' or 'Java' folder, where main triplea.jar should be.");
            }
          }
        }
      }
    }
    // so, what we do here is try to see if our installed copy of triplea includes older jars with it that are the same
    // engine as was used
    // for this savegame, and if so try to run it
    // we don't care what the last (micro) number is of the version number. example: triplea 1.5.2.1 can open 1.5.2.0
    // savegames.
    final String jarName = "triplea_" + oldVersionNeeded.toStringFull("_", ignoreMicro);
    final File oldJarsFolder = new File(ClientFileSystemHelper.getRootFolder(), "old/");
    if (!oldJarsFolder.exists()) {
      throw new IOException("Cannot find 'old' engine jars folder");
    }
    final File[] files = oldJarsFolder.listFiles();
    if (files == null) {
      throw new IOException("Cannot find 'old' engine jars folder");
    }
    File ourOldJar = null;
    for (final File f : Arrays.asList(files)) {
      if (!f.exists()) {
        continue;
      }
      // final String jarPath = f.getCanonicalPath();
      final String name = f.getName();
      if (name.contains(jarName) && name.contains(".jar")) {
        ourOldJar = f;
        break;
      }
    }
    if (ourOldJar == null) {
      throw new IOException("Cannot find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
    }
    final String newClassPath = ourOldJar.getCanonicalPath();
    if (newClassPath.length() <= 0) {
      throw new IOException("Cannot find 'old' engine jar for version: " + oldVersionNeeded.toStringFull("_"));
    }
    return newClassPath;
  }


  public static void exitGameIfFinished() {
    SwingUtilities.invokeLater(() -> {
      boolean allFramesClosed = true;
      for (final Frame f : Frame.getFrames()) {
        if (f.isVisible()) {
          allFramesClosed = false;
          break;
        }
      }
      if (allFramesClosed) {
        System.exit(0);
      }
    });
  }

  /**
   * todo, replace with something better
   * Get the chat for the game, or null if there is no chat.
   */
  public static Chat getChat() {
    final ISetupPanel model = setupPanelModel.getPanel();
    if (model instanceof ServerSetupPanel) {
      return model.getChatPanel().getChat();
    } else if (model instanceof ClientSetupPanel) {
      return model.getChatPanel().getChat();
    } else {
      return null;
    }
  }

  public static boolean hasChat() {
    return getChat() != null;
  }

  /**
   * After the game has been left, call this.
   */
  public static void clientLeftGame() {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingAction.invokeAndWait(GameRunner::clientLeftGame);
      return;
    }
    // having an oddball issue with the zip stream being closed while parsing to load default game. might be caused by
    // closing of stream while unloading map resources.
    ThreadUtil.sleep(100);
    setupPanelModel.showSelectType();
    showMainFrame();
  }

  public static void quitGame() {
    mainFrame.dispatchEvent(new WindowEvent(mainFrame, WindowEvent.WINDOW_CLOSING));
  }

}
