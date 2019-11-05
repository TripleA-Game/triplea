package games.strategy.triplea.ui;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameDataEvent;
import games.strategy.engine.data.GameStep;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.events.GameDataChangeListener;
import games.strategy.engine.framework.LocalPlayers;
import games.strategy.engine.player.IPlayerBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.PlayerAttachment;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.data.PlaceableUnits;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.util.UnitCategory;
import games.strategy.triplea.util.UnitSeparator;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Predicate;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import org.triplea.java.collections.CollectionUtils;
import org.triplea.swing.CollapsiblePanel;

class PlacePanel extends AbstractMovePanel implements GameDataChangeListener {
  private static final long serialVersionUID = -4411301492537704785L;
  private final JLabel actionLabel = new JLabel();
  private PlaceData placeData;

  private final CollapsiblePanel detachedCollapsiblePanel;
  private final SimpleUnitPanel unitsToPlacePanel;

  private PlayerId lastPlayer;
  private boolean postProductionStep;

  private final MapSelectionListener placeMapSelectionListener =
      new DefaultMapSelectionListener() {
        @Override
        public void territorySelected(final Territory territory, final MouseDetails e) {
          if (!isActive() || (e.getButton() != MouseEvent.BUTTON1)) {
            return;
          }
          final int[] maxUnits = new int[1];
          final Collection<Unit> units = getUnitsToPlace(territory, maxUnits);
          if (units.isEmpty()) {
            return;
          }
          final UnitChooser chooser =
              new UnitChooser(units, Collections.emptyMap(), false, getMap().getUiContext());
          final String messageText = "Place units in " + territory.getName();
          if (maxUnits[0] >= 0) {
            chooser.setMaxAndShowMaxButton(maxUnits[0]);
          }
          final Dimension screenResolution = Toolkit.getDefaultToolkit().getScreenSize();
          final int availHeight = screenResolution.height - 120;
          final int availWidth = screenResolution.width - 40;
          final JScrollPane scroll = new JScrollPane(chooser);
          scroll.setBorder(BorderFactory.createEmptyBorder());
          scroll.setPreferredSize(
              new Dimension(
                  (scroll.getPreferredSize().width > availWidth
                      ? availWidth
                      : (scroll.getPreferredSize().width
                          + (scroll.getPreferredSize().height > availHeight ? 20 : 0))),
                  (scroll.getPreferredSize().height > availHeight
                      ? availHeight
                      : (scroll.getPreferredSize().height
                          + (scroll.getPreferredSize().width > availWidth ? 26 : 0)))));
          final int option =
              JOptionPane.showOptionDialog(
                  getTopLevelAncestor(),
                  scroll,
                  messageText,
                  JOptionPane.OK_CANCEL_OPTION,
                  JOptionPane.PLAIN_MESSAGE,
                  null,
                  null,
                  null);
          if (option == JOptionPane.OK_OPTION) {
            final Collection<Unit> choosen = chooser.getSelected();
            placeData = new PlaceData(choosen, territory);
            release();
          }
        }
      };

  PlacePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map, frame);
    undoableMovesPanel = new UndoablePlacementsPanel(this);
    unitsToPlacePanel =
        new SimpleUnitPanel(
            map.getUiContext(), SimpleUnitPanel.Style.SMALL_ICONS_WRAPPED_WITH_LABEL_WHEN_EMPTY);
    detachedCollapsiblePanel = new CollapsiblePanel(unitsToPlacePanel, "Units to Place");
    data.addGameDataEventListener(GameDataEvent.GAME_STEP_CHANGED, this::updateStep);
  }

  public JComponent getDetachedUnitsToPlacePanel() {
    return detachedCollapsiblePanel;
  }

  private void updateStep() {
    final Collection<UnitCategory> unitsToPlace;
    final boolean showUnitsToPlace;
    final GameData data = getData();
    data.acquireReadLock();
    try {
      final GameStep step = data.getSequence().getStep();
      if (step == null) {
        return;
      }
      // Note: This doesn't use getCurrentPlayer() as that may not be updated yet.
      final PlayerId player = step.getPlayerId();
      if (player == null) {
        return;
      }
      final boolean isNewPlayerTurn = !player.equals(lastPlayer);
      if (isNewPlayerTurn) {
        postProductionStep = false;
      }
      final Collection<Unit> playerUnits = player.getUnits();
      // If we're past the production step (even if player didn't produce anything) or
      // there are units that are available to place, show the panel (set unitsToPlace).
      showUnitsToPlace = (postProductionStep || !playerUnits.isEmpty());
      unitsToPlace = showUnitsToPlace ? UnitSeparator.categorize(playerUnits) : null;
      if (GameStep.isPurchaseOrBidStep(step.getName())) {
        postProductionStep = true;
      }
      lastPlayer = player;
      // During the place step, listen for changes to update the panel.
      if (GameStep.isPlaceStep(step.getName())) {
        data.addDataChangeListener(this);
      } else {
        data.removeDataChangeListener(this);
      }
    } finally {
      data.releaseReadLock();
    }

    SwingUtilities.invokeLater(
        () -> {
          if (showUnitsToPlace) {
            unitsToPlacePanel.setUnitsFromCategories(unitsToPlace);
            detachedCollapsiblePanel.setVisible(true);
            unitsToPlacePanel.revalidate();
            unitsToPlacePanel.repaint();
          } else {
            detachedCollapsiblePanel.setVisible(false);
            unitsToPlacePanel.removeAll();
          }
        });
  }

  @Override
  public void gameDataChanged(final Change change) {
    final Collection<UnitCategory> unitsToPlace;
    final GameData data = getData();
    data.acquireReadLock();
    try {
      final PlayerId player = data.getSequence().getStep().getPlayerId();
      unitsToPlace = UnitSeparator.categorize(player.getUnits());
    } finally {
      data.releaseReadLock();
    }

    SwingUtilities.invokeLater(
        () -> {
          unitsToPlacePanel.setUnitsFromCategories(unitsToPlace);
          unitsToPlacePanel.revalidate();
          unitsToPlacePanel.repaint();
        });
  }

  @Override
  Component getUnitScrollerPanel(
      final LocalPlayers localPlayers, final Runnable toggleFlagsAction) {
    return new JPanel();
  }

  @Override
  public void display(final PlayerId id) {
    super.display(id, " place");
  }

  private void refreshActionLabelText(final boolean bid) {
    SwingUtilities.invokeLater(
        () ->
            actionLabel.setText(getCurrentPlayer().getName() + " place" + (bid ? " for bid" : "")));
  }

  PlaceData waitForPlace(final boolean bid, final IPlayerBridge playerBridge) {
    setUp(playerBridge);
    // workaround: meant to be in setUpSpecific, but it requires a variable
    refreshActionLabelText(bid);
    waitForRelease();
    cleanUp();
    return placeData;
  }

  private boolean canProduceFightersOnCarriers() {
    return Properties.getProduceFightersOnCarriers(getData());
  }

  private boolean canProduceNewFightersOnOldCarriers() {
    return Properties.getProduceNewFightersOnOldCarriers(getData());
  }

  private boolean isLhtrCarrierProductionRules() {
    return Properties.getLhtrCarrierProductionRules(getData());
  }

  private Collection<Unit> getUnitsToPlace(final Territory territory, final int[] maxUnits) {
    getData().acquireReadLock();
    try {
      // not our territory
      if (!territory.isWater() && !territory.getOwner().equals(getCurrentPlayer())) {
        if (GameStepPropertiesHelper.isBid(getData())) {
          final PlayerAttachment pa = PlayerAttachment.get(territory.getOwner());
          if ((pa == null
                  || pa.getGiveUnitControl() == null
                  || !pa.getGiveUnitControl().contains(getCurrentPlayer()))
              && !territory
                  .getUnitCollection()
                  .anyMatch(Matches.unitIsOwnedBy(getCurrentPlayer()))) {
            return Collections.emptyList();
          }
        } else {
          return Collections.emptyList();
        }
      }
      // get the units that can be placed on this territory.
      Collection<Unit> units = getCurrentPlayer().getUnits();
      if (territory.isWater()) {
        if (!(canProduceFightersOnCarriers()
            || canProduceNewFightersOnOldCarriers()
            || isLhtrCarrierProductionRules()
            || GameStepPropertiesHelper.isBid(getData()))) {
          units = CollectionUtils.getMatches(units, Matches.unitIsSea());
        } else {
          final Predicate<Unit> unitIsSeaOrCanLandOnCarrier =
              Matches.unitIsSea().or(Matches.unitCanLandOnCarrier());
          units = CollectionUtils.getMatches(units, unitIsSeaOrCanLandOnCarrier);
        }
      } else {
        units = CollectionUtils.getMatches(units, Matches.unitIsNotSea());
      }
      if (units.isEmpty()) {
        return Collections.emptyList();
      }
      final IAbstractPlaceDelegate placeDel =
          (IAbstractPlaceDelegate) getPlayerBridge().getRemoteDelegate();
      final PlaceableUnits production = placeDel.getPlaceableUnits(units, territory);
      if (production.isError()) {
        JOptionPane.showMessageDialog(
            getTopLevelAncestor(),
            production.getErrorMessage(),
            "No units",
            JOptionPane.INFORMATION_MESSAGE);
        return Collections.emptyList();
      }
      maxUnits[0] = production.getMaxUnits();
      return production.getUnits();
    } finally {
      getData().releaseReadLock();
    }
  }

  @Override
  public String toString() {
    return "PlacePanel";
  }

  @Override
  protected final void cancelMoveAction() {
    getMap().showMouseCursor();
    getMap().setMouseShadowUnits(null);
  }

  @Override
  protected final void undoMoveSpecific() {}

  @Override
  protected final void cleanUpSpecific() {
    getMap().removeMapSelectionListener(placeMapSelectionListener);
  }

  @Override
  protected final void setUpSpecific() {
    getMap().addMapSelectionListener(placeMapSelectionListener);
  }

  @Override
  protected boolean doneMoveAction() {
    if (getCurrentPlayer().getUnitCollection().size() > 0) {
      final int option =
          JOptionPane.showConfirmDialog(
              getTopLevelAncestor(),
              "You have not placed all your units yet.  Are you sure you want to end your turn?",
              "TripleA",
              JOptionPane.YES_NO_OPTION,
              JOptionPane.PLAIN_MESSAGE);
      if (option != JOptionPane.YES_OPTION) {
        return false;
      }
    }
    placeData = null;
    return true;
  }

  @Override
  protected boolean setCancelButton() {
    return false;
  }
}
