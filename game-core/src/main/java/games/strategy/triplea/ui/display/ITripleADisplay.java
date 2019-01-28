package games.strategy.triplea.ui.display;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.display.IDisplay;
import games.strategy.net.GUID;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.Die;
import games.strategy.triplea.delegate.IBattle.BattleType;

/**
 * Extension of {@link IDisplay} that expands the set of game events that may be displayed to a game player.
 */
public interface ITripleADisplay extends IDisplay {
  /**
   * Sends a message to all TripleAFrame that have joined the game, possibly including observers.
   */
  void reportMessageToAll(String message, String title, boolean doNotIncludeHost,
      boolean doNotIncludeClients, boolean doNotIncludeObservers);

  /**
   * Sends a message to all TripleAFrame's that are playing AND are controlling one or more of the players listed but
   * NOT any of the players listed as butNotThesePlayers. (No message to any observers or players not in the list.)
   */
  void reportMessageToPlayers(Collection<PlayerId> playersToSendTo,
      Collection<PlayerId> butNotThesePlayers, String message, String title);

  /**
   * Display info about the battle. This is the first message to be displayed in a battle.
   *
   * @param battleId - a unique id for the battle
   * @param location - where the battle occurs
   * @param battleTitle - the title of the battle
   * @param attackingUnits - attacking units
   * @param defendingUnits - defending units
   * @param killedUnits - killed units
   * @param dependentUnits - unit dependencies, maps Unit->Collection of units
   * @param attacker - PlayerId of attacker
   * @param defender - PlayerId of defender
   */
  void showBattle(GUID battleId, Territory location, String battleTitle, Collection<Unit> attackingUnits,
      Collection<Unit> defendingUnits, Collection<Unit> killedUnits, Collection<Unit> attackingWaitingToDie,
      Collection<Unit> defendingWaitingToDie, Map<Unit, Collection<Unit>> dependentUnits, PlayerId attacker,
      PlayerId defender, boolean isAmphibious, BattleType battleType, Collection<Unit> amphibiousLandAttackers);

  /**
   * Displays the steps for the specified battle.
   *
   * @param battleId - the battle we are listing steps for.
   * @param steps - a collection of strings denoting all steps in the battle
   */
  void listBattleSteps(GUID battleId, List<String> steps);

  /**
   * The given battle has ended.
   */
  void battleEnd(GUID battleId, String message);

  /**
   * Notify that the casualties occurred.
   */
  void casualtyNotification(GUID battleId, String step, DiceRoll dice, PlayerId player, Collection<Unit> killed,
      Collection<Unit> damaged, Map<Unit, Collection<Unit>> dependents);

  /**
   * Notify that the casualties occurred, and only the casualty.
   */
  void deadUnitNotification(GUID battleId, PlayerId player, Collection<Unit> dead,
      Map<Unit, Collection<Unit>> dependents);

  void changedUnitsNotification(GUID battleId, PlayerId player, Collection<Unit> removedUnits,
      Collection<Unit> addedUnits, Map<Unit, Collection<Unit>> dependents);

  /**
   * Notification of the results of a bombing raid.
   */
  void bombingResults(GUID battleId, List<Die> dice, int cost);

  /**
   * Notify that the given player has retreated some or all of his units.
   */
  void notifyRetreat(String shortMessage, String message, String step, PlayerId retreatingPlayer);

  void notifyRetreat(GUID battleId, Collection<Unit> retreating);

  /**
   * Show dice for the given battle and step.
   */
  void notifyDice(DiceRoll dice, String stepName);

  void gotoBattleStep(GUID battleId, String step);
}
