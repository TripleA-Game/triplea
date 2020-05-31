package games.strategy.triplea.delegate.battle.steps;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.battle.BattleStepStrings;
import games.strategy.triplea.delegate.battle.MustFightBattle.ReturnFire;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import lombok.Builder;
import lombok.NonNull;
import org.triplea.java.collections.CollectionUtils;

/** Get the steps that will occurr in the battle */
@Builder
public class BattleSteps implements BattleStepStrings {

  final @NonNull Boolean canFireOffensiveAa;
  final @NonNull Boolean canFireDefendingAa;
  final @NonNull Boolean showFirstRun;
  final @NonNull GamePlayer attacker;
  final @NonNull GamePlayer defender;
  final @NonNull Collection<Unit> offensiveAa;
  final @NonNull Collection<Unit> defendingAa;
  final @NonNull Collection<Unit> attackingUnits;
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
  final @NonNull BiFunction<GamePlayer, Collection<Unit>, Collection<Territory>>
      getEmptyOrFriendlySeaNeighbors;

  public List<String> get() {

    final List<String> steps = new ArrayList<>();
    if (canFireOffensiveAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(offensiveAa)) {
        steps.add(attacker.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(defender.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(defender.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
    }
    if (canFireDefendingAa) {
      for (final String typeAa : UnitAttachment.getAllOfTypeAas(defendingAa)) {
        steps.add(defender.getName() + " " + typeAa + AA_GUNS_FIRE_SUFFIX);
        steps.add(attacker.getName() + SELECT_PREFIX + typeAa + CASUALTIES_SUFFIX);
        steps.add(attacker.getName() + REMOVE_PREFIX + typeAa + CASUALTIES_SUFFIX);
      }
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
    if (isBattleSiteWater && Properties.getTransportCasualtiesRestricted(gameData)) {
      if (attackingUnits.stream().anyMatch(Matches.unitIsTransport())
          || defendingUnits.stream().anyMatch(Matches.unitIsTransport())) {
        steps.add(REMOVE_UNESCORTED_TRANSPORTS);
      }
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
    // Air units can't attack subs without Destroyers present
    if (attackingUnits.stream().anyMatch(Matches.unitIsAir())
        && defendingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(defendingUnits, attackingUnits)) {
      steps.add(SUBMERGE_SUBS_VS_AIR_ONLY);
      steps.add(AIR_ATTACK_NON_SUBS);
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
    // Air Units can't attack subs without Destroyers present
    if (defendingUnits.stream().anyMatch(Matches.unitIsAir())
        && attackingUnits.stream().anyMatch(Matches.unitCanNotBeTargetedByAll())
        && !canAirAttackSubs(attackingUnits, defendingUnitsAliveAndDamaged)) {
      steps.add(AIR_DEFEND_NON_SUBS);
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
            defender,
            defendingUnits,
            gameData,
            getEmptyOrFriendlySeaNeighbors)) {
          steps.add(defender.getName() + SUBS_WITHDRAW);
        }
      }
    }
    return steps;
  }

  private boolean canAirAttackSubs(final Collection<Unit> firedAt, final Collection<Unit> firing) {
    return firedAt.stream().noneMatch(Matches.unitCanNotBeTargetedByAll())
        || firing.stream().anyMatch(Matches.unitIsDestroyer());
  }
}
