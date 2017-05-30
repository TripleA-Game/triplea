package games.strategy.triplea.oddsCalculator.ta;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.Callable;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHitsChange;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.engine.display.IDisplay;
import games.strategy.engine.framework.GameDataUtils;
import games.strategy.engine.framework.IGameModifiedChannel;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.engine.random.PlainRandomSource;
import games.strategy.net.GUID;
import games.strategy.sound.HeadlessSoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ai.AIUtils;
import games.strategy.triplea.ai.AbstractAI;
import games.strategy.triplea.delegate.BattleTracker;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.GameDelegateBridge;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MustFightBattle;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.delegate.dataObjects.CasualtyList;
import games.strategy.triplea.delegate.remote.IAbstractPlaceDelegate;
import games.strategy.triplea.delegate.remote.IMoveDelegate;
import games.strategy.triplea.delegate.remote.IPurchaseDelegate;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.triplea.ui.display.HeadlessDisplay;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.util.CompositeMatch;
import games.strategy.util.CompositeMatchAnd;
import games.strategy.util.Match;
import games.strategy.util.Tuple;

public class OddsCalculator implements IOddsCalculator, Callable<AggregateResults> {
  public static final String OOL_ALL = "*";
  public static final String OOL_ALL_REGEX = "\\*";
  public static final String OOL_SEPARATOR = ";";
  public static final String OOL_SEPARATOR_REGEX = ";";
  public static final String OOL_AMOUNT_DESCRIPTOR = "^";
  public static final String OOL_AMOUNT_DESCRIPTOR_REGEX = "\\^";
  private GameData m_data = null;
  private PlayerID m_attacker = null;
  private PlayerID m_defender = null;
  private Territory m_location = null;
  private Collection<Unit> m_attackingUnits = new ArrayList<>();
  private Collection<Unit> m_defendingUnits = new ArrayList<>();
  private Collection<Unit> m_bombardingUnits = new ArrayList<>();
  private Collection<TerritoryEffect> m_territoryEffects = new ArrayList<>();
  private boolean m_keepOneAttackingLandUnit = false;
  private boolean m_amphibious = false;
  private int m_retreatAfterRound = -1;
  private int m_retreatAfterXUnitsLeft = -1;
  private boolean m_retreatWhenOnlyAirLeft = false;
  private String m_attackerOrderOfLosses = null;
  private String m_defenderOrderOfLosses = null;
  private int m_runCount = 0;
  private volatile boolean m_cancelled = false;
  private volatile boolean m_isDataSet = false;
  private volatile boolean m_isCalcSet = false;
  private volatile boolean m_isRunning = false;
  private final List<OddsCalculatorListener> m_listeners = new ArrayList<>();

  public OddsCalculator(final GameData data) {
    this(data, false);
  }

  OddsCalculator(final GameData data, final boolean dataHasAlreadyBeenCloned) {
    m_data = data == null ? null : (dataHasAlreadyBeenCloned ? data : GameDataUtils.cloneGameData(data, false));
    if (data != null) {
      m_isDataSet = true;
      notifyListenersGameDataIsSet();
    }
  }

  @Override
  public void setGameData(final GameData data) {
    if (m_isRunning) {
      return;
    }
    m_isDataSet = false;
    m_isCalcSet = false;
    m_data = (data == null ? null : GameDataUtils.cloneGameData(data, false));
    // reset old data
    m_attacker = null;
    m_defender = null;
    m_location = null;
    m_attackingUnits = new ArrayList<>();
    m_defendingUnits = new ArrayList<>();
    m_bombardingUnits = new ArrayList<>();
    m_territoryEffects = new ArrayList<>();
    m_runCount = 0;
    if (data != null) {
      m_isDataSet = true;
      notifyListenersGameDataIsSet();
    }
  }

