package org.triplea.game.client.ui.javafx.screens;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.ClientContext;
import java.util.ArrayList;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.mockito.internal.util.reflection.FieldSetter;
import org.triplea.game.client.ui.javafx.screen.NavigationPane;
import org.triplea.game.client.ui.javafx.util.FxmlManager;

class MainMenuPaneTest {

  @Test
  void testGetNode() {
    final BorderPane mock = mock(BorderPane.class);
    final MainMenuPane aboutInformation = new MainMenuPane(null, mock, null, null);
    assertEquals(mock, aboutInformation.getNode());
  }

  @Test
  void testInitialize() throws Exception {
    final NavigationPane mock = mock(NavigationPane.class);
    final StackPane mock2 = mock(StackPane.class);
    FieldSetter.setField(null, Application.class.getDeclaredField("userAgentStylesheet"), "");
    final Label mock3 = mock(Label.class);
    final Node mock4 = mock(Node.class);
    final NavigationPane mock5 = mock(NavigationPane.class);
    final ObservableList<Node> children = FXCollections.observableList(new ArrayList<>());

    when(mock.getNode()).thenReturn(mock4);
    when(mock2.getChildren()).thenReturn(children);
    mock3.setText("Test String {0}");

    final MainMenuPane aboutInformation = new MainMenuPane(() -> mock, null, mock2, mock3);

    aboutInformation.initialize();

    assertEquals("Test String " + ClientContext.engineVersion().toString(), mock3.getText());
    assertEquals(1, children.size());
    assertEquals(mock4, children.get(0));

    verify(mock).registerScreen(FxmlManager.GAME_SELECTION_CONTROLS);
    verify(mock).registerScreen(FxmlManager.ABOUT_INFORMATION);
    verify(mock).registerScreen(FxmlManager.MAIN_MENU_CONTROLS);

    verify(mock).switchScreen(FxmlManager.MAIN_MENU_CONTROLS);

    aboutInformation.connect(mock5);

    verify(mock).setParent(mock5);
  }

  @AfterAll
  static void cleanup() throws Exception {
    FieldSetter.setField(null, Application.class.getDeclaredField("userAgentStylesheet"), null);
  }
}
