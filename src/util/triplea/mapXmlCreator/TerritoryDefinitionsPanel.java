package util.triplea.mapXmlCreator;

import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import com.google.common.collect.Maps;

import util.triplea.mapXmlCreator.TerritoryDefinitionDialog.DEFINITION;


class TerritoryDefinitionsPanel extends ImageScrollPanePanel {

  private TerritoryDefinitionsPanel() {}

  public static void layout(final MapXmlCreator mapXMLCreator) {
    ImageScrollPanePanel.mapXMLCreator = mapXMLCreator;
    new TerritoryDefinitionsPanel().layout(mapXMLCreator.getStepActionPanel());
  }

  @Override
  protected void paintCenterSpecifics(final Graphics g, final String centerName, final FontMetrics fontMetrics,
      final Point item, final int x_text_start) {
    final HashMap<DEFINITION, Boolean> territoryDefinition = MapXmlHelper.getTerritoryDefintionsMap().get(centerName);
    if (territoryDefinition != null) {
      final int y_value = item.y + 10;
      short definition_count = 0;
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.BOLD));
      final FontMetrics fm = g.getFontMetrics();
      int h = fm.getAscent();
      final int oneCharacterWidthSpace = 17;
      for (final Entry<DEFINITION, Boolean> definitionEntry : territoryDefinition.entrySet()) {
        if (definitionEntry.getValue()) {
          final int x_value = x_text_start + oneCharacterWidthSpace * definition_count;
          int w;
          String character = null;
          switch (definitionEntry.getKey()) {
            case IS_WATER:
              g.setColor(Color.blue);
              character = "W";
              break;
            case IS_VICTORY_CITY:
              g.setColor(Color.yellow);
              character = "V";
              break;
            case IMPASSABLE:
              g.setColor(Color.gray);
              character = "I";
              break;
            case IS_CAPITAL:
              g.setColor(Color.green);
              break;
            default:
              throw new IllegalStateException("No valid value for " + DEFINITION.class);
          }
          g.fillOval(x_value, y_value, 16, 16);
          g.setColor(Color.red);
          w = fm.stringWidth(character);
          h = fm.getAscent();
          g.drawString(character, x_value + 8 - (w / 2), y_value + 8 + (h / 2));
        }
        ++definition_count;
      }
      g.setColor(Color.red);
      g.setFont(g.getFontMetrics().getFont().deriveFont(Font.PLAIN));
    }
  }

  @Override
  protected void paintPreparation(final Map<String, Point> centers) {
    if (!MapXmlCreator.waterFilterString.isEmpty() && MapXmlHelper.getTerritoryDefintionsMap().isEmpty()) {
      for (final String centerName : centers.keySet()) {
        final HashMap<DEFINITION, Boolean> territoyDefintion =
            Maps.newHashMap();
        if (centerName.startsWith(MapXmlCreator.waterFilterString)) {
          territoyDefintion.put(DEFINITION.IS_WATER, true);
        }
        MapXmlHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    } else {
      for (final String centerName : centers.keySet()) {
        final HashMap<DEFINITION, Boolean> territoyDefintion =
            Maps.newHashMap();
        MapXmlHelper.putTerritoryDefintions(centerName, territoyDefintion);
      }
    }
  }

  @Override
  protected void paintOwnSpecifics(Graphics g, Map<String, Point> centers) {}

  @Override
  protected void mouseClickedOnImage(final Map<String, Point> centers, final MouseEvent e) {
    final Point point = e.getPoint();
    final String territoryName = findTerritoryName(point, polygons);
    if (SwingUtilities.isRightMouseButton(e)) {
      String territoryNameNew =
          JOptionPane.showInputDialog(getImagePanel(), "Enter the territory name:", territoryName);
      if (territoryNameNew == null || territoryNameNew.trim().length() == 0)
        return;
      if (!territoryName.equals(territoryNameNew) && centers.containsKey(territoryNameNew)
          && JOptionPane.showConfirmDialog(getImagePanel(),
              "Another center exists with the same name. Are you sure you want to replace it with this one?") != 0)
        return;
      centers.put(territoryNameNew, centers.get(territoryName));
    } else {
      SwingUtilities.invokeLater(new Runnable() {
        @Override
        public void run() {
          HashMap<DEFINITION, Boolean> territoyDefintions = MapXmlHelper.getTerritoryDefintionsMap().get(territoryName);
          if (territoyDefintions == null)
            territoyDefintions = Maps.newHashMap();
          new TerritoryDefinitionDialog(mapXMLCreator, territoryName, territoyDefintions);
          getImagePanel().repaint();
        }
      });
    }
  }
}
