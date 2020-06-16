package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleActions;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.DefensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.aa.OffensiveAaFire;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirAttackVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.fire.air.AirDefendVsNonSubsStep;
import games.strategy.triplea.delegate.battle.steps.retreat.sub.SubmergeSubsVsOnlyAirStep;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

/** Get the steps that will occurr in the battle */
@Builder
public class BattleSteps implements BattleStepStrings, BattleState {

  final @NonNull Boolean showFirstRun;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer attacker;

  @Getter(onMethod = @__({@Override}))
  final @NonNull GamePlayer defender;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> offensiveAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingAa;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> attackingUnits;

  @Getter(onMethod = @__({@Override}))
  final @NonNull Collection<Unit> defendingUnits;

  final @NonNull Collection<Unit> attackingWaitingToDie;
  final @NonNull Collection<Unit> defendingWaitingToDie;
  final @NonNull Territory battleSite;
  final @NonNull GameData gameData;
  final @NonNull Collection<Unit> bombardingUnits;
  final @NonNull Function<Collection<Unit>, Collection<Unit>> getDependentUnits;
  final @NonNull Boolean isBattleSiteWater;
  final @NonNull Boolean isAmphibious;
  final @NonNull Supplier<Collection<Territory>> getAttackerRetreatTerritories;
  final @NonNull Function<Collection<Unit>, Collection<Territory>> getEmptyOrFriendlySeaNeighbors;
  final @NonNull BattleActions battleActions;

