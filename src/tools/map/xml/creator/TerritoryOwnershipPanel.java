package tools.map.xml.creator;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.util.Map;
import java.util.Optional;

import javax.swing.JOptionPane;

import games.strategy.common.swing.SwingAction;
import games.strategy.ui.Util;


public class TerritoryOwnershipPanel extends ImageScrollPanePanel {

  private final String[] players = MapXmlHelper.getPlayersListInclNeutral();
  private String lastChosenPlayer = MapXmlHelper.getPlayerNames().iterator().next();

  private TerritoryOwnershipPanel() {}

  public static void layout(final MapXmlCreator mapXmlCreator) {
    setMapXmlCreator(mapXmlCreator);
    final TerritoryOwnershipPanel panel = new TerritoryOwnershipPanel();
    panel.layout(mapXmlCreator.getStepActionPanel());
    mapXmlCreator.setAutoFillAction(SwingAction.of(e -> {
      panel.paintPreparation(null);
      panel.repaint();
    }));
  }

  @Override
  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {

    String ownership = MapXmlHelper.getTerritoryOwnershipsMap().get(centerName);
    if (ownership == null) {
      ownership = MapXmlHelper.playerNeutral;
    }
    final Rectangle2D prodStringBounds = fontMetrics.getStringBounds(ownership, g);
    final Rectangle2D centerStringBounds = fontMetrics.getStringBounds(centerName, g);
    final double wDiff = (centerStringBounds.getWidth() - prodStringBounds.getWidth()) / 2;
    g.setColor(Color.yellow);
    g.fillRect(Math.max(0, x_text_start - 2 + (int) wDiff), item.y + 6, (int) prodStringBounds.getWidth() + 4,
        (int) prodStringBounds.getHeight());
    g.setColor(Color.red);
    g.drawString(ownership, Math.max(0, x_text_start + (int) wDiff), item.y + 17);
    g.setColor(Color.red);
  }

  @Override
  protected void paintPreparation(final Map<String, Point> centers) {}

  @Override
  protected void paintOwnSpecifics(final Graphics g, final Map<String, Point> centers) {}

  @Override
  protected void mouseClickedOnImage(final Map<String, Point> centers, final MouseEvent e) {
    final Optional<String> territoryNameOptional = Util.findTerritoryName(e.getPoint(), polygons);
    if (!territoryNameOptional.isPresent()) {
      return;
    }
    final String territoryName = territoryNameOptional.get();

    final String currValue = MapXmlHelper.getTerritoryOwnershipsMap().get(territoryName);
    final String inputText = (String) JOptionPane.showInputDialog(null,
        "Which player should be the initial owner of territory '" + territoryName + "'?",
        "Choose Owner of " + territoryName,
        JOptionPane.QUESTION_MESSAGE, null, players, // Array of choices
        (currValue != null ? currValue : lastChosenPlayer)); // Initial choice
    final boolean inputIsNeutralPlayer = MapXmlHelper.playerNeutral.equals(inputText);
    if (inputText == null || inputText.equals(currValue) || inputIsNeutralPlayer && currValue == null) {
      return;
    }
    if (inputIsNeutralPlayer) {
      MapXmlHelper.getTerritoryOwnershipsMap().remove(territoryName);
    } else {
      MapXmlHelper.putTerritoyOwnerships(territoryName, inputText);
    }
    lastChosenPlayer = inputText;

    repaint();
  }

}
