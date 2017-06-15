package games.strategy.engine.framework.startup.ui;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;

import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.properties.GameProperties;
import games.strategy.triplea.Constants;

class PlayerSelectorRow {

  private final JCheckBox enabledCheckBox;
  private final String playerName;
  private final JComboBox<String> playerTypes;
  private JComponent incomePercentage;
  private final JLabel incomePercentageLabel;
  private JComponent puIncomeBonus;
  private final JLabel puIncomeBonusLabel;
  private boolean enabled = true;
  private final JLabel name;
  private final JLabel alliances;
  private final Collection<String> disableable;
  private final SetupPanel parent;

  PlayerSelectorRow(final PlayerID player, final Map<String, String> reloadSelections,
      final Collection<String> disableable, final HashMap<String, Boolean> playersEnablementListing,
      final Collection<String> playerAlliances, final String[] types, final SetupPanel parent,
      final GameProperties gameProperties) {
    this.disableable = disableable;
    this.parent = parent;
    playerName = player.getName();
    name = new JLabel(playerName + ":");

    enabledCheckBox = new JCheckBox();
    final ActionListener m_disablePlayerActionListener = new ActionListener() {
      @Override
      public void actionPerformed(final ActionEvent e) {
        if (enabledCheckBox.isSelected()) {
          enabled = true;
          // the 1st in the list should be human
          playerTypes.setSelectedItem(types[0]);
        } else {
          enabled = false;
          // the 2nd in the list should be Weak AI
          playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 1))]);
        }
        setWidgetActivation();
      }
    };
    enabledCheckBox.addActionListener(m_disablePlayerActionListener);
    enabledCheckBox.setSelected(playersEnablementListing.get(playerName));
    enabledCheckBox.setEnabled(disableable.contains(playerName));

    playerTypes = new JComboBox<>(types);
    String previousSelection = reloadSelections.get(playerName);
    if (previousSelection.equalsIgnoreCase("Client")) {
      previousSelection = types[0];
    }
    if (!(previousSelection.equals("no_one")) && Arrays.asList(types).contains(previousSelection)) {
      playerTypes.setSelectedItem(previousSelection);
    } else if (player.isAiDefault()) {
      // the 4th in the list should be Pro AI (Hard AI)
      playerTypes.setSelectedItem(types[Math.max(0, Math.min(types.length - 1, 3))]);
    }

    // we do not set the default for the combo box because the default is the top item, which in this case is human
    String alliancesLabelText;
    if (playerAlliances.contains(playerName)) {
      alliancesLabelText = "";
    } else {
      alliancesLabelText = playerAlliances.toString();
    }
    alliances = new JLabel(alliancesLabelText);

    // TODO: remove null check for next incompatible release
    incomePercentage = null;
    if (gameProperties.getPlayerProperty(Constants.getIncomePercentageFor(player)) != null) {
      incomePercentage =
          gameProperties.getPlayerProperty(Constants.getIncomePercentageFor(player)).getEditorComponent();
    }
    incomePercentageLabel = new JLabel("%");

    // TODO: remove null check for next incompatible release
    puIncomeBonus = null;
    if (gameProperties.getPlayerProperty(Constants.getPuIncomeBonus(player)) != null) {
      puIncomeBonus =
          gameProperties.getPlayerProperty(Constants.getPuIncomeBonus(player)).getEditorComponent();
    }
    puIncomeBonusLabel = new JLabel("PUs");

    setWidgetActivation();
  }

  void layout(final int row, final Container container) {
    int gridx = 0;
    if (!disableable.isEmpty()) {
      container.add(enabledCheckBox, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    }
    container.add(name, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    container.add(playerTypes, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 5, 5, 0), 0, 0));
    container.add(alliances, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
        GridBagConstraints.NONE, new Insets(0, 7, 5, 5), 0, 0));
    // TODO: remove null check for next incompatible release
    if (incomePercentage != null) {
      container.add(incomePercentage, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 20, 2, 0), 0, 0));
      container.add(incomePercentageLabel,
          new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    }
    // TODO: remove null check for next incompatible release
    if (puIncomeBonus != null) {
      container.add(puIncomeBonus, new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
          GridBagConstraints.NONE, new Insets(0, 20, 2, 0), 0, 0));
      container.add(puIncomeBonusLabel,
          new GridBagConstraints(gridx++, row, 1, 1, 0, 0, GridBagConstraints.WEST,
              GridBagConstraints.NONE, new Insets(0, 5, 5, 5), 0, 0));
    }
  }

  void setResourceModifiersVisble(boolean isVisible) {
    incomePercentage.setVisible(isVisible);
    incomePercentageLabel.setVisible(isVisible);
    puIncomeBonus.setVisible(isVisible);
    puIncomeBonusLabel.setVisible(isVisible);
  }

  String getPlayerName() {
    return playerName;
  }

  String getPlayerType() {
    return (String) playerTypes.getSelectedItem();
  }

  boolean isPlayerEnabled() {
    return enabledCheckBox.isSelected();
  }

  private void setWidgetActivation() {
    name.setEnabled(enabled);
    alliances.setEnabled(enabled);
    enabledCheckBox.setEnabled(disableable.contains(playerName));
    incomePercentage.setEnabled(enabled);
    puIncomeBonus.setEnabled(enabled);
    parent.notifyObservers();
  }

}
