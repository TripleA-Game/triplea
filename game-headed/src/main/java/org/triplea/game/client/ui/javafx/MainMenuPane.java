package org.triplea.game.client.ui.javafx;

import java.text.MessageFormat;
import java.util.function.Function;

import javax.swing.SwingUtilities;

import org.triplea.awt.OpenFileUtility;
import org.triplea.game.client.ui.javafx.screen.ControlledScreen;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.screen.RootActionPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

import games.strategy.engine.ClientContext;
import games.strategy.engine.framework.map.download.DownloadMapsWindow;
import games.strategy.triplea.UrlConstants;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.NumberBinding;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

/**
 * Controller representing the MainMenu JavaFX implementation.
 */
public class MainMenuPane implements ControlledScreen<NavigationPane> {

  private RootActionPane actionPane;
  private NavigationPane screenController;

  @FXML
  private Button buttonBack;

  @FXML
  private Label version;

  @FXML
  private VBox aboutSection;

  @FXML
  private HBox gameOptions;

  @FXML
  private VBox mainOptions;

  @FXML
  private BorderPane root;

  @FXML
  private void initialize() {
    version.setText(MessageFormat.format(version.getText(), ClientContext.engineVersion().getExactVersion()));
    applyFileSelectionAnimation();
  }

  private void applyFileSelectionAnimation() {
    mainOptions.lookupAll(".button").forEach(node -> {
      final Function<Node, NumberBinding> hoverBinding = n -> Bindings.when(n.hoverProperty()).then(-10).otherwise(0);
      final NumberBinding numberBinding = hoverBinding.apply(node);
      node.translateYProperty().bind(numberBinding.multiply(-1));
      node.getParent().translateYProperty().bind(!mainOptions.equals(node.getParent().getParent())
          ? Bindings.add(numberBinding,
              hoverBinding.apply(node.getParent().getParent().getChildrenUnmodifiable().get(0)).multiply(-1))
          : numberBinding);
    });
  }

  @FXML
  private void showLastMenu() {
    // TODO check which menu we are in
    aboutSection.setVisible(false);
    gameOptions.setVisible(false);
    buttonBack.setVisible(false);
    mainOptions.setVisible(true);
  }

  @FXML
  private void showHelp() {
    open(UrlConstants.GITHUB_HELP);
  }

  @FXML
  private void showRuleBook() {
    open(UrlConstants.RULE_BOOK);
  }

  private void open(final String url) {
    OpenFileUtility.openUrl(url, () -> new Alert(Alert.AlertType.INFORMATION, url, ButtonType.CLOSE).show());
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

  @FXML
  private void showPlayOptions() {
    mainOptions.setVisible(false);
    gameOptions.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showDownloadMenu() {
    SwingUtilities.invokeLater(DownloadMapsWindow::showDownloadMapsWindow);
  }

  @FXML
  private void showSettingsMenu() {
    screenController.switchScreen(FxmlManager.SETTINGS_PANE);
  }

  @FXML
  private void showAboutSection() {
    mainOptions.setVisible(false);
    aboutSection.setVisible(true);
    buttonBack.setVisible(true);
  }

  @FXML
  private void showExitConfirmDialog() {
    actionPane.promptExit();
  }

  @Override
  public void connect(final NavigationPane screenController) {
    this.screenController = screenController;
  }

  void setRootActionPane(final RootActionPane actionPane) {
    this.actionPane = actionPane;
  }

  @Override
  public Node getNode() {
    return root;
  }
}
