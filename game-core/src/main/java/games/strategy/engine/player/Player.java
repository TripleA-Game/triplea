package games.strategy.engine.player;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.engine.message.IRemote;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.data.CasualtyList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/**
 * Used for both IRemotePlayer (used by the server, etc.) and specific game players such as
 * IRemotePlayer and IGridGamePlayer (used by delegates for communication, etc.).
 */
public interface Player extends IRemote {
  /**
   * Returns the id of this player. This id is initialized by the initialize method in
   * IRemotePlayer.
   */
  PlayerId getPlayerId();

  /** Called before the game starts. */
  void initialize(IPlayerBridge bridge, PlayerId id);

  /** Returns the nation name. */
  String getName();

  PlayerType getPlayerType();

  /**
   * Start the given step. stepName appears as it does in the game xml file. The game step will
   * finish executing when this method returns.
   */
  void start(String stepName);

  /** Called when the game is stopped (like if we are closing the window or leaving the game). */
  void stopGame();

  /**
   * Select casualties.
   *
   * @param selectFrom - the units to select casualties from
   * @param dependents - dependents of the units to select from
   * @param count - the number of casualties to select
   * @param message - ui message to display
   * @param dice - the dice rolled for the casualties
   * @param hit - the player hit
   * @param friendlyUnits - all friendly units in the battle (or moving)
   * @param enemyUnits - all enemy units in the battle (or defending aa)
   * @param amphibious - is the battle amphibious?
   * @param amphibiousLandAttackers - can be null
   * @param defaultCasualties - default casualties as selected by the game
   * @param battleId - the battle we are fighting in, may be null if this is an aa casualty
   *     selection during a move
   * @param battlesite - the territory where this happened
   * @param allowMultipleHitsPerUnit - can units be hit more than one time if they have more than
   *     one hitpoints left?
   * @return CasualtyDetails
   */
  CasualtyDetails selectCasualties(
      Collection<Unit> selectFrom,
      Map<Unit, Collection<Unit>> dependents,
      int count,
      String message,
      DiceRoll dice,
      PlayerId hit,
      Collection<Unit> friendlyUnits,
      Collection<Unit> enemyUnits,
      boolean amphibious,
      Collection<Unit> amphibiousLandAttackers,
      CasualtyList defaultCasualties,
      GUID battleId,
      Territory battlesite,
      boolean allowMultipleHitsPerUnit);

  /**
   * Select a fixed dice roll.
   *
   * @param numDice - the number of dice rolls
   * @param hitAt - the roll value that constitutes a hit
   * @param title - the title for the DiceChooser
   * @param diceSides - the number of sides on the die, found by data.getDiceSides()
   * @return the resulting dice array
   */
  int[] selectFixedDice(int numDice, int hitAt, String title, int diceSides);

  // TODO: Remove noneAvailable as it is always passed as 'true'
  /**
   * Select the territory to bombard with the bombarding capable unit (eg battleship).
   *
   * @param unit - the bombarding unit
   * @param unitTerritory - where the bombarding unit is
   * @param territories - territories where the unit can bombard
   * @return the Territory to bombard in, null if the unit should not bombard
   */
  Territory selectBombardingTerritory(
      Unit unit, Territory unitTerritory, Collection<Territory> territories, boolean noneAvailable);

  /**
   * Ask if the player wants to attack lone subs.
   *
   * @param unitTerritory - where the potential battle is
   */
  boolean selectAttackSubs(Territory unitTerritory);

  /**
   * Ask if the player wants to attack lone transports.
   *
   * @param unitTerritory - where the potential battle is
   */
  boolean selectAttackTransports(Territory unitTerritory);

  /**
   * Ask if the player wants to attack units.
   *
   * @param unitTerritory - where the potential battle is
   */
  boolean selectAttackUnits(Territory unitTerritory);

  /**
   * Ask if the player wants to shore bombard.
   *
   * @param unitTerritory - where the potential battle is
   */
  boolean selectShoreBombard(Territory unitTerritory);

  // TODO: this is only called from BattleCalculator.selectCasualties() and should probably be
  // removed
  /**
   * Report an error to the user.
   *
   * @param error that an error occurred
   */
  void reportError(String error);