  public List<String> get() {

    final BattleStep offensiveAaStep = new OffensiveAaFire(this, battleActions);
    final BattleStep defensiveAaStep = new DefensiveAaFire(this, battleActions);
    final BattleStep submergeSubsVsOnlyAir = new SubmergeSubsVsOnlyAirStep(this, battleActions);
    final BattleStep airAttackVsNonSubs = new AirAttackVsNonSubsStep(this);
    final BattleStep airDefendVsNonSubs = new AirDefendVsNonSubsStep(this);

    final List<String> steps = new ArrayList<>();
    if (offensiveAaStep.getOrder() != BattleStep.Order.NOT_APPLICABLE) {
      steps.addAll(offensiveAaStep.getNames());
    }
    if (defensiveAaStep.getOrder() != BattleStep.Order.NOT_APPLICABLE) {
      steps.addAll(defensiveAaStep.getNames());
    }

    if (showFirstRun) {
      if (!isBattleSiteWater && !bombardingUnits.isEmpty()) {
        steps.add(NAVAL_BOMBARDMENT);
        steps.add(SELECT_NAVAL_BOMBARDMENT_CASUALTIES);
      }
      if (!isBattleSiteWater && TechAttachment.isAirTransportable(attacker)) {
        final Collection<Unit> bombers =
            CollectionUtils.getMatches(battleSite.getUnits(), Matches.unitIsAirTransport());
        if (!bombers.isEmpty()) {
          final Collection<Unit> dependents = getDependentUnits.apply(bombers);
          if (!dependents.isEmpty()) {
            steps.add(LAND_PARATROOPS);
          }
        }
      }
    }
    // Check if defending subs can submerge before battle
    if (Properties.getSubRetreatBeforeBattle(gameData)) {
      if (defendingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(attacker.getName() + SUBS_SUBMERGE);
      }
      if (attackingUnits.stream().noneMatch(Matches.unitIsDestroyer())
          && defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
        steps.add(defender.getName() + SUBS_SUBMERGE);
      }
    }
    // See if there any unescorted transports
    if (isBattleSiteWater
        && Properties.getTransportCasualtiesRestricted(gameData)
        && (attackingUnits.stream().anyMatch(Matches.unitIsTransport())
            || defendingUnits.stream().anyMatch(Matches.unitIsTransport()))) {
      steps.add(REMOVE_UNESCORTED_TRANSPORTS);
    }
    if (submergeSubsVsOnlyAir.getOrder() != BattleStep.Order.NOT_APPLICABLE) {
      steps.addAll(submergeSubsVsOnlyAir.getNames());
    }

    final boolean defenderSubsFireFirst =
        SubsChecks.defenderSubsFireFirst(attackingUnits, defendingUnits, gameData);
    final ReturnFire returnFireAgainstAttackingSubs =
        SubsChecks.returnFireAgainstAttackingSubs(attackingUnits, defendingUnits, gameData);
    final ReturnFire returnFireAgainstDefendingSubs =
        SubsChecks.returnFireAgainstDefendingSubs(attackingUnits, defendingUnits, gameData);
    // if attacker has no sneak attack subs, then defender sneak attack subs fire first and remove
    // casualties
    if (defenderSubsFireFirst && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }
    final boolean onlyAttackerSneakAttack =
        !defenderSubsFireFirst
            && returnFireAgainstAttackingSubs == ReturnFire.NONE
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;
    // attacker subs sneak attack, no sneak attack if destroyers are present
    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(attacker.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(defender.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
      if (onlyAttackerSneakAttack) {
        steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
      }
    }
    // ww2v2 rules, all subs fire FIRST in combat, regardless of presence of destroyers.
    final boolean defendingSubsFireWithAllDefenders =
        !defenderSubsFireFirst
            && !Properties.getWW2V2(gameData)
            && returnFireAgainstDefendingSubs == ReturnFire.ALL;
    // defender subs sneak attack, no sneak attack in Pacific/Europe Theaters or if destroyers are
    // present
    final boolean defendingSubsFireWithAllDefendersAlways =
        !SubsChecks.defendingSubsSneakAttack(gameData);
    if (!defendingSubsFireWithAllDefendersAlways
        && !defendingSubsFireWithAllDefenders
        && !defenderSubsFireFirst
        && defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike())) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    if ((attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike())
            || defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike()))
        && !defenderSubsFireFirst
        && !onlyAttackerSneakAttack
        && (returnFireAgainstDefendingSubs != ReturnFire.ALL
            || returnFireAgainstAttackingSubs != ReturnFire.ALL)) {
      steps.add(REMOVE_SNEAK_ATTACK_CASUALTIES);
    }

    if (airAttackVsNonSubs.getOrder() != BattleStep.Order.NOT_APPLICABLE) {
      steps.addAll(airAttackVsNonSubs.getNames());
    }

    if (attackingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(attacker.getName() + FIRE);
      steps.add(defender.getName() + SELECT_CASUALTIES);
    }
    // classic rules, subs fire with all defenders
    // also, ww2v3/global rules, defending subs without sneak attack fire with all defenders
    final Collection<Unit> defendingUnitsAliveAndDamaged = new ArrayList<>(defendingUnits);
    defendingUnitsAliveAndDamaged.addAll(defendingWaitingToDie);
    // TODO: BUG? why is unitCanNotTargetAll used instead of unitIsFirstStrike?
    if (defendingUnitsAliveAndDamaged.stream().anyMatch(Matches.unitCanNotTargetAll())
        && !defenderSubsFireFirst
        && (defendingSubsFireWithAllDefenders || defendingSubsFireWithAllDefendersAlways)) {
      steps.add(defender.getName() + FIRST_STRIKE_UNITS_FIRE);
      steps.add(attacker.getName() + SELECT_FIRST_STRIKE_CASUALTIES);
    }
    if (airDefendVsNonSubs.getOrder() != BattleStep.Order.NOT_APPLICABLE) {
      steps.addAll(airDefendVsNonSubs.getNames());
    }
    if (defendingUnits.stream().anyMatch(Matches.unitIsFirstStrike().negate())) {
      steps.add(defender.getName() + FIRE);
      steps.add(attacker.getName() + SELECT_CASUALTIES);
    }
    // remove casualties
    steps.add(REMOVE_CASUALTIES);
    // retreat attacking subs
    if (attackingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (Properties.getSubmersibleSubs(gameData)) {
        // TODO: BUG? Should the presence of destroyers be checked?
        if (!Properties.getSubRetreatBeforeBattle(gameData)) {
          steps.add(attacker.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (RetreatChecks.canAttackerRetreatSubs(
            defendingUnits,
            defendingWaitingToDie,
            gameData,
            getAttackerRetreatTerritories,
            isAmphibious)) {
          steps.add(attacker.getName() + SUBS_WITHDRAW);
        }
      }
    }
    // if we are a sea zone, then we may not be able to retreat
    // (ie a sub traveled under another unit to get to the battle site)
    // or an enemy sub retreated to our sea zone
    // however, if all our sea units die, then the air units can still retreat, so if we have any
    // air units attacking in
    // a sea zone, we always have to have the retreat option shown
    // later, if our sea units die, we may ask the user to retreat
    final boolean someAirAtSea =
        isBattleSiteWater && attackingUnits.stream().anyMatch(Matches.unitIsAir());
    if (RetreatChecks.canAttackerRetreat(
            defendingUnits, gameData, getAttackerRetreatTerritories, isAmphibious)
        || someAirAtSea
        || RetreatChecks.canAttackerRetreatPartialAmphib(attackingUnits, gameData, isAmphibious)
        || RetreatChecks.canAttackerRetreatPlanes(attackingUnits, gameData, isAmphibious)) {
      steps.add(attacker.getName() + ATTACKER_WITHDRAW);
    }
    // retreat defending subs
    if (defendingUnits.stream().anyMatch(Matches.unitCanEvade())) {
      if (Properties.getSubmersibleSubs(gameData)) {
        // TODO: BUG? Should the presence of destroyers be checked?
        if (!Properties.getSubRetreatBeforeBattle(gameData)) {
          steps.add(defender.getName() + SUBS_SUBMERGE);
        }
      } else {
        if (RetreatChecks.canDefenderRetreatSubs(
            attackingUnits,
            attackingWaitingToDie,
            defendingUnits,
            gameData,
            getEmptyOrFriendlySeaNeighbors)) {
          steps.add(defender.getName() + SUBS_WITHDRAW);
        }
      }
    }
    return steps;
  }
}