  /**
   * Calculates odds using the stored game data.
   */
  @Override
  @SuppressWarnings("unchecked")
  public void setCalculateData(final PlayerID attacker, final PlayerID defender, final Territory location,
      final Collection<Unit> attacking, final Collection<Unit> defending, final Collection<Unit> bombarding,
      final Collection<TerritoryEffect> territoryEffects, final int runCount) throws IllegalStateException {
    if (m_isRunning) {
      return;
    }
    m_isCalcSet = false;
    if (!m_isDataSet) {
      throw new IllegalStateException("Called set calculation before setting game data!");
    }
    m_attacker =
        m_data.getPlayerList().getPlayerID((attacker == null ? PlayerID.NULL_PLAYERID.getName() : attacker.getName()));
    m_defender =
        m_data.getPlayerList().getPlayerID((defender == null ? PlayerID.NULL_PLAYERID.getName() : defender.getName()));
    m_location = m_data.getMap().getTerritory(location.getName());
    m_attackingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(attacking, m_data);
    m_defendingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(defending, m_data);
    m_bombardingUnits = (Collection<Unit>) GameDataUtils.translateIntoOtherGameData(bombarding, m_data);
    m_territoryEffects =
        (Collection<TerritoryEffect>) GameDataUtils.translateIntoOtherGameData(territoryEffects, m_data);
    m_data.performChange(ChangeFactory.removeUnits(m_location, m_location.getUnits().getUnits()));
    m_data.performChange(ChangeFactory.addUnits(m_location, m_attackingUnits));
    m_data.performChange(ChangeFactory.addUnits(m_location, m_defendingUnits));
    m_runCount = runCount;
    m_isCalcSet = true;
  }

  @Override
  public AggregateResults setCalculateDataAndCalculate(final PlayerID attacker, final PlayerID defender,
      final Territory location, final Collection<Unit> attacking, final Collection<Unit> defending,
      final Collection<Unit> bombarding, final Collection<TerritoryEffect> territoryEffects, final int runCount) {
    setCalculateData(attacker, defender, location, attacking, defending, bombarding, territoryEffects, runCount);
    return calculate();
  }

  @Override
  public AggregateResults calculate() {
    if (!getIsReady()) {
      throw new IllegalStateException("Called calculate before setting calculate data!");
    }
    return calculate(m_runCount);
  }

  @Override
  public AggregateResults call() throws Exception {
    return calculate();
  }

  @Override
  public boolean getIsReady() {
    return m_isDataSet && m_isCalcSet;
  }

  @Override
  public int getRunCount() {
    return m_runCount;
  }

  @Override
  public void setKeepOneAttackingLandUnit(final boolean bool) {
    m_keepOneAttackingLandUnit = bool;
  }

  @Override
  public void setAmphibious(final boolean bool) {
    m_amphibious = bool;
  }

  @Override
  public void setRetreatAfterRound(final int value) {
    m_retreatAfterRound = value;
  }

  @Override
  public void setRetreatAfterXUnitsLeft(final int value) {
    m_retreatAfterXUnitsLeft = value;
  }

  @Override
  public void setRetreatWhenOnlyAirLeft(final boolean value) {
    m_retreatWhenOnlyAirLeft = value;
  }

  @Override
  public void setAttackerOrderOfLosses(final String attackerOrderOfLosses) {
    m_attackerOrderOfLosses = attackerOrderOfLosses;
  }

  @Override
  public void setDefenderOrderOfLosses(final String defenderOrderOfLosses) {
    m_defenderOrderOfLosses = defenderOrderOfLosses;
  }

  @Override
  public void cancel() {
    m_cancelled = true;
  }

  @Override
  public void shutdown() {
    cancel();
    synchronized (m_listeners) {
      m_listeners.clear();
    }
  }

  @Override
  public int getThreadCount() {
    return 1;
  }

