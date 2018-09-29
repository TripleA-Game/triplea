package games.strategy.triplea.ui;

import java.awt.Frame;
import java.util.Collection;
import java.util.Optional;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;

import games.strategy.engine.data.Territory;
import games.strategy.ui.SwingComponents;
import swinglib.JButtonBuilder;
import swinglib.JPanelBuilder;

final class SelectTerritoryDialog extends JDialog {
  private static final long serialVersionUID = -1601616824595826610L;

  private Result result = Result.CANCEL;
  private final JComboBox<Territory> territoryComboBox;

  SelectTerritoryDialog(final Frame owner, final String title, final Collection<Territory> territories) {
    super(owner, title, true);

    territoryComboBox = new JComboBox<>(territories.toArray(new Territory[0]));
    final JButton okButton = JButtonBuilder.builder()
        .okTitle()
        .actionListener(() -> close(Result.OK))
        .build();
    getRootPane().setDefaultButton(okButton);

    add(JPanelBuilder.builder()
        .borderEmpty(10)
        .verticalBoxLayout()
        .add(territoryComboBox)
        .addVerticalStrut(20)
        .add(JPanelBuilder.builder()
            .horizontalBoxLayout()
            .addHorizontalGlue()
            .add(okButton)
            .addHorizontalStrut(5)
            .add(JButtonBuilder.builder()
                .cancelTitle()
                .actionListener(() -> close(Result.CANCEL))
                .build())
            .build())
        .build());
    pack();
    setLocationRelativeTo(owner);

    SwingComponents.addEscapeKeyListener(this, () -> close(Result.CANCEL));
  }

  Optional<Territory> open() {
    setVisible(true);

    switch (result) {
      case OK:
        return Optional.of((Territory) territoryComboBox.getSelectedItem());
      case CANCEL:
        return Optional.empty();
      default:
        throw new AssertionError("unknown result: " + result);
    }
  }

  private void close(final Result result) {
    setVisible(false);
    dispose();
    this.result = result;
  }

  private enum Result {
    OK, CANCEL
  }
}