  /** report a message to the user. */
  void reportMessage(String message, String title);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing raid can be
   * conducted, should the bomber bomb.
   */
  boolean shouldBomberBomb(Territory territory);

  /**
   * One or more bombers have just moved into a territory where a strategic bombing raid can be
   * conducted, what should the bomber bomb.
   */
  Unit whatShouldBomberBomb(
      Territory territory, Collection<Unit> potentialTargets, Collection<Unit> bombers);

  /**
   * Choose where my rockets should fire.
   *
   * @param candidates - a collection of Territories, the possible territories to attack
   * @param from - where the rockets are launched from, null for WW2V1 rules
   * @return the territory to attack, null if no territory should be attacked
   */
  Territory whereShouldRocketsAttack(Collection<Territory> candidates, Territory from);

  /**
   * Get the fighters to move to a newly produced carrier.
   *
   * @param fightersThatCanBeMoved - the fighters that can be moved
   * @param from - the territory containing the factory
   * @return - the fighters to move
   */
  Collection<Unit> getNumberOfFightersToMoveToNewCarrier(
      Collection<Unit> fightersThatCanBeMoved, Territory from);

  /**
   * Some carriers were lost while defending. We must select where to land some air units.
   *
   * @param candidates - a list of territories - these are the places where air units can land
   * @return - the territory to land the fighters in, must be non null
   */
  Territory selectTerritoryForAirToLand(
      Collection<Territory> candidates, Territory currentTerritory, String unitMessage);

  /**
   * The attempted move will incur aa fire, confirm that you still want to move.
   *
   * @param aaFiringTerritories - the territories where aa will fire
   */
  boolean confirmMoveInFaceOfAa(Collection<Territory> aaFiringTerritories);

  /** The attempted move will kill some air units. */
  boolean confirmMoveKamikaze();

  /** The attempted move will kill some units. */
  void confirmMoveHariKari();

  /**
   * Ask the player if he wishes to retreat.
   *
   * @param battleId - the battle
   * @param submerge - is submerging possible (means the retreat territory CAN be the current battle
   *     territory)
   * @param possibleTerritories - where the player can retreat to
   * @param message - user displayable message
   * @return the territory to retreat to, or null if the player doesnt wish to retreat
   */
  Territory retreatQuery(
      GUID battleId,
      boolean submerge,
      Territory battleTerritory,
      Collection<Territory> possibleTerritories,
      String message);

  /**
   * Ask the player which units, if any, they want to scramble to defend against the attacker.
   *
   * @param scrambleTo - the territory we are scrambling to defend in, where the units will end up
   *     if scrambled
   * @param possibleScramblers possible units which we could scramble, with where they are from and
   *     how many allowed from that location
   * @return a list of units to scramble mapped to where they are coming from
   */
  Map<Territory, Collection<Unit>> scrambleUnitsQuery(
      Territory scrambleTo,
      Map<Territory, Tuple<Collection<Unit>, Collection<Unit>>> possibleScramblers);

  /** Ask the player which if any units they want to select. */
  Collection<Unit> selectUnitsQuery(Territory current, Collection<Unit> possible, String message);

  /** Allows the user to pause and confirm enemy casualties. */
  void confirmEnemyCasualties(GUID battleId, String message, PlayerId hitPlayer);

  void confirmOwnCasualties(GUID battleId, String message);

  /**
   * Indicates the player accepts the proposed action.
   *
   * @param acceptanceQuestion the question that should be asked to this player
   * @param politics is this from politics delegate?
   * @return whether the player accepts the action proposal
   */
  boolean acceptAction(PlayerId playerSendingProposal, String acceptanceQuestion, boolean politics);

  /** Asks the player if they wish to perform any kamikaze suicide attacks. */
  Map<Territory, Map<Unit, IntegerMap<Resource>>> selectKamikazeSuicideAttacks(
      Map<Territory, Collection<Unit>> possibleUnitsToAttack);

  /**
   * Used during the RandomStartDelegate for assigning territories to players, and units to
   * territories.
   */
  Tuple<Territory, Set<Unit>> pickTerritoryAndUnits(
      List<Territory> territoryChoices, List<Unit> unitChoices, int unitsPerPick);
}