  private AggregateResults calculate(final int count) {
    m_isRunning = true;
    final long start = System.currentTimeMillis();
    final AggregateResults rVal = new AggregateResults(count);
    final BattleTracker battleTracker = new BattleTracker();
    // CasualtySortingCaching can cause issues if there is more than 1 one battle being calced at the same time (like if
    // the AI and a human
    // are both using the calc)
    // TODO: first, see how much it actually speeds stuff up by, and if it does make a difference then convert it to a
    // per-thread, per-calc
    // caching
    final List<Unit> attackerOrderOfLosses =
        OddsCalculator.getUnitListByOrderOfLoss(m_attackerOrderOfLosses, m_attackingUnits, m_data);
    final List<Unit> defenderOrderOfLosses =
        OddsCalculator.getUnitListByOrderOfLoss(m_defenderOrderOfLosses, m_defendingUnits, m_data);
    for (int i = 0; i < count && !m_cancelled; i++) {
      final CompositeChange allChanges = new CompositeChange();
      final DummyDelegateBridge bridge1 =
          new DummyDelegateBridge(m_attacker, m_data, allChanges, attackerOrderOfLosses, defenderOrderOfLosses,
              m_keepOneAttackingLandUnit, m_retreatAfterRound, m_retreatAfterXUnitsLeft, m_retreatWhenOnlyAirLeft);
      final GameDelegateBridge bridge = new GameDelegateBridge(bridge1);
      final MustFightBattle battle = new MustFightBattle(m_location, m_attacker, m_data, battleTracker);
      battle.setHeadless(true);
      battle.isAmphibious();
      battle.setUnits(m_defendingUnits, m_attackingUnits, m_bombardingUnits,
          (m_amphibious ? m_attackingUnits : new ArrayList<>()), m_defender, m_territoryEffects);
      // battle.setAttackingFromAndMap(attackingFromMap);
      bridge1.setBattle(battle);
      battle.fight(bridge);
      rVal.addResult(new BattleResults(battle, m_data));
      // restore the game to its original state
      m_data.performChange(allChanges.invert());
      battleTracker.clear();
      battleTracker.clearBattleRecords();
    }
    // BattleCalculator.DisableCasualtySortingCaching();
    rVal.setTime(System.currentTimeMillis() - start);
    m_isRunning = false;
    m_cancelled = false;
    return rVal;
  }

  static boolean isValidOrderOfLoss(final String orderOfLoss, final GameData data) {
    if (orderOfLoss == null || orderOfLoss.trim().length() == 0) {
      return true;
    }
    try {
      final String[] sections;
      if (orderOfLoss.contains(OOL_SEPARATOR)) {
        sections = orderOfLoss.trim().split(OOL_SEPARATOR_REGEX);
      } else {
        sections = new String[1];
        sections[0] = orderOfLoss.trim();
      }
      final UnitTypeList unitTypes;
      try {
        data.acquireReadLock();
        unitTypes = data.getUnitTypeList();
      } finally {
        data.releaseReadLock();
      }
      for (final String section : sections) {
        if (section.length() == 0) {
          continue;
        }
        final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
        if (amountThenType.length != 2) {
          return false;
        }
        if (!amountThenType[0].equals(OOL_ALL)) {
          final int amount = Integer.parseInt(amountThenType[0]);
          if (amount <= 0) {
            return false;
          }
        }
        final UnitType type = unitTypes.getUnitType(amountThenType[1]);
        if (type == null) {
          return false;
        }
      }
    } catch (final Exception e) {
      return false;
    }
    return true;
  }

  private static List<Unit> getUnitListByOrderOfLoss(final String ool, final Collection<Unit> units,
      final GameData data) {
    if (ool == null || ool.trim().length() == 0) {
      return null;
    }
    final List<Tuple<Integer, UnitType>> map = new ArrayList<>();
    final String[] sections;
    if (ool.contains(OOL_SEPARATOR)) {
      sections = ool.trim().split(OOL_SEPARATOR_REGEX);
    } else {
      sections = new String[1];
      sections[0] = ool.trim();
    }
    for (final String section : sections) {
      if (section.length() == 0) {
        continue;
      }
      final String[] amountThenType = section.split(OOL_AMOUNT_DESCRIPTOR_REGEX);
      final int amount = amountThenType[0].equals(OOL_ALL) ? Integer.MAX_VALUE : Integer.parseInt(amountThenType[0]);
      final UnitType type = data.getUnitTypeList().getUnitType(amountThenType[1]);
      map.add(Tuple.of(amount, type));
    }
    Collections.reverse(map);
    final Set<Unit> unitsLeft = new HashSet<>(units);
    final List<Unit> order = new ArrayList<>();
    for (final Tuple<Integer, UnitType> section : map) {
      final List<Unit> unitsOfType =
          Match.getNMatches(unitsLeft, section.getFirst(), Matches.unitIsOfType(section.getSecond()));
      order.addAll(unitsOfType);
      unitsLeft.removeAll(unitsOfType);
    }
    Collections.reverse(order);
    return order;
  }

