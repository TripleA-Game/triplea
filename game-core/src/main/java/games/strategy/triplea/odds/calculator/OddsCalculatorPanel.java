package games.strategy.triplea.odds.calculator;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;

import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.framework.ui.background.WaitDialog;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.BattleCalculator;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TerritoryEffectHelper;
import games.strategy.triplea.delegate.UnitBattleComparator;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.triplea.ui.UiContext;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.ui.IntTextField;
import games.strategy.ui.SwingComponents;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import lombok.extern.java.Log;

@Log
class OddsCalculatorPanel extends JPanel {
  private static final long serialVersionUID = -3559687618320469183L;
  private static final String NO_EFFECTS = "*None*";
  private final JLabel attackerWin = new JLabel();
  private final JLabel defenderWin = new JLabel();
  private final JLabel draw = new JLabel();
  private final JLabel defenderLeft = new JLabel();
  private final JLabel attackerLeft = new JLabel();
  private final JLabel defenderLeftWhenDefenderWon = new JLabel();
  private final JLabel attackerLeftWhenAttackerWon = new JLabel();
  private final JLabel averageChangeInTuv = new JLabel();
  private final JLabel roundsAverage = new JLabel();
  private final JLabel count = new JLabel();
  private final JLabel time = new JLabel();
  private final IntTextField numRuns = new IntTextField();
  private final IntTextField retreatAfterXRounds = new IntTextField();
  private final IntTextField retreatAfterXUnitsLeft = new IntTextField();
  private final JButton calculateButton = new JButton("Pls Wait, Copying Data...");
  private final JCheckBox keepOneAttackingLandUnitCheckBox = new JCheckBox("One attacking land must live");
  private final JCheckBox amphibiousCheckBox = new JCheckBox("Battle is Amphibious");
  private final JCheckBox landBattleCheckBox = new JCheckBox("Land Battle");
  private final JCheckBox retreatWhenOnlyAirLeftCheckBox = new JCheckBox("Retreat when only air left");
  private final UiContext uiContext;
  private final GameData data;
  private final IOddsCalculator calculator;
  private final PlayerUnitsPanel attackingUnitsPanel;
  private final PlayerUnitsPanel defendingUnitsPanel;
  private final JComboBox<PlayerID> attackerCombo;
  private final JComboBox<PlayerID> defenderCombo;
  private final JComboBox<PlayerID> swapSidesCombo;
  private final JLabel attackerUnitsTotalNumber = new JLabel();
  private final JLabel defenderUnitsTotalNumber = new JLabel();
  private final JLabel attackerUnitsTotalTuv = new JLabel();
  private final JLabel defenderUnitsTotalTuv = new JLabel();
  private final JLabel attackerUnitsTotalHitpoints = new JLabel();
  private final JLabel defenderUnitsTotalHitpoints = new JLabel();
  private final JLabel attackerUnitsTotalPower = new JLabel();
  private final JLabel defenderUnitsTotalPower = new JLabel();
  private String attackerOrderOfLosses = null;
  private String defenderOrderOfLosses = null;
  private final Territory location;
  private final JList<String> territoryEffectsJList;

