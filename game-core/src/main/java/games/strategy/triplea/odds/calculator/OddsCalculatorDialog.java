package games.strategy.triplea.odds.calculator;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.WindowConstants;

import org.triplea.swing.SwingComponents;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.ui.TripleAFrame;
import games.strategy.triplea.ui.UiContext;

/**
 * A dialog that allows the user to set up an arbitrary battle and calculate the attacker's odds of successfully winning
 * the battle. Also known as the Battle Calculator.
 */
public class OddsCalculatorDialog extends JDialog {
  private static final long serialVersionUID = -7625420355087851930L;
  private static Point lastPosition;
  private static List<OddsCalculatorDialog> instances = new ArrayList<>();
  private final OddsCalculatorPanel panel;

  OddsCalculatorDialog(final GameData data, final UiContext uiContext, final JFrame parent, final Territory location) {
    super(parent, "Odds Calculator");
    panel = new OddsCalculatorPanel(data, uiContext, location, this);
    getContentPane().setLayout(new BorderLayout());
    getContentPane().add(panel, BorderLayout.CENTER);
    pack();
  }

  /**
   * Shows the Odds Calculator dialog and initializes it using the current state of the specified territory.
   */
  public static void show(final TripleAFrame taFrame, final Territory t) {
    final OddsCalculatorDialog dialog =
        new OddsCalculatorDialog(taFrame.getGame().getData(), taFrame.getUiContext(), taFrame, t);
    dialog.pack();

    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(final WindowEvent e) {
        instances.remove(dialog);
        instances.add(dialog);
      }

      @Override
      public void windowClosed(final WindowEvent e) {
        if (taFrame.getUiContext() != null && !taFrame.getUiContext().isShutDown()) {
          taFrame.getUiContext().removeShutdownWindow(dialog);
        }
      }
    });

    dialog.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        final Dimension size = dialog.getSize();
        size.width = Math.min(size.width, taFrame.getWidth() - 50);
        size.height = Math.min(size.height, taFrame.getHeight() - 50);
        dialog.setSize(size);
      }
    });

    // close when hitting the escape key
    SwingComponents.addEscapeKeyListener(dialog, () -> {
      dialog.setVisible(false);
      dialog.dispose();
    });
    dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    if (lastPosition == null) {
      dialog.setLocationRelativeTo(taFrame);
    } else {
      dialog.setLocation(lastPosition);
    }
    dialog.setVisible(true);
    taFrame.getUiContext().addShutdownWindow(dialog);
  }

  public static void addAttackers(final Territory t) {
    if (instances.isEmpty()) {
      return;
    }
    final OddsCalculatorPanel currentPanel = instances.get(instances.size() - 1).panel;
    currentPanel.addAttackingUnits(t.getUnitCollection().getMatches(Matches.unitIsOwnedBy(currentPanel.getAttacker())));
  }

  public static void addDefenders(final Territory t) {
    if (instances.isEmpty()) {
      return;
    }
    final OddsCalculatorDialog currentDialog = instances.get(instances.size() - 1);
    currentDialog.panel
        .addDefendingUnits(
            t.getUnitCollection().getMatches(Matches.alliedUnit(currentDialog.panel.getDefender(), t.getData())));
    currentDialog.pack();
  }

  @Override
  public void dispose() {
    instances.remove(this);
    lastPosition = new Point(getLocation());
    panel.shutdown();
    super.dispose();
  }

  @Override
  public void setVisible(final boolean vis) {
    super.setVisible(vis);
    panel.selectCalculateButton();
  }
}