  @Override
  public void addOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (m_listeners) {
      m_listeners.add(listener);
    }
  }

  @Override
  public void removeOddsCalculatorListener(final OddsCalculatorListener listener) {
    synchronized (m_listeners) {
      m_listeners.remove(listener);
    }
  }

  private void notifyListenersGameDataIsSet() {
    synchronized (m_listeners) {
      for (final OddsCalculatorListener listener : m_listeners) {
        listener.dataReady();
      }
    }
  }
}


class DummyDelegateBridge implements IDelegateBridge {
  private final PlainRandomSource m_randomSource = new PlainRandomSource();
  private final ITripleADisplay m_display = new HeadlessDisplay();
  private final ISound m_soundChannel = new HeadlessSoundChannel();
  private final DummyPlayer m_attackingPlayer;
  private final DummyPlayer m_defendingPlayer;
  private final PlayerID m_attacker;
  private final DelegateHistoryWriter m_writer = new DelegateHistoryWriter(new DummyGameModifiedChannel());
  private final CompositeChange m_allChanges;
  private final GameData m_data;
  private MustFightBattle m_battle = null;

  public DummyDelegateBridge(final PlayerID attacker, final GameData data, final CompositeChange allChanges,
      final List<Unit> attackerOrderOfLosses, final List<Unit> defenderOrderOfLosses,
      final boolean attackerKeepOneLandUnit, final int retreatAfterRound, final int retreatAfterXUnitsLeft,
      final boolean retreatWhenOnlyAirLeft) {
    m_attackingPlayer = new DummyPlayer(this, true, "battle calc dummy", "None (AI)", attackerOrderOfLosses,
        attackerKeepOneLandUnit, retreatAfterRound, retreatAfterXUnitsLeft, retreatWhenOnlyAirLeft);
    m_defendingPlayer = new DummyPlayer(this, false, "battle calc dummy", "None (AI)", defenderOrderOfLosses, false,
        retreatAfterRound, -1, false);
    m_data = data;
    m_attacker = attacker;
    m_allChanges = allChanges;
  }

  @Override
  public GameData getData() {
    return m_data;
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public Properties getStepProperties() {
    throw new UnsupportedOperationException();
  }

  @Override
  public String getStepName() {
    throw new UnsupportedOperationException();
  }

  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    if (id.equals(m_attacker)) {
      return m_attackingPlayer;
    } else {
      return m_defendingPlayer;
    }
  }

  @Override
  public IRemotePlayer getRemotePlayer() {
    // the current player is attacker
    return m_attackingPlayer;
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType,
      final String annotation) {
    return m_randomSource.getRandom(max, count, annotation);
  }

  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_randomSource.getRandom(max, annotation);
  }

  @Override
  public PlayerID getPlayerID() {
    return m_attacker;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_writer;
  }

  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return m_display;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return m_soundChannel;
  }

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void addChange(final Change aChange) {
    if (!(aChange instanceof UnitHitsChange)) {
      return;
    }
    m_allChanges.add(aChange);
    m_data.performChange(aChange);
  }

  @Override
  public void stopGameSequence() {}

  public MustFightBattle getBattle() {
    return m_battle;
  }

  public void setBattle(final MustFightBattle battle) {
    m_battle = battle;
  }
}


class DummyGameModifiedChannel implements IGameModifiedChannel {
  @Override
  public void addChildToEvent(final String text, final Object renderingData) {}

  @Override
  public void gameDataChanged(final Change aChange) {}

  @Override
  public void shutDown() {}

  @Override
  public void startHistoryEvent(final String event) {}

  @Override
  public void stepChanged(final String stepName, final String delegateName, final PlayerID player, final int round,
      final String displayName, final boolean loadedFromSavedGame) {}

  @Override
  public void startHistoryEvent(final String event, final Object renderingData) {}
}


class DummyPlayer extends AbstractAI {
  private final boolean m_keepAtLeastOneLand;
  // negative = do not retreat
  private final int m_retreatAfterRound;
  // negative = do not retreat
  private final int m_retreatAfterXUnitsLeft;
  private final boolean m_retreatWhenOnlyAirLeft;
  private final DummyDelegateBridge m_bridge;
  private final boolean m_isAttacker;
  private final List<Unit> m_orderOfLosses;

