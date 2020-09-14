package games.strategy.triplea.delegate.battle.steps;

import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_DEFENSIVE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_DEFENSIVE_REGULAR;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_OFFENSIVE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.FIRST_STRIKE_OFFENSIVE_REGULAR;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_AFTER_BATTLE;
import static games.strategy.triplea.delegate.battle.steps.BattleStep.Order.SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE;

import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.steps.change.LandParatroopers;
import games.strategy.triplea.delegate.battle.steps.change.RemoveUnprotectedUnits;
import games.strategy.triplea.delegate.battle.steps.fire.NavalBombardment;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirAttackVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirDefendVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.ClearFirstStrikeCasualties;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.DefensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.firststrike.OffensiveFirstStrike;
import games.strategy.triplea.delegate.battle.steps.fire.general.DefensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.fire.general.OffensiveGeneral;
import games.strategy.triplea.delegate.battle.steps.retreat.DefensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveGeneralRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.OffensiveSubsRetreat;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import java.util.ArrayList;
import java.util.List;
import lombok.Builder;

/** Get the steps that will occur in the battle */
@Builder
public class BattleSteps implements BattleStepStrings {

  final BattleState battleState;
  final BattleActions battleActions;

  public List<String> get() {
    final BattleStep offensiveAaStep = new OffensiveAaFire(battleState, battleActions);
    final BattleStep defensiveAaStep = new DefensiveAaFire(battleState, battleActions);
    final BattleStep submergeSubsVsOnlyAir =
        new SubmergeSubsVsOnlyAirStep(battleState, battleActions);
    final BattleStep removeUnprotectedUnits =
        new RemoveUnprotectedUnits(battleState, battleActions);
    final BattleStep airAttackVsNonSubs = new AirAttackVsNonSubsStep(battleState);
    final BattleStep airDefendVsNonSubs = new AirDefendVsNonSubsStep(battleState);
    final BattleStep navalBombardment = new NavalBombardment(battleState, battleActions);
    final BattleStep landParatroopers = new LandParatroopers(battleState, battleActions);
    final BattleStep offensiveSubsSubmerge = new OffensiveSubsRetreat(battleState, battleActions);
    final BattleStep defensiveSubsSubmerge = new DefensiveSubsRetreat(battleState, battleActions);
    final BattleStep offensiveFirstStrike = new OffensiveFirstStrike(battleState, battleActions);
    final BattleStep defensiveFirstStrike = new DefensiveFirstStrike(battleState, battleActions);
    final BattleStep firstStrikeCasualties =
        new ClearFirstStrikeCasualties(battleState, battleActions);
    final BattleStep offensiveStandard = new OffensiveGeneral(battleState, battleActions);
    final BattleStep defensiveStandard = new DefensiveGeneral(battleState, battleActions);
    final BattleStep generalRetreat = new OffensiveGeneralRetreat(battleState, battleActions);

    final List<String> steps = new ArrayList<>();
    steps.addAll(offensiveAaStep.getNames());
    steps.addAll(defensiveAaStep.getNames());

    steps.addAll(navalBombardment.getNames());
    steps.addAll(landParatroopers.getNames());

    if (offensiveSubsSubmerge.getOrder() == SUB_OFFENSIVE_RETREAT_BEFORE_BATTLE) {
      steps.addAll(offensiveSubsSubmerge.getNames());
    }
    if (defensiveSubsSubmerge.getOrder() == SUB_DEFENSIVE_RETREAT_BEFORE_BATTLE) {
      steps.addAll(defensiveSubsSubmerge.getNames());
    }
    steps.addAll(removeUnprotectedUnits.getNames());
    steps.addAll(submergeSubsVsOnlyAir.getNames());

    if (offensiveFirstStrike.getOrder() == FIRST_STRIKE_OFFENSIVE) {
      steps.addAll(offensiveFirstStrike.getNames());
    }
    if (defensiveFirstStrike.getOrder() == FIRST_STRIKE_DEFENSIVE) {
      steps.addAll(defensiveFirstStrike.getNames());
    }
    steps.addAll(firstStrikeCasualties.getNames());

    if (offensiveFirstStrike.getOrder() == FIRST_STRIKE_OFFENSIVE_REGULAR) {
      steps.addAll(offensiveFirstStrike.getNames());
    }
    steps.addAll(airAttackVsNonSubs.getNames());
    steps.addAll(offensiveStandard.getNames());

    if (defensiveFirstStrike.getOrder() == FIRST_STRIKE_DEFENSIVE_REGULAR) {
      steps.addAll(defensiveFirstStrike.getNames());
    }
    steps.addAll(airDefendVsNonSubs.getNames());
    steps.addAll(defensiveStandard.getNames());

    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat attacking subs
    if (offensiveSubsSubmerge.getOrder() == SUB_OFFENSIVE_RETREAT_AFTER_BATTLE) {
      steps.addAll(offensiveSubsSubmerge.getNames());
    }
    steps.addAll(generalRetreat.getNames());
    if (defensiveSubsSubmerge.getOrder() == SUB_DEFENSIVE_RETREAT_AFTER_BATTLE) {
      steps.addAll(defensiveSubsSubmerge.getNames());
    }
    return steps;
  }
}
