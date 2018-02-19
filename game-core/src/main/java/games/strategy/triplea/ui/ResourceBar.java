package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.border.EtchedBorder;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.triplea.Constants;

/**
 * Panel used to display the current players resources.
 */
public class ResourceBar extends AbstractStatPanel implements GameDataChangeListener {
  private static final long serialVersionUID = -7713792841831042952L;

  private final UiContext uiContext;
  private final List<ResourceStat> resourceStats = new ArrayList<>();
  private final List<JLabel> labels = new ArrayList<>();

  public ResourceBar(final GameData data, final UiContext uiContext) {
    super(data);
    this.uiContext = uiContext;
    setResources();
    initLayout();
    gameData.addDataChangeListener(this);
  }

  @Override
  protected void initLayout() {
    setBorder(new EtchedBorder(EtchedBorder.RAISED));
    labels.stream().forEachOrdered(this::add);
  }

  @Override
  public void setGameData(final GameData data) {
    gameData.removeDataChangeListener(this);
    gameData = data;
    gameData.addDataChangeListener(this);
  }

  private void setResources() {
    for (final Resource resource : gameData.getResourceList().getResources()) {
      if (resource.getName().equals(Constants.VPS)) {
        continue;
      }
      resourceStats.add(new ResourceStat(resource));
      final JLabel label = new JLabel();
      label.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 20));
      labels.add(label);
    }
  }

  @Override
  public void gameDataChanged(final Change change) {
    gameData.acquireReadLock();
    try {
      final PlayerID player = gameData.getSequence().getStep().getPlayerId();
      if (player != null) {
        for (int i = 0; i < resourceStats.size(); i++) {
          final ResourceStat resourceStat = resourceStats.get(i);
          final Resource resource = resourceStat.resource;
          final JLabel label = labels.get(i);
          final String quantity = resourceStat.getFormatter().format(resourceStat.getValue(player, gameData));
          label.setVisible(resource.isDisplayedFor(player));
          try {
            label.setIcon(uiContext.getResourceImageFactory().getIcon(resource, true));
            label.setText(quantity);
            label.setToolTipText(resourceStat.getName());
          } catch (final IllegalStateException e) {
            label.setText(resourceStat.getName() + " " + quantity);
          }
        }
      }
    } finally {
      gameData.releaseReadLock();
    }
  }

}