  public DummyPlayer(final DummyDelegateBridge dummyDelegateBridge, final boolean attacker, final String name,
      final String type, final List<Unit> orderOfLosses, final boolean keepAtLeastOneLand, final int retreatAfterRound,
      final int retreatAfterXUnitsLeft, final boolean retreatWhenOnlyAirLeft) {
    super(name, type);
    m_keepAtLeastOneLand = keepAtLeastOneLand;
    m_retreatAfterRound = retreatAfterRound;
    m_retreatAfterXUnitsLeft = retreatAfterXUnitsLeft;
    m_retreatWhenOnlyAirLeft = retreatWhenOnlyAirLeft;
    m_bridge = dummyDelegateBridge;
    m_isAttacker = attacker;
    m_orderOfLosses = orderOfLosses;
  }

  private MustFightBattle getBattle() {
    return m_bridge.getBattle();
  }

  private List<Unit> getOurUnits() {
    final MustFightBattle battle = getBattle();
    if (battle == null) {
      return null;
    }
    return new ArrayList<>((m_isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits()));
  }

  private List<Unit> getEnemyUnits() {
    final MustFightBattle battle = getBattle();
    if (battle == null) {
      return null;
    }
    return new ArrayList<>((m_isAttacker ? battle.getDefendingUnits() : battle.getAttackingUnits()));
  }

  @Override
  protected void move(final boolean nonCombat, final IMoveDelegate moveDel, final GameData data,
      final PlayerID player) {}

  @Override
  protected void place(final boolean placeForBid, final IAbstractPlaceDelegate placeDelegate, final GameData data,
      final PlayerID player) {}

  @Override
  protected void purchase(final boolean purcahseForBid, final int PUsToSpend, final IPurchaseDelegate purchaseDelegate,
      final GameData data, final PlayerID player) {}

  @Override
  protected void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {}

  @Override
  public boolean confirmMoveInFaceOfAA(final Collection<Territory> aaFiringTerritories) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Collection<Unit> getNumberOfFightersToMoveToNewCarrier(final Collection<Unit> fightersThatCanBeMoved,
      final Territory from) {
    throw new UnsupportedOperationException();
  }

  /**
   * The battle calc doesn't actually care if you have available territories to retreat to or not.
   * It will always let you retreat to the 'current' territory (the battle territory), even if that is illegal.
   * This is because the battle calc does not know where the attackers are actually coming from.
   */
  @Override
  public Territory retreatQuery(final GUID battleID, final boolean submerge, final Territory battleSite,
      final Collection<Territory> possibleTerritories, final String message) {
    // null = do not retreat
    if (possibleTerritories.isEmpty()) {
      return null;
    }
    if (submerge) {
      // submerge if all air vs subs
      final CompositeMatch<Unit> seaSub = new CompositeMatchAnd<>(Matches.UnitIsSea, Matches.UnitIsSub);
      final CompositeMatch<Unit> planeNotDestroyer =
          new CompositeMatchAnd<>(Matches.UnitIsAir, Matches.UnitIsDestroyer.invert());
      final List<Unit> ourUnits = getOurUnits();
      final List<Unit> enemyUnits = getEnemyUnits();
      if (ourUnits == null || enemyUnits == null) {
        return null;
      }
      if (enemyUnits.size() > 0 && Match.allMatch(ourUnits, seaSub) && Match.allMatch(enemyUnits, planeNotDestroyer)) {
        return possibleTerritories.iterator().next();
      }
      return null;
    } else {
      final MustFightBattle battle = getBattle();
      if (battle == null) {
        return null;
      }
      if (m_retreatAfterRound > -1 && battle.getBattleRound() >= m_retreatAfterRound) {
        return possibleTerritories.iterator().next();
      }
      if (!m_retreatWhenOnlyAirLeft && m_retreatAfterXUnitsLeft <= -1) {
        return null;
      }
      final Collection<Unit> unitsLeft = m_isAttacker ? battle.getAttackingUnits() : battle.getDefendingUnits();
      final Collection<Unit> airLeft = Match.getMatches(unitsLeft, Matches.UnitIsAir);
      if (m_retreatWhenOnlyAirLeft) {
        // lets say we have a bunch of 3 attack air unit, and a 4 attack non-air unit,
        // and we want to retreat when we have all air units left + that 4 attack non-air (cus it gets taken casualty
        // last)
        // then we add the number of air, to the retreat after X left number (which we would set to '1')
        int retreatNum = airLeft.size();
        if (m_retreatAfterXUnitsLeft > 0) {
          retreatNum += m_retreatAfterXUnitsLeft;
        }
        if (retreatNum >= unitsLeft.size()) {
          return possibleTerritories.iterator().next();
        }
      }
      if (m_retreatAfterXUnitsLeft > -1 && m_retreatAfterXUnitsLeft >= unitsLeft.size()) {
        return possibleTerritories.iterator().next();
      }
      return null;
    }
  }