  OddsCalculatorPanel(final GameData data, final UiContext uiContext, final Territory location,
      final Window parent) {
    this.data = data;
    this.uiContext = uiContext;
    this.location = location;
    calculateButton.setEnabled(false);
    data.acquireReadLock();
    try {
      final Collection<PlayerID> playerList = new ArrayList<>(data.getPlayerList().getPlayers());
      if (doesPlayerHaveUnitsOnMap(PlayerID.NULL_PLAYERID, data)) {
        playerList.add(PlayerID.NULL_PLAYERID);
      }
      attackerCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      defenderCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      swapSidesCombo = new JComboBox<>(SwingComponents.newComboBoxModel(playerList));
      final Map<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
      if (allTerritoryEffects == null || allTerritoryEffects.isEmpty()) {
        territoryEffectsJList = null;
      } else {
        final List<String> effectNames = new ArrayList<>();
        effectNames.add(NO_EFFECTS);
        effectNames.addAll(allTerritoryEffects.keySet());
        territoryEffectsJList = new JList<>(SwingComponents.newListModel(effectNames));
        territoryEffectsJList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        territoryEffectsJList.setLayoutOrientation(JList.VERTICAL);
        // equal to the amount of space left (number of remaining items on the right)
        territoryEffectsJList.setVisibleRowCount(4);
        if (location != null) {
          final Collection<TerritoryEffect> currentEffects = TerritoryEffectHelper.getEffects(location);
          if (!currentEffects.isEmpty()) {
            final int[] selectedIndexes = new int[currentEffects.size()];
            int currentIndex = 0;
            for (final TerritoryEffect te : currentEffects) {
              selectedIndexes[currentIndex] = effectNames.indexOf(te.getName());
              currentIndex++;
            }
            territoryEffectsJList.setSelectedIndices(selectedIndexes);
          }
        }
      }
    } finally {
      data.releaseReadLock();
    }
    defenderCombo.setRenderer(new PlayerRenderer());
    attackerCombo.setRenderer(new PlayerRenderer());
    swapSidesCombo.setRenderer(new PlayerRenderer());
    defendingUnitsPanel = new PlayerUnitsPanel(data, uiContext, true);
    attackingUnitsPanel = new PlayerUnitsPanel(data, uiContext, false);
    numRuns.setColumns(4);
    numRuns.setMin(1);
    numRuns.setMax(20000);

    final int simulationCount = Properties.getLowLuck(data)
        ? ClientSetting.BATTLE_CALC_SIMULATION_COUNT_LOW_LUCK.intValue()
        : ClientSetting.BATTLE_CALC_SIMULATION_COUNT_DICE.intValue();
    numRuns.setValue(simulationCount);
    retreatAfterXRounds.setColumns(4);
    retreatAfterXRounds.setMin(-1);
    retreatAfterXRounds.setMax(1000);
    retreatAfterXRounds.setValue(-1);
    retreatAfterXRounds.setToolTipText("-1 means never.");
    retreatAfterXUnitsLeft.setColumns(4);
    retreatAfterXUnitsLeft.setMin(-1);
    retreatAfterXUnitsLeft.setMax(1000);
    retreatAfterXUnitsLeft.setValue(-1);
    retreatAfterXUnitsLeft.setToolTipText("-1 means never. If positive and 'retreat when only air left' is also "
        + "selected, then we will retreat when X of non-air units is left.");
    setResultsToBlank();
    defenderLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include "
        + "Bombarding sea units for land battles.");
    attackerLeft.setToolTipText("Units Left does not include AA guns and other infrastructure, and does not include "
        + "Bombarding sea units for land battles.");
    defenderLeftWhenDefenderWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    attackerLeftWhenAttackerWon.setToolTipText("Units Left does not include AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    averageChangeInTuv.setToolTipText("TUV Swing does not include captured AA guns and other infrastructure, and "
        + "does not include Bombarding sea units for land battles.");
    retreatWhenOnlyAirLeftCheckBox.setToolTipText("We retreat if only air is left, and if 'retreat when x units "
        + "left' is positive we will retreat when x of non-air is left too.");
    attackerUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not "
        + "include Bombarding sea units for land battles.");
    defenderUnitsTotalNumber.setToolTipText("Totals do not include AA guns and other infrastructure, and does not "
        + "include Bombarding sea units for land battles.");
    setLayout(new BorderLayout());

    final JPanel main = new JPanel();
    main.setBorder(BorderFactory.createEmptyBorder(10, 0, 10, 0));
    add(main, BorderLayout.CENTER);
    main.setLayout(new BorderLayout());

    final JPanel attackAndDefend = new JPanel();
    attackAndDefend.setLayout(new GridBagLayout());
    final int gap = 20;
    int row0 = 0;
    attackAndDefend.add(new JLabel("Attacker: "), new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
    attackAndDefend.add(attackerCombo, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
    attackAndDefend.add(new JLabel("Defender: "), new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, gap, 0), 0, 0));
    attackAndDefend.add(defenderCombo, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, gap / 2, gap), 0, 0));
    row0++;
    attackAndDefend.add(attackerUnitsTotalNumber, new GridBagConstraints(0, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
    attackAndDefend.add(attackerUnitsTotalTuv, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
    attackAndDefend.add(defenderUnitsTotalNumber, new GridBagConstraints(2, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap, 0, 0), 0, 0));
    attackAndDefend.add(defenderUnitsTotalTuv, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, 0, gap * 2), 0, 0));
    row0++;
    attackAndDefend.add(attackerUnitsTotalHitpoints, new GridBagConstraints(0, row0, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
    attackAndDefend.add(attackerUnitsTotalPower, new GridBagConstraints(1, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
    attackAndDefend.add(defenderUnitsTotalHitpoints, new GridBagConstraints(2, row0, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, gap, gap / 2, 0), 0, 0));
    attackAndDefend.add(defenderUnitsTotalPower, new GridBagConstraints(3, row0, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, gap / 2, gap / 2, gap * 2), 0, 0));
    row0++;
    final JScrollPane attackerScroll = new JScrollPane(attackingUnitsPanel);
    attackerScroll.setBorder(null);
    attackerScroll.getViewport().setBorder(null);
    final JScrollPane defenderScroll = new JScrollPane(defendingUnitsPanel);
    defenderScroll.setBorder(null);
    defenderScroll.getViewport().setBorder(null);
    attackAndDefend.add(attackerScroll, new GridBagConstraints(0, row0, 2, 1, 1, 1, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
    attackAndDefend.add(defenderScroll, new GridBagConstraints(2, row0, 2, 1, 1, 1, GridBagConstraints.NORTH,
        GridBagConstraints.BOTH, new Insets(10, gap, gap, gap), 0, 0));
    main.add(attackAndDefend, BorderLayout.CENTER);

    final JPanel resultsText = new JPanel();
    resultsText.setLayout(new GridBagLayout());
    int row1 = 0;
    resultsText.add(new JLabel("Attacker Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Draw:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Defender Wins:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Ave. Defender Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Units Left If Def Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Ave. Attacker Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Units Left If Att Won:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Average TUV Swing:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(6, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Average Rounds:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Simulation Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(15, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Time:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));
    resultsText.add(calculateButton, new GridBagConstraints(0, row1++, 2, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(20, 60, 0, 100), 0, 0));
    final JButton clearButton = new JButton("Clear");
    resultsText.add(clearButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(6, 60, 0, 0), 0, 0));
    resultsText.add(new JLabel("Run Count:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.NONE, new Insets(20, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Retreat After Round:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    resultsText.add(new JLabel("Retreat When X Units Left:"), new GridBagConstraints(0, row1++, 1, 1, 0, 0,
        GridBagConstraints.EAST, GridBagConstraints.NONE, new Insets(10, 0, 0, 0), 0, 0));
    int row2 = 0;
    resultsText.add(attackerWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(draw, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(defenderWin, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(defenderLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(defenderLeftWhenDefenderWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(attackerLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(attackerLeftWhenAttackerWon, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(averageChangeInTuv, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(6, 10, 0, 0), 0, 0));
    resultsText.add(roundsAverage, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    resultsText.add(count, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(15, 10, 0, 0), 0, 0));
    resultsText.add(time, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 10, 0, 0), 0, 0));
    row2++;
    final JButton swapSidesButton = new JButton("Swap Sides");
    resultsText.add(swapSidesButton, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.BOTH, new Insets(6, 10, 0, 100), 0, 0));
    resultsText.add(numRuns, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(20, 10, 0, 0), 0, 0));
    resultsText.add(retreatAfterXRounds, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
    resultsText.add(retreatAfterXUnitsLeft, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(10, 10, 0, 0), 0, 0));
    row1 = row2;

    final JButton orderOfLossesButton = new JButton("Order Of Losses");
    resultsText.add(orderOfLossesButton, new GridBagConstraints(0, row1++, 1, 1, 0, 0, GridBagConstraints.EAST,
        GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
    if (territoryEffectsJList != null) {
      resultsText.add(new JScrollPane(territoryEffectsJList),
          new GridBagConstraints(0, row1, 1, territoryEffectsJList.getVisibleRowCount(), 0, 0,
              GridBagConstraints.EAST, GridBagConstraints.BOTH, new Insets(10, 15, 0, 0), 0, 0));
    }
    resultsText.add(retreatWhenOnlyAirLeftCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(10, 10, 0, 5), 0, 0));
    resultsText.add(keepOneAttackingLandUnitCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0,
        GridBagConstraints.WEST, GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
    resultsText.add(amphibiousCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));
    resultsText.add(landBattleCheckBox, new GridBagConstraints(1, row2++, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(2, 10, 0, 5), 0, 0));

    final JPanel resultsPanel = new JPanel();
    resultsPanel.add(resultsText);
    resultsPanel.setBorder(BorderFactory.createEmptyBorder());
    final JScrollPane resultsScroll = new JScrollPane(resultsPanel);
    resultsScroll.setBorder(BorderFactory.createEmptyBorder());
    final Dimension resultsScrollDimensions = resultsScroll.getPreferredSize();
    // add some so that we don't have double scroll bars appear when only one is needed
    resultsScrollDimensions.width += 22;
    resultsScroll.setPreferredSize(resultsScrollDimensions);
    main.add(resultsScroll, BorderLayout.EAST);

    final JPanel south = new JPanel();
    south.setLayout(new BorderLayout());
    final JPanel buttons = new JPanel();
    buttons.setLayout(new FlowLayout(FlowLayout.CENTER));
    final JButton closeButton = new JButton("Close");
    buttons.add(closeButton);
    south.add(buttons, BorderLayout.SOUTH);
    add(south, BorderLayout.SOUTH);

    defenderCombo.addActionListener(e -> {
      data.acquireReadLock();
      try {
        if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
          attackerCombo.setSelectedItem(getEnemy(getDefender()));
        }
      } finally {
        data.releaseReadLock();
      }
      updateDefender(null);
      setWidgetActivation();
    });
    attackerCombo.addActionListener(e -> {
      data.acquireReadLock();
      try {
        if (data.getRelationshipTracker().isAllied(getDefender(), getAttacker())) {
          defenderCombo.setSelectedItem(getEnemy(getAttacker()));
        }
      } finally {
        data.releaseReadLock();
      }
      updateAttacker(null);
      setWidgetActivation();
    });
    amphibiousCheckBox.addActionListener(e -> setWidgetActivation());
    landBattleCheckBox.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      updateDefender(null);
      updateAttacker(null);
      setWidgetActivation();
    });
    calculateButton.addActionListener(e -> updateStats());
    closeButton.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      parent.setVisible(false);
      shutdown();
      parent.dispatchEvent(new WindowEvent(parent, WindowEvent.WINDOW_CLOSING));
    });
    clearButton.addActionListener(e -> {
      defendingUnitsPanel.clear();
      attackingUnitsPanel.clear();
      setWidgetActivation();
    });
    swapSidesButton.addActionListener(e -> {
      attackerOrderOfLosses = null;
      defenderOrderOfLosses = null;
      final List<Unit> getDefenders = defendingUnitsPanel.getUnits();
      final List<Unit> getAttackers = attackingUnitsPanel.getUnits();
      swapSidesCombo.setSelectedItem(getAttacker());
      attackerCombo.setSelectedItem(getDefender());
      defenderCombo.setSelectedItem(getSwapSides());
      attackingUnitsPanel.init(getAttacker(), getDefenders, isLand());
      defendingUnitsPanel.init(getDefender(), getAttackers, isLand());
      setWidgetActivation();
    });
    orderOfLossesButton.addActionListener(e -> {
      final OrderOfLossesInputPanel oolPanel = new OrderOfLossesInputPanel(attackerOrderOfLosses,
          defenderOrderOfLosses, attackingUnitsPanel.getCategories(), defendingUnitsPanel.getCategories(),
          landBattleCheckBox.isSelected(), uiContext, data);
      if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(OddsCalculatorPanel.this, oolPanel,
          "Create Order Of Losses for each side", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)) {
        if (OrderOfLossesInputPanel.isValidOrderOfLoss(oolPanel.getAttackerOrder(), data)) {
          attackerOrderOfLosses = oolPanel.getAttackerOrder();
        }
        if (OrderOfLossesInputPanel.isValidOrderOfLoss(oolPanel.getDefenderOrder(), data)) {
          defenderOrderOfLosses = oolPanel.getDefenderOrder();
        }
      }
    });
    if (territoryEffectsJList != null) {
      territoryEffectsJList.addListSelectionListener(e -> setWidgetActivation());
    }
    attackingUnitsPanel.addChangeListener(this::setWidgetActivation);
    defendingUnitsPanel.addChangeListener(this::setWidgetActivation);

    // use the one passed, not the one we found:
    if (location != null) {
      data.acquireReadLock();
      try {
        landBattleCheckBox.setSelected(!location.isWater());
        // default to the current player
        if (data.getSequence().getStep().getPlayerId() != null
            && !data.getSequence().getStep().getPlayerId().isNull()) {
          attackerCombo.setSelectedItem(data.getSequence().getStep().getPlayerId());
        }
        if (!location.isWater()) {
          defenderCombo.setSelectedItem(location.getOwner());
        } else {
          // we need to find out the defender for sea zones
          for (final PlayerID player : location.getUnits().getPlayersWithUnits()) {
            if (!player.equals(getAttacker()) && !data.getRelationshipTracker().isAllied(player, getAttacker())) {
              defenderCombo.setSelectedItem(player);
              break;
            }
          }
        }
        updateDefender(location.getUnits().getMatches(Matches.alliedUnit(getDefender(), data)));
        updateAttacker(location.getUnits().getMatches(Matches.alliedUnit(getAttacker(), data)));
      } finally {
        data.releaseReadLock();
      }
    } else {
      landBattleCheckBox.setSelected(true);
      defenderCombo.setSelectedItem(data.getPlayerList().getPlayers().iterator().next());
      updateDefender(null);
      updateAttacker(null);
    }
    calculator = new ConcurrentOddsCalculator("BtlCalc Panel", () -> SwingUtilities.invokeLater(() -> {
      calculateButton.setText("Calculate Odds");
      calculateButton.setEnabled(true);
    }));

    calculator.setGameData(data);
    setWidgetActivation();
    revalidate();
  }

  void shutdown() {
    try {
      // use this if not using a static calc, so that we gc the calc and shutdown all threads.
      // must be shutdown, as it has a thread pool per each instance.
      calculator.shutdown();
    } catch (final Exception e) {
      log.log(Level.SEVERE, "Failed to shut down odds calculator", e);
    }
  }

  private PlayerID getDefender() {
    return (PlayerID) defenderCombo.getSelectedItem();
  }

  private PlayerID getAttacker() {
    return (PlayerID) attackerCombo.getSelectedItem();
  }

  private PlayerID getSwapSides() {
    return (PlayerID) swapSidesCombo.getSelectedItem();
  }


  private boolean isAmphibiousBattle() {
    return (landBattleCheckBox.isSelected() && amphibiousCheckBox.isSelected());
  }

  private Collection<TerritoryEffect> getTerritoryEffects() {
    final Collection<TerritoryEffect> territoryEffects = new ArrayList<>();
    if (territoryEffectsJList != null) {
      final List<String> selected = territoryEffectsJList.getSelectedValuesList();
      data.acquireReadLock();
      try {
        final Map<String, TerritoryEffect> allTerritoryEffects = data.getTerritoryEffectList();
        for (final String selection : selected) {
          if (selection.equals(NO_EFFECTS)) {
            territoryEffects.clear();
            break;
          }
          territoryEffects.add(allTerritoryEffects.get(selection));
        }
      } finally {
        data.releaseReadLock();
      }
    }
    return territoryEffects;
  }

  private void updateStats() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new IllegalStateException("Wrong thread");
    }
    final AtomicReference<AggregateResults> results = new AtomicReference<>();
    final WaitDialog dialog = new WaitDialog(
        this,
        "Calculating Odds (" + calculator.getThreadCount() + " threads)",
        calculator::cancel);
    final AtomicReference<Collection<Unit>> defenders = new AtomicReference<>();
    final AtomicReference<Collection<Unit>> attackers = new AtomicReference<>();
    new Thread(() -> {
      try {
        // find a territory to fight in
        Territory location = null;
        if (this.location == null || this.location.isWater() == isLand()) {
          for (final Territory t : data.getMap()) {
            if (t.isWater() == !isLand()) {
              location = t;
              break;
            }
          }
        } else {
          location = this.location;
        }
        if (location == null) {
          throw new IllegalStateException("No territory found that is land:" + isLand());
        }
        final List<Unit> defending = defendingUnitsPanel.getUnits();
        final List<Unit> attacking = attackingUnitsPanel.getUnits();
        List<Unit> bombarding = new ArrayList<>();
        if (isLand()) {
          bombarding = CollectionUtils.getMatches(attacking, Matches.unitCanBombard(getAttacker()));
          attacking.removeAll(bombarding);
        }
        calculator.setRetreatAfterRound(retreatAfterXRounds.getValue());
        calculator.setRetreatAfterXUnitsLeft(retreatAfterXUnitsLeft.getValue());
        if (retreatWhenOnlyAirLeftCheckBox.isSelected()) {
          calculator.setRetreatWhenOnlyAirLeft(true);
        } else {
          calculator.setRetreatWhenOnlyAirLeft(false);
        }
        if (landBattleCheckBox.isSelected() && keepOneAttackingLandUnitCheckBox.isSelected()) {
          calculator.setKeepOneAttackingLandUnit(true);
        } else {
          calculator.setKeepOneAttackingLandUnit(false);
        }
        if (isAmphibiousBattle()) {
          calculator.setAmphibious(true);
        } else {
          calculator.setAmphibious(false);
        }
        calculator.setAttackerOrderOfLosses(attackerOrderOfLosses);
        calculator.setDefenderOrderOfLosses(defenderOrderOfLosses);
        final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
        defenders.set(defending);
        attackers.set(attacking);
        results.set(calculator.setCalculateDataAndCalculate(getAttacker(), getDefender(), location, attacking,
            defending, bombarding, territoryEffects, numRuns.getValue()));
      } finally {
        SwingUtilities.invokeLater(() -> {
          dialog.setVisible(false);
          dialog.dispose();
        });
      }
    }, "Odds calc thread").start();
    // the runnable setting the dialog visible must run after this code executes, since this code is running on the
    // swing event thread
    dialog.setVisible(true);
    // results.get() could be null if we cancelled to quickly or something weird like that.
    if (results.get() == null) {
      setResultsToBlank();
    } else {
      attackerWin.setText(formatPercentage(results.get().getAttackerWinPercent()));
      defenderWin.setText(formatPercentage(results.get().getDefenderWinPercent()));
      draw.setText(formatPercentage(results.get().getDrawPercent()));
      final boolean isLand = isLand();
      final List<Unit> mainCombatAttackers =
          CollectionUtils.getMatches(attackers.get(), Matches.unitCanBeInBattle(true, isLand, 1, false, true, true));
      final List<Unit> mainCombatDefenders =
          CollectionUtils.getMatches(defenders.get(), Matches.unitCanBeInBattle(false, isLand, 1, false, true, true));
      final int attackersTotal = mainCombatAttackers.size();
      final int defendersTotal = mainCombatDefenders.size();
      defenderLeft.setText(formatValue(results.get().getAverageDefendingUnitsLeft()) + " / " + defendersTotal);
      attackerLeft.setText(formatValue(results.get().getAverageAttackingUnitsLeft()) + " / " + attackersTotal);
      defenderLeftWhenDefenderWon
          .setText(formatValue(results.get().getAverageDefendingUnitsLeftWhenDefenderWon()) + " / " + defendersTotal);
      attackerLeftWhenAttackerWon
          .setText(formatValue(results.get().getAverageAttackingUnitsLeftWhenAttackerWon()) + " / " + attackersTotal);
      roundsAverage.setText("" + formatValue(results.get().getAverageBattleRoundsFought()));
      try {
        data.acquireReadLock();
        averageChangeInTuv.setText("" + formatValue(results.get().getAverageTuvSwing(getAttacker(),
            mainCombatAttackers, getDefender(), mainCombatDefenders, data)));
      } finally {
        data.releaseReadLock();
      }
      count.setText(results.get().getRollCount() + "");
      time.setText(formatValue(results.get().getTime() / 1000.0) + " s");
    }
  }

  private static String formatPercentage(final double percentage) {
    return new DecimalFormat("#%").format(percentage);
  }

  private static String formatValue(final double value) {
    return new DecimalFormat("#0.##").format(value);
  }

  private void updateDefender(final List<Unit> initialUnits) {
    final List<Unit> units = Optional.ofNullable(initialUnits).orElseGet(Collections::emptyList);
    final boolean isLand = isLand();
    defendingUnitsPanel.init(
        getDefender(),
        CollectionUtils.getMatches(units, Matches.unitCanBeInBattle(false, isLand, 1, false, false, false)),
        isLand);
  }

  private void updateAttacker(final List<Unit> initialUnits) {
    final List<Unit> units = Optional.ofNullable(initialUnits).orElseGet(Collections::emptyList);
    final boolean isLand = isLand();
    attackingUnitsPanel.init(
        getAttacker(),
        CollectionUtils.getMatches(units, Matches.unitCanBeInBattle(true, isLand, 1, false, false, false)),
        isLand);
  }

  private boolean isLand() {
    return landBattleCheckBox.isSelected();
  }

  private PlayerID getEnemy(final PlayerID player) {
    for (final PlayerID id : data.getPlayerList()) {
      if (data.getRelationshipTracker().isAtWar(player, id)) {
        return id;
      }
    }
    for (final PlayerID id : data.getPlayerList()) {
      if (!data.getRelationshipTracker().isAllied(player, id)) {
        return id;
      }
    }
    // TODO: do we allow fighting allies in the battle calc?
    throw new IllegalStateException("No enemies or non-allies for :" + player);
  }

  private void setResultsToBlank() {
    final String blank = "------";
    attackerWin.setText(blank);
    defenderWin.setText(blank);
    draw.setText(blank);
    defenderLeft.setText(blank);
    attackerLeft.setText(blank);
    defenderLeftWhenDefenderWon.setText(blank);
    attackerLeftWhenAttackerWon.setText(blank);
    roundsAverage.setText(blank);
    averageChangeInTuv.setText(blank);
    count.setText(blank);
    time.setText(blank);
  }

  private void setWidgetActivation() {
    keepOneAttackingLandUnitCheckBox.setEnabled(landBattleCheckBox.isSelected());
    amphibiousCheckBox.setEnabled(landBattleCheckBox.isSelected());
    final boolean isLand = isLand();
    try {
      data.acquireReadLock();
      // do not include bombardment and aa guns in our "total" labels
      final List<Unit> attackers = CollectionUtils.getMatches(attackingUnitsPanel.getUnits(),
          Matches.unitCanBeInBattle(true, isLand, 1, false, true, true));
      final List<Unit> defenders = CollectionUtils.getMatches(defendingUnitsPanel.getUnits(),
          Matches.unitCanBeInBattle(false, isLand, 1, false, true, true));
      attackerUnitsTotalNumber.setText("Units: " + attackers.size());
      defenderUnitsTotalNumber.setText("Units: " + defenders.size());
      attackerUnitsTotalTuv.setText("TUV: " + TuvUtils.getTuv(attackers, getAttacker(),
          TuvUtils.getCostsForTuv(getAttacker(), data), data));
      defenderUnitsTotalTuv.setText("TUV: " + TuvUtils.getTuv(defenders, getDefender(),
          TuvUtils.getCostsForTuv(getDefender(), data), data));
      final int attackHitPoints = BattleCalculator.getTotalHitpointsLeft(attackers);
      final int defenseHitPoints = BattleCalculator.getTotalHitpointsLeft(defenders);
      attackerUnitsTotalHitpoints.setText("HP: " + attackHitPoints);
      defenderUnitsTotalHitpoints.setText("HP: " + defenseHitPoints);
      final boolean isAmphibiousBattle = isAmphibiousBattle();
      final Collection<TerritoryEffect> territoryEffects = getTerritoryEffects();
      final IntegerMap<UnitType> costs = TuvUtils.getCostsForTuv(getAttacker(), data);
      attackers.sort(new UnitBattleComparator(false, costs, territoryEffects, data, false, false));
      Collections.reverse(attackers);
      final int attackPower = DiceRoll.getTotalPower(DiceRoll.getUnitPowerAndRollsForNormalBattles(attackers, defenders,
          false, false, data, location, territoryEffects, isAmphibiousBattle,
          (isAmphibiousBattle ? attackers : new ArrayList<>())), data);
      // defender is never amphibious
      final int defensePower =
          DiceRoll
              .getTotalPower(
                  DiceRoll.getUnitPowerAndRollsForNormalBattles(defenders, attackers, true, false,
                      data, location, territoryEffects, isAmphibiousBattle, new ArrayList<>()),
                  data);
      attackerUnitsTotalPower.setText("Power: " + attackPower);
      defenderUnitsTotalPower.setText("Power: " + defensePower);
    } finally {
      data.releaseReadLock();
    }
  }

  class PlayerRenderer extends DefaultListCellRenderer {
    private static final long serialVersionUID = -7639128794342607309L;

    @Override
    public Component getListCellRendererComponent(final JList<?> list, final Object value, final int index,
        final boolean isSelected, final boolean cellHasFocus) {
      super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
      final PlayerID id = (PlayerID) value;
      setText(id.getName());
      setIcon(new ImageIcon(uiContext.getFlagImageFactory().getSmallFlag(id)));
      return this;
    }
  }

  void selectCalculateButton() {
    calculateButton.requestFocus();
  }

  private static boolean doesPlayerHaveUnitsOnMap(final PlayerID player, final GameData data) {
    for (final Territory t : data.getMap()) {
      for (final Unit u : t.getUnits()) {
        if (u.getOwner().equals(player)) {
          return true;
        }
      }
    }
    return false;
  }
}
