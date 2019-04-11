package org.triplea.game.client.ui.javafx.screen;

import java.util.EnumMap;
import java.util.Map;

import org.triplea.game.client.ui.javafx.util.FxmlManager;

import com.google.common.base.Preconditions;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.layout.StackPane;

/**
 * The default implementation of {@link ScreenController}.
 * Using this class enables to swap screens with a rather flat
 * call tree by using the class name as the identifier.
 * Make sure to register Screens before using them.
 */
public class NavigationPane extends StackPane implements ScreenController<FxmlManager> {
  private final Map<FxmlManager, Node> screens = new EnumMap<>(FxmlManager.class);
  private NavigationPane parent;

  private void registerScreen(final FxmlManager manager, final ControlledScreen<NavigationPane> screen) {
    Preconditions.checkNotNull(screen);
    screens.put(manager, screen.getNode());
    screen.connect(this);
  }

  public void registerScreen(final FxmlManager manager) {
    Preconditions.checkState(Platform.isFxApplicationThread());
    Preconditions.checkNotNull(manager);
    registerScreen(manager, manager.<ControlledScreen<NavigationPane>, Object>load().getController());
  }

  @Override
  public void switchScreen(final FxmlManager identifier) {
    Preconditions.checkNotNull(identifier);

    // Pass request to parent if we can't handle it.
    if (!screens.containsKey(identifier)) {
      if (parent != null) {
        parent.switchScreen(identifier);
        return;
      }
      throw new IllegalArgumentException("Screen of Type " + identifier + " not present");
    }

    final Node screen = screens.get(identifier);

    if (getChildren().isEmpty()) {
      getChildren().add(screen);
    } else {
      getChildren().set(0, screen);
    }
  }

  public void setParent(final NavigationPane parent) {
    this.parent = parent;
  }
}
