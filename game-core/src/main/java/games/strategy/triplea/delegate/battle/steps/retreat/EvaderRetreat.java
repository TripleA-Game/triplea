package games.strategy.triplea.delegate.battle.steps.retreat;

import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.MustFightBattle;
import games.strategy.triplea.formatter.MyFormatter;
import java.util.ArrayList;
import java.util.Collection;
import lombok.Builder;
import lombok.experimental.UtilityClass;
import org.triplea.sound.SoundUtils;

@UtilityClass
public class EvaderRetreat {

  @Builder(toBuilder = true)
  public static class Parameters {
    BattleState battleState;
    BattleActions battleActions;
    BattleState.Side side;
    IDelegateBridge bridge;
    Collection<Territory> possibleRetreatSites;
    Collection<Unit> units;
    String step;
  }

  public static void retreatUnits(final Parameters parameters) {
    final GamePlayer retreatingPlayer =
        parameters.side == BattleState.Side.DEFENSE
            ? parameters.battleState.getDefender()
            : parameters.battleState.getAttacker();
    final String text = retreatingPlayer.getName() + " retreat subs?";

    parameters
        .bridge
        .getDisplayChannelBroadcaster()
        .gotoBattleStep(parameters.battleState.getBattleId(), parameters.step);
    final boolean isAttemptingSubmerge =
        parameters.possibleRetreatSites.size() == 1
            && parameters.possibleRetreatSites.contains(parameters.battleState.getBattleSite());
    final Territory retreatTo =
        isAttemptingSubmerge
            ? parameters.battleActions.querySubmergeTerritory(
                parameters.battleState,
                parameters.bridge,
                retreatingPlayer,
                parameters.possibleRetreatSites,
                text)
            : parameters.battleActions.queryRetreatTerritory(
                parameters.battleState,
                parameters.bridge,
                retreatingPlayer,
                parameters.possibleRetreatSites,
                text);
    if (retreatTo == null) {
      return;
    }
    SoundUtils.playRetreatType(
        retreatingPlayer, parameters.units, MustFightBattle.RetreatType.SUBS, parameters.bridge);
    if (parameters.battleState.getBattleSite().equals(retreatTo)) {
      submergeEvaders(parameters);
      parameters
          .bridge
          .getDisplayChannelBroadcaster()
          .notifyRetreat(
              retreatingPlayer.getName() + " submerges subs",
              retreatingPlayer.getName() + " submerges subs",
              parameters.step,
              retreatingPlayer);
    } else {
      retreatEvaders(parameters, retreatTo);
      parameters
          .bridge
          .getDisplayChannelBroadcaster()
          .notifyRetreat(
              retreatingPlayer.getName() + " retreats",
              retreatingPlayer.getName() + (" retreats subs to " + retreatTo.getName()),
              parameters.step,
              retreatingPlayer);
    }
  }

  public static void submergeEvaders(final Parameters parameters) {
    final CompositeChange change = new CompositeChange();
    for (final Unit u : parameters.units) {
      change.add(ChangeFactory.unitPropertyChange(u, true, Unit.SUBMERGED));
    }
    parameters.bridge.addChange(change);
    parameters.battleState.retreatUnits(parameters.side, parameters.units);

    addHistoryRetreat(parameters.bridge, parameters.units, " submerged");
    notifyRetreat(
        parameters.battleState,
        parameters.battleActions,
        parameters.units,
        parameters.side,
        parameters.bridge);
  }

  private static void addHistoryRetreat(
      final IDelegateBridge bridge, final Collection<Unit> units, final String suffix) {
    final String transcriptText = MyFormatter.unitsToText(units) + suffix;
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(units));
  }

  private static void notifyRetreat(
      final BattleState battleState,
      final BattleActions battleActions,
      final Collection<Unit> retreating,
      final BattleState.Side side,
      final IDelegateBridge bridge) {
    if (battleState.getUnits(side).isEmpty()) {
      battleActions.endBattle(side.getOpposite().getWhoWon(), bridge);
    } else {
      bridge.getDisplayChannelBroadcaster().notifyRetreat(battleState.getBattleId(), retreating);
    }
  }

  private static void retreatEvaders(final Parameters parameters, final Territory retreatTo) {
    final CompositeChange change = new CompositeChange();
    change.add(
        ChangeFactory.moveUnits(
            parameters.battleState.getBattleSite(), retreatTo, parameters.units));
    parameters.bridge.addChange(change);
    parameters.battleState.retreatUnits(parameters.side, parameters.units);

    addHistoryRetreat(parameters.bridge, parameters.units, " retreated to " + retreatTo.getName());
    notifyRetreat(
        parameters.battleState,
        parameters.battleActions,
        parameters.units,
        parameters.side,
        parameters.bridge);
  }
}
