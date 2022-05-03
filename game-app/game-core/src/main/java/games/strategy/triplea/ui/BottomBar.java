package games.strategy.triplea.ui;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceCollection;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.util.List;
import java.util.Optional;
import javax.annotation.Nullable;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.border.EtchedBorder;
import org.triplea.java.collections.IntegerMap;
import org.triplea.swing.SwingComponents;
import org.triplea.swing.jpanel.GridBagConstraintsAnchor;
import org.triplea.swing.jpanel.GridBagConstraintsBuilder;
import org.triplea.swing.jpanel.GridBagConstraintsFill;

public class BottomBar extends JPanel {
  private final UiContext uiContext;
  private final GameData data;

  private final ResourceBar resourceBar;
  private final JPanel territoryInfo = new JPanel();

  private final JLabel statusMessage = new JLabel();

  private final JLabel playerLabel = new JLabel("xxxxxx");
  private final JLabel stepLabel = new JLabel("xxxxxx");
  private final JLabel roundLabel = new JLabel("xxxxxx");

  public BottomBar(final UiContext uiContext, final GameData data, final boolean usingDiceServer) {
    this.uiContext = uiContext;
    this.data = data;
    setLayout(new BorderLayout());

    resourceBar = new ResourceBar(data, uiContext);

    add(createCenterPanel(), BorderLayout.CENTER);
    add(createStepPanel(usingDiceServer), BorderLayout.EAST);
  }

  private JPanel createCenterPanel() {
    final JPanel centerPanel = new JPanel();
    centerPanel.setLayout(new GridBagLayout());
    centerPanel.setBorder(BorderFactory.createEmptyBorder());
    final var gridBuilder =
        new GridBagConstraintsBuilder(0, 0).weightY(1).fill(GridBagConstraintsFill.BOTH);

    centerPanel.add(
        resourceBar, gridBuilder.weightX(0).anchor(GridBagConstraintsAnchor.WEST).build());

    territoryInfo.setLayout(new BoxLayout(territoryInfo, BoxLayout.LINE_AXIS));
    territoryInfo.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    territoryInfo.setPreferredSize(new Dimension(0, 0));
    centerPanel.add(
        territoryInfo,
        gridBuilder.gridX(1).weightX(1).anchor(GridBagConstraintsAnchor.SOUTHWEST).build());

    statusMessage.setPreferredSize(new Dimension(0, 0));
    statusMessage.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    centerPanel.add(
        statusMessage, gridBuilder.gridX(3).anchor(GridBagConstraintsAnchor.EAST).build());
    return centerPanel;
  }

  private JPanel createStepPanel(boolean usingDiceServer) {
    final JPanel stepPanel = new JPanel();
    stepPanel.setLayout(new GridBagLayout());
    final var gridBuilder = new GridBagConstraintsBuilder(0, 0).fill(GridBagConstraintsFill.BOTH);
    stepPanel.add(playerLabel, gridBuilder.gridX(0).build());
    stepPanel.add(stepLabel, gridBuilder.gridX(1).build());
    stepPanel.add(roundLabel, gridBuilder.gridX(2).build());
    if (usingDiceServer) {
      final JLabel diceServerLabel = new JLabel("Dice Server On");
      diceServerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
      stepPanel.add(diceServerLabel, gridBuilder.gridX(3).build());
    }
    stepLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    roundLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    playerLabel.setBorder(new EtchedBorder(EtchedBorder.RAISED));
    stepLabel.setHorizontalTextPosition(SwingConstants.LEADING);
    return stepPanel;
  }

  public void setStatus(final String msg, final Optional<Image> image) {
    statusMessage.setText(msg);

    if (!msg.isEmpty() && image.isPresent()) {
      statusMessage.setIcon(new ImageIcon(image.get()));
    } else {
      statusMessage.setIcon(null);
    }
  }

  public void setTerritory(final @Nullable Territory territory) {
    territoryInfo.removeAll();
    territoryInfo.add(Box.createHorizontalGlue());

    if (territory == null) {
      SwingComponents.redraw(territoryInfo);
      return;
    }

    // Display territory effects, territory name, and resources
    final StringBuilder territoryEffectText = new StringBuilder();
    final TerritoryAttachment ta = TerritoryAttachment.get(territory);
    if (ta != null) {
      final List<TerritoryEffect> territoryEffects = ta.getTerritoryEffect();
      for (final TerritoryEffect territoryEffect : territoryEffects) {
        try {
          final JLabel territoryEffectLabel = new JLabel();
          territoryEffectLabel.setToolTipText(territoryEffect.getName());
          territoryEffectLabel.setIcon(
              uiContext.getTerritoryEffectImageFactory().getIcon(territoryEffect.getName()));
          territoryEffectLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 10));
          territoryInfo.add(territoryEffectLabel);
        } catch (final IllegalStateException e) {
          territoryEffectText.append(territoryEffect.getName()).append(", ");
        }
      }
    }

    final JLabel nameLabel = new JLabel(territory.getName());
    nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD));
    territoryInfo.add(nameLabel);

    if (territoryEffectText.length() > 0) {
      territoryEffectText.setLength(territoryEffectText.length() - 2);
      final JLabel territoryEffectTextLabel = new JLabel("(" + territoryEffectText + ")");
      territoryInfo.add(territoryEffectTextLabel);
    }

    if (ta != null) {
      final IntegerMap<Resource> resources = new IntegerMap<>();
      final int production = ta.getProduction();
      if (production > 0) {
        resources.add(new Resource(Constants.PUS, data), production);
      }
      final ResourceCollection resourceCollection = ta.getResources();
      if (resourceCollection != null) {
        resources.add(resourceCollection.getResourcesCopy());
      }
      for (final Resource resource : resources.keySet()) {
        final JLabel resourceLabel =
            uiContext.getResourceImageFactory().getLabel(resource, resources);
        resourceLabel.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 0));
        territoryInfo.add(resourceLabel);
      }
    }

    final var unitsPanel = new SimpleUnitPanel(uiContext, SimpleUnitPanel.Style.SMALL_ICONS_ROW);
    unitsPanel.setScaleFactor(0.5);
    unitsPanel.setUnitsFromCategories(UnitSeparator.categorize(territory.getUnits()));
    unitsPanel.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 0));
    unitsPanel.setPreferredSize(
        new Dimension(
            unitsPanel.getPreferredSize().width,
            getHeight() - getBorderVerticalSpace(territoryInfo)));
    territoryInfo.add(unitsPanel);
    territoryInfo.add(Box.createHorizontalGlue());

    SwingComponents.redraw(territoryInfo);
  }

  private int getBorderVerticalSpace(JComponent c) {
    Insets insets = c.getBorder().getBorderInsets(c);
    return insets.top + insets.bottom;
  }

  public void gameDataChanged() {
    resourceBar.gameDataChanged(null);
  }

  public void setRoundIcon(ImageIcon icon) {
    roundLabel.setIcon(icon);
  }

  public void setStepInfo(
      int roundNumber, String stepName, GamePlayer player, boolean isRemotePlayer) {
    roundLabel.setText("Round:" + roundNumber + " ");
    stepLabel.setText(stepName);
    if (player != null) {
      this.playerLabel.setText((isRemotePlayer ? "REMOTE: " : "") + player.getName());
    }
  }
}
