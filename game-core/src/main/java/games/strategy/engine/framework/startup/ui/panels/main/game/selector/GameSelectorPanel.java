package games.strategy.engine.framework.startup.ui.panels.main.game.selector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.util.HashMap;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;

import games.strategy.engine.ClientContext;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.engine.data.properties.PropertiesUi;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.engine.framework.startup.mc.ClientModel;
import games.strategy.engine.framework.startup.mc.GameSelectorModel;
import games.strategy.engine.framework.startup.ui.FileBackedGamePropertiesCache;
import games.strategy.engine.framework.startup.ui.IGamePropertiesCache;
import games.strategy.engine.framework.ui.GameChooser;
import games.strategy.engine.framework.ui.GameChooserEntry;
import games.strategy.engine.framework.ui.SaveGameFileChooser;
import games.strategy.ui.SwingAction;
import games.strategy.util.Interruptibles;
import swinglib.JButtonBuilder;

/**
 * Left hand side panel of the launcher screen that has various info, like selected game and engine version.
 */
public class GameSelectorPanel extends JPanel implements Observer {
  private static final long serialVersionUID = -4598107601238030020L;
  private final GameSelectorModel model;
  private final IGamePropertiesCache gamePropertiesCache = new FileBackedGamePropertiesCache();
  private final Map<String, Object> originalPropertiesMap = new HashMap<>();
  private final JLabel engineVersionLabel = new JLabel("Engine Version:");
  private final JLabel engineVersionText = new JLabel(ClientContext.engineVersion().getExactVersion());
  private final JLabel nameText = new JLabel();
  private final JLabel versionText = new JLabel();
  private final JLabel fileNameLabel = new JLabel("File Name:");
  private final JLabel fileNameText = new JLabel();
  private final JLabel nameLabel = new JLabel("Map Name:");
  private final JLabel versionLabel = new JLabel("Map Version:");
  private final JLabel roundLabel = new JLabel("Game Round:");
  private final JLabel roundText = new JLabel();
  private final JButton loadSavedGame = JButtonBuilder.builder()
      .title("Open Saved Game")
      .toolTip("Open a previously saved game, or an autosave.")
      .build();
  private final JButton loadNewGame = JButtonBuilder.builder()
      .title("Select Map")
      .toolTip("<html>Select a game from all the maps/games that come with TripleA, <br>and the ones "
          + "you have downloaded.</html>")
      .build();
  private final JButton gameOptions = JButtonBuilder.builder()
      .title("Map Options")
      .toolTip("<html>Set options for the currently selected game, <br>such as enabling/disabling "
          + "Low Luck, or Technology, etc.</html>")
      .build();

