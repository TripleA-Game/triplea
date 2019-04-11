package org.triplea.game.client.ui.javafx.screens;

import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.ScreenController;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import com.google.common.annotations.VisibleForTesting;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.layout.BorderPane;
import lombok.NoArgsConstructor;

/**
 * Controller class for the Game Type selection screen.
 */
@NoArgsConstructor
public class GameSelectionControls implements ControlledScreen<ScreenController<FxmlManager>> {

  @FXML
  private BorderPane gameOptions;

  private ScreenController<FxmlManager> screenController;

  @VisibleForTesting
  GameSelectionControls(final BorderPane gameOptions) {
    this.gameOptions = gameOptions;
  }

  @FXML
  private void showLobbyMenu() {}

  @FXML
  private void showLocalGameMenu() {}

  @FXML
  private void showHostNetworkGameMenu() {}

  @FXML
  private void showJoinNetworkGameMenu() {}

  @FXML
  private void showPlayByForumMenu() {}

  @FXML
  private void showPlayByEmailMenu() {}

  @Override
  public void connect(final ScreenController<FxmlManager> screenController) {
    this.screenController = screenController;
  }

  @Override
  public Node getNode() {
    return gameOptions;
  }

  @FXML
  @VisibleForTesting
  void back() {
    screenController.switchScreen(FxmlManager.MAIN_MENU_CONTROLS);
  }
}
