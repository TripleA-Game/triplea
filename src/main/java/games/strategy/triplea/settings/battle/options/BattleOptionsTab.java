package games.strategy.triplea.settings.battle.options;

import java.util.Arrays;
import java.util.List;

import games.strategy.engine.ClientContext;
import games.strategy.triplea.settings.SettingInputComponent;
import games.strategy.triplea.settings.SettingInputComponentFactory;
import games.strategy.triplea.settings.SettingsTab;

public class BattleOptionsTab implements SettingsTab<BattleOptionsSettings> {
  private final List<SettingInputComponent<BattleOptionsSettings>> inputs;

  public BattleOptionsTab(final BattleOptionsSettings battleOptionSettings) {
    inputs = Arrays.asList(
        SettingInputComponentFactory.buildYesOrNoRadioButtons("Confirm Enemy Casualties",
            "When set to yes, enemy casualty selections will always require confirmation.",
            battleOptionSettings.confirmEnemyCasualties(),
            ((settings, s) -> settings.setConfirmEnemyCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmEnemyCasualties()))),
        SettingInputComponentFactory.buildYesOrNoRadioButtons("Confirm Defensive Rolls",
            "When set to yes, defender dice rolls will always require confirmation.",
            battleOptionSettings.confirmDefensiveRolls(),
            ((settings, s) -> settings.setConfirmDefensiveRolls(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.confirmDefensiveRolls()))),
        SettingInputComponentFactory.buildYesOrNoRadioButtons("Focus on own casualties",
            "When set to yes, the default casualty selection can be accepted by pressing space bar. When set to 'no', "
                + "you will always have to click a button to confirm casualty selections.",
            battleOptionSettings.focusOnOwnCasualties(),
            ((settings, s) -> settings.setFocusOnOwnCasualties(Boolean.valueOf(s))),
            (settings -> String.valueOf(settings.focusOnOwnCasualties()))));
  }

  @Override
  public String getTabTitle() {
    return "Combat Options";
  }

  @Override
  public List<SettingInputComponent<BattleOptionsSettings>> getInputs() {
    return inputs;
  }

  @Override
  public BattleOptionsSettings getSettingsObject() {
    return ClientContext.battleOptionsSettings();
  }
}