  public GameSelectorPanel(final GameSelectorModel model) {
    this.model = model;
    final GameData data = model.getGameData();
    if (data != null) {
      setOriginalPropertiesMap(data);
      gamePropertiesCache.loadCachedGamePropertiesInto(data);
    }

    setLayout(new GridBagLayout());
    add(engineVersionLabel, buildGridCell(0, 0, new Insets(10, 10, 3, 5)));
    add(engineVersionText, buildGridCell(1, 0, new Insets(10, 0, 3, 0)));

    add(nameLabel, buildGridCell(0, 1, new Insets(0, 10, 3, 5)));
    add(nameText, buildGridCell(1, 1, new Insets(0, 0, 3, 0)));

    add(versionLabel, buildGridCell(0, 2, new Insets(0, 10, 3, 5)));
    add(versionText, buildGridCell(1, 2, new Insets(0, 0, 3, 0)));

    add(roundLabel, buildGridCell(0, 3, new Insets(0, 10, 3, 5)));
    add(roundText, buildGridCell(1, 3, new Insets(0, 0, 3, 0)));

    add(fileNameLabel, buildGridCell(0, 4, new Insets(20, 10, 3, 5)));

    add(fileNameText, buildGridRow(0, 5, new Insets(0, 10, 3, 5)));

    add(loadNewGame, buildGridRow(0, 6, new Insets(25, 10, 10, 10)));

    add(loadSavedGame, buildGridRow(0, 7, new Insets(0, 10, 10, 10)));

    final JButton downloadMapButton = JButtonBuilder.builder()
        .title("Download Maps")
        .toolTip("Click this button to install additional maps")
        .actionListener(DownloadMapsWindow::showDownloadMapsWindow)
        .build();
    add(downloadMapButton, buildGridRow(0, 8, new Insets(0, 10, 10, 10)));

    add(gameOptions, buildGridRow(0, 9, new Insets(25, 10, 10, 10)));

    // spacer
    add(new JPanel(), new GridBagConstraints(0, 10, 2, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH,
        new Insets(0, 0, 0, 0), 0, 0));

    loadNewGame.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectGameFile();
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          clientModelForHostBots.getHostBotSetMapClientAction(GameSelectorPanel.this).actionPerformed(e);
        }
      }
    });
    loadSavedGame.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectSavedGameFile();
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          final JPopupMenu menu = new JPopupMenu();
          menu.add(clientModelForHostBots.getHostBotChangeGameToSaveGameClientAction());
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE));
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_ODD));
          menu.add(clientModelForHostBots.getHostBotChangeToAutosaveClientAction(GameSelectorPanel.this,
              SaveGameFileChooser.AUTOSAVE_TYPE.AUTOSAVE_EVEN));
          menu.add(clientModelForHostBots.getHostBotGetGameSaveClientAction(GameSelectorPanel.this));
          final Point point = loadSavedGame.getLocation();
          menu.show(GameSelectorPanel.this, point.x + loadSavedGame.getWidth(), point.y);
        }
      }
    });
    gameOptions.addActionListener(e -> {
      if (canSelectLocalGameData()) {
        selectGameOptions();
      } else if (canChangeHostBotGameData()) {
        final ClientModel clientModelForHostBots = model.getClientModelForHostBots();
        if (clientModelForHostBots != null) {
          clientModelForHostBots.getHostBotChangeGameOptionsClientAction(GameSelectorPanel.this).actionPerformed(e);
        }
      }
    });

    updateGameData();
  }

  private static GridBagConstraints buildGridCell(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 1);
  }

  private static GridBagConstraints buildGridRow(final int x, final int y, final Insets insets) {
    return buildGrid(x, y, insets, 2);
  }

  private static GridBagConstraints buildGrid(final int x, final int y, final Insets insets, final int width) {
    final int gridHeight = 1;
    final double weigthX = 0;
    final double weigthY = 0;
    final int anchor = GridBagConstraints.WEST;
    final int fill = GridBagConstraints.NONE;
    final int ipadx = 0;
    final int ipady = 0;

    return new GridBagConstraints(x, y, width, gridHeight, weigthX, weigthY, anchor, fill, insets, ipadx, ipady);
  }

  private void setOriginalPropertiesMap(final GameData data) {
    originalPropertiesMap.clear();
    if (data != null) {
      for (final IEditableProperty property : data.getProperties().getEditableProperties()) {
        originalPropertiesMap.put(property.getName(), property.getValue());
      }
    }
  }

  private void selectGameOptions() {
    // backup current game properties before showing dialog
    final Map<String, Object> currentPropertiesMap = new HashMap<>();
    for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
      currentPropertiesMap.put(property.getName(), property.getValue());
    }
    final PropertiesUi panel = new PropertiesUi(model.getGameData().getProperties(), true);
    final JScrollPane scroll = new JScrollPane(panel);
    scroll.setBorder(null);
    scroll.getViewport().setBorder(null);
    final JOptionPane pane = new JOptionPane(scroll, JOptionPane.PLAIN_MESSAGE);
    final String ok = "OK";
    final String cancel = "Cancel";
    final String makeDefault = "Make Default";
    final String reset = "Reset";
    pane.setOptions(new Object[] {ok, makeDefault, reset, cancel});
    final JDialog window = pane.createDialog(JOptionPane.getFrameForComponent(this), "Map Options");
    window.setVisible(true);
    final Object buttonPressed = pane.getValue();
    if (buttonPressed == null || buttonPressed.equals(cancel)) {
      // restore properties, if cancel was pressed, or window was closed
      for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
        property.setValue(currentPropertiesMap.get(property.getName()));
      }
    } else if (buttonPressed.equals(reset)) {
      if (!originalPropertiesMap.isEmpty()) {
        // restore properties, if cancel was pressed, or window was closed
        for (final IEditableProperty property : model.getGameData().getProperties().getEditableProperties()) {
          property.setValue(originalPropertiesMap.get(property.getName()));
        }
        selectGameOptions();
      }
    } else if (buttonPressed.equals(makeDefault)) {
      gamePropertiesCache.cacheGameProperties(model.getGameData());
    } else {
      // ok was clicked, and we have modified the properties already
    }
  }

  private void updateGameData() {
    SwingAction.invokeNowOrLater(() -> {
      nameText.setText(model.getGameName());
      versionText.setText(model.getGameVersion());
      roundText.setText(model.getGameRound());
      fileNameText.setText(model.getFormattedFileNameText());

      final boolean canSelectGameData = canSelectLocalGameData();
      final boolean canChangeHostBotGameData = canChangeHostBotGameData();
      loadSavedGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
      loadNewGame.setEnabled(canSelectGameData || canChangeHostBotGameData);
      // Disable game options if there are none.
      if (canChangeHostBotGameData || (canSelectGameData && model.getGameData() != null
          && model.getGameData().getProperties().getEditableProperties().size() > 0)) {
        gameOptions.setEnabled(true);
      } else {
        gameOptions.setEnabled(false);
      }
    });
  }

  private boolean canSelectLocalGameData() {
    return model != null && model.canSelect();
  }

  private boolean canChangeHostBotGameData() {
    return model != null && model.isHostHeadlessBot();
  }

  @Override
  public void update(final Observable o, final Object arg) {
    updateGameData();
  }

  private void selectSavedGameFile() {
    // For some strange reason,
    // the only way to get a Mac OS X native-style file dialog
    // is to use an AWT FileDialog instead of a Swing JDialog
    GameFileSelector.selectGameFile()
        .ifPresent(file -> Interruptibles
            .await(() -> GameRunner.newBackgroundTaskRunner().runInBackground("Loading savegame...", () -> {
              model.load(file);
              setOriginalPropertiesMap(model.getGameData());
            })));
  }

  private void selectGameFile() {
    try {
      final GameChooserEntry entry =
          GameChooser.chooseGame(JOptionPane.getFrameForComponent(this), model.getGameName());
      if (entry != null) {
        GameRunner.newBackgroundTaskRunner().runInBackground("Loading map...", () -> {
          if (!entry.isGameDataLoaded()) {
            try {
              entry.fullyParseGameData();
            } catch (final GameParseException e) {
              // TODO remove bad entries from the underlying model
              return;
            }
          }
          model.load(entry);
        });
        // warning: NPE check is not to protect against concurrency, another thread could still null out game data.
        // The NPE check is to protect against the case where there are errors loading game, in which case
        // we'll have a null game data.
        if (model.getGameData() != null) {
          setOriginalPropertiesMap(model.getGameData());
          // only for new games, not saved games, we set the default options, and set them only once
          // (the first time it is loaded)
          gamePropertiesCache.loadCachedGamePropertiesInto(model.getGameData());
        }
      }
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }
}