  // Added new collection autoKilled to handle killing units prior to casualty selection
  @Override
  public CasualtyDetails selectCasualties(final Collection<Unit> selectFrom,
      final Map<Unit, Collection<Unit>> dependents, final int count, final String message, final DiceRoll dice,
      final PlayerID hit, final Collection<Unit> friendlyUnits, final PlayerID enemyPlayer,
      final Collection<Unit> enemyUnits, final boolean amphibious, final Collection<Unit> amphibiousLandAttackers,
      final CasualtyList defaultCasualties, final GUID battleID, final Territory battlesite,
      final boolean allowMultipleHitsPerUnit) {
    final List<Unit> rDamaged = new ArrayList<>(defaultCasualties.getDamaged());
    final List<Unit> rKilled = new ArrayList<>(defaultCasualties.getKilled());
    if (m_keepAtLeastOneLand) {
      final List<Unit> notKilled = new ArrayList<>(selectFrom);
      notKilled.removeAll(rKilled);
      // no land units left, but we have a non land unit to kill and land unit was killed
      if (!Match.someMatch(notKilled, Matches.UnitIsLand) && Match.someMatch(notKilled, Matches.UnitIsNotLand)
          && Match.someMatch(rKilled, Matches.UnitIsLand)) {
        final List<Unit> notKilledAndNotLand = Match.getMatches(notKilled, Matches.UnitIsNotLand);
        // sort according to cost
        Collections.sort(notKilledAndNotLand, AIUtils.getCostComparator());
        // remove the last killed unit, this should be the strongest
        rKilled.remove(rKilled.size() - 1);
        // add the cheapest unit
        rKilled.add(notKilledAndNotLand.get(0));
      }
    }
    if (m_orderOfLosses != null && !m_orderOfLosses.isEmpty() && !rKilled.isEmpty()) {
      final List<Unit> orderOfLosses = new ArrayList<>(m_orderOfLosses);
      orderOfLosses.retainAll(selectFrom);
      if (!orderOfLosses.isEmpty()) {
        int killedSize = rKilled.size();
        rKilled.clear();
        while (killedSize > 0 && !orderOfLosses.isEmpty()) {
          rKilled.add(orderOfLosses.get(0));
          orderOfLosses.remove(0);
          killedSize--;
        }
        if (killedSize > 0) {
          final List<Unit> defaultKilled = new ArrayList<>(defaultCasualties.getKilled());
          defaultKilled.removeAll(rKilled);
          while (killedSize > 0) {
            rKilled.add(defaultKilled.get(0));
            defaultKilled.remove(0);
            killedSize--;
          }
        }
      }
    }
    final CasualtyDetails casualtyDetails = new CasualtyDetails(rKilled, rDamaged, false);
    return casualtyDetails;
  }

  @Override
  public Territory selectTerritoryForAirToLand(final Collection<Territory> candidates, final Territory currentTerritory,
      final String unitMessage) {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean shouldBomberBomb(final Territory territory) {
    throw new UnsupportedOperationException();
  }

  @Override
  public Unit whatShouldBomberBomb(final Territory territory, final Collection<Unit> potentialTargets,
      final Collection<Unit> bombers) {
    throw new UnsupportedOperationException();
  }

}
