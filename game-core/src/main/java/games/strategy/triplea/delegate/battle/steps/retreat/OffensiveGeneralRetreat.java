package games.strategy.triplea.delegate.battle.steps.retreat;

import static games.strategy.triplea.delegate.battle.BattleStepStrings.ATTACKER_WITHDRAW;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Properties;
import games.strategy.triplea.delegate.ExecutionStack;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.IBattle;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.delegate.battle.steps.BattleStep;
import games.strategy.triplea.delegate.battle.steps.RetreatChecks;
import java.util.Collection;
import java.util.List;
import lombok.AllArgsConstructor;
import org.triplea.java.RemoveOnNextMajorRelease;
import org.triplea.sound.SoundUtils;

@AllArgsConstructor
public class OffensiveGeneralRetreat implements BattleStep {

  private static final long serialVersionUID = -9192684621899682418L;

  private final BattleState battleState;

  private final BattleActions battleActions;

  @Override
  public List<String> getNames() {
    return isRetreatPossible() ? List.of(getName()) : List.of();
  }

  private boolean isRetreatPossible() {
    return canAttackerRetreat()
        || canAttackerRetreatSeaPlanes()
        || (battleState.isAmphibious()
            && (canAttackerRetreatPartialAmphib() || canAttackerRetreatAmphibPlanes()));
  }

  private boolean canAttackerRetreat() {
    return RetreatChecks.canAttackerRetreat(
        battleState.getUnits(BattleState.Side.DEFENSE),
        battleState.getGameData(),
        battleState::getAttackerRetreatTerritories,
        battleState.isAmphibious());
  }

  private boolean canAttackerRetreatSeaPlanes() {
    return battleState.getBattleSite().isWater()
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private boolean canAttackerRetreatPartialAmphib() {
    if (!Properties.getPartialAmphibiousRetreat(battleState.getGameData())) {
      return false;
    }
    // Only include land units when checking for allow amphibious retreat
    return battleState.getUnits(BattleState.Side.OFFENSE).stream()
        .filter(Matches.unitIsLand())
        .anyMatch(Matches.unitWasNotAmphibious());
  }

  private boolean canAttackerRetreatAmphibPlanes() {
    final GameData gameData = battleState.getGameData();
    return (Properties.getWW2V2(gameData)
            || Properties.getAttackerRetreatPlanes(gameData)
            || Properties.getPartialAmphibiousRetreat(gameData))
        && battleState.getUnits(BattleState.Side.OFFENSE).stream().anyMatch(Matches.unitIsAir());
  }

  private String getName() {
    return battleState.getAttacker().getName() + ATTACKER_WITHDRAW;
  }

  @Override
  public Order getOrder() {
    return Order.OFFENSIVE_GENERAL_RETREAT;
  }

  @Override
  public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
    retreatUnits(bridge);
  }

  @RemoveOnNextMajorRelease("This doesn't need to be public in the next major release")
  public void retreatUnits(final IDelegateBridge bridge) {
    if (battleState.isOver()) {
      return;
    }

    Retreater retreater = null;

    if (battleState.isAmphibious()) {
      retreater = getAmphibiousRetreater();
    } else if (canAttackerRetreat()) {
      retreater = new RetreaterGeneral(battleState);
    }

    if (retreater != null) {
      final Collection<Unit> retreatUnits = retreater.getRetreatUnits();
      final Collection<Territory> possibleRetreatSites =
          retreater.getPossibleRetreatSites(retreatUnits);

      bridge.getDisplayChannelBroadcaster().gotoBattleStep(battleState.getBattleId(), getName());
      final Territory retreatTo =
          battleActions.queryRetreatTerritory(
              battleState,
              bridge,
              battleState.getAttacker(),
              possibleRetreatSites,
              getQueryText(retreater.getRetreatType()));

      if (retreatTo != null) {
        retreat(bridge, retreater, retreatTo);
      }
    }
  }

  private Retreater getAmphibiousRetreater() {
    if (canAttackerRetreatPartialAmphib()) {
      return new RetreaterPartialAmphibious(battleState);
    } else if (canAttackerRetreatAmphibPlanes()) {
      return new RetreaterAirAmphibious(battleState);
    }
    return null;
  }

  private String getQueryText(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return battleState.getAttacker().getName() + " retreat?";
      case PARTIAL_AMPHIB:
        return battleState.getAttacker().getName() + " retreat non-amphibious units?";
      case PLANES:
        return battleState.getAttacker().getName() + " retreat planes?";
    }
  }

  private void retreat(
      final IDelegateBridge bridge, final Retreater retreater, final Territory retreatTo) {
    final Collection<Unit> retreatUnits = retreater.getRetreatUnits();

    SoundUtils.playRetreatType(
        battleState.getAttacker(), retreatUnits, retreater.getRetreatType(), bridge);

    final Retreater.RetreatChanges retreatChanges = retreater.computeChanges(retreatTo);
    bridge.addChange(retreatChanges.getChange());

    retreatChanges
        .getHistoryText()
        .forEach(
            historyChild -> {
              bridge
                  .getHistoryWriter()
                  .addChildToEvent(historyChild.getText(), historyChild.getUnits());
            });

    if (battleState.getUnits(BattleState.Side.OFFENSE).isEmpty()) {
      battleActions.endBattle(IBattle.WhoWon.DEFENDER, bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreatUnits);
    }

    bridge
        .getDisplayChannelBroadcaster()
        .notifyRetreat(
            battleState.getAttacker().getName()
                + getShortBroadcastSuffix(retreater.getRetreatType()),
            battleState.getAttacker().getName()
                + getLongBroadcastSuffix(retreater.getRetreatType(), retreatTo),
            getName(),
            battleState.getAttacker());
  }

  private String getShortBroadcastSuffix(final MustFightBattle.RetreatType retreatType) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return " retreats";
      case PARTIAL_AMPHIB:
        return " retreats non-amphibious units";
      case PLANES:
        return " retreats planes";
    }
  }

  private String getLongBroadcastSuffix(
      final MustFightBattle.RetreatType retreatType, final Territory retreatTo) {
    switch (retreatType) {
      case DEFAULT:
      default:
        return " retreats all units to " + retreatTo.getName();
      case PARTIAL_AMPHIB:
        return " retreats non-amphibious units";
      case PLANES:
        return " retreats planes";
    }
  }
}
