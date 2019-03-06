package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.Map.Entry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.delegate.IBattle.BattleType;
import games.strategy.triplea.xml.TestMapGameData;

public class AirThatCantLandUtilTest {
  private GameData gameData;
  private PlayerId americansPlayer;
  private UnitType fighterType;

  @BeforeEach
  void setUp() throws Exception {
    gameData = TestMapGameData.REVISED.getGameData();
    americansPlayer = GameDataTestUtil.americans(gameData);
    fighterType = GameDataTestUtil.fighter(gameData);
  }

  private IDelegateBridge newDelegateBridge(final PlayerId player) {
    return MockDelegateBridge.newInstance(gameData, player);
  }

  private static String fight(final BattleDelegate battle, final Territory territory, final boolean bombing) {
    for (final Entry<BattleType, Collection<Territory>> entry : battle.getBattles().getBattles().entrySet()) {
      if (entry.getKey().isBombingRun() == bombing) {
        if (entry.getValue().contains(territory)) {
          return battle.fightBattle(territory, bombing, entry.getKey());
        }
      }
    }
    throw new IllegalStateException(
        "Could not find " + (bombing ? "bombing" : "normal") + " battle in: " + territory.getName());
  }

  @Test
  void testSimple() {
    final PlayerId player = americansPlayer;
    // everything can land
    final IDelegateBridge bridge = newDelegateBridge(player);
    final AirThatCantLandUtil util = new AirThatCantLandUtil(bridge);
    assertTrue(util.getTerritoriesWhereAirCantLand(player).isEmpty());
  }

  @Test
  void testCantLandEnemyTerritory() {
    final PlayerId player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory balkans = gameData.getMap().getTerritory("Balkans");
    final Change addAir = ChangeFactory.addUnits(balkans, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(balkans, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // jsut the original german fighter
    assertEquals(1, balkans.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCantLandWater() {
    final PlayerId player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz55, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    assertEquals(0, sz55.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testSpareNextToFactory() {
    final PlayerId player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz55 = gameData.getMap().getTerritory("55 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz55, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    airThatCantLandUtil.removeAirThatCantLand(player, true);
    assertEquals(2, sz55.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCantLandCarrier() {
    // 1 carrier in the region, but three fighters, make sure we cant land
    final PlayerId player = americansPlayer;
    final IDelegateBridge bridge = newDelegateBridge(player);
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final Change addAir = ChangeFactory.addUnits(sz52, fighterType.create(2, player));
    gameData.performChange(addAir);
    final AirThatCantLandUtil airThatCantLandUtil = new AirThatCantLandUtil(bridge);
    final Collection<Territory> cantLand = airThatCantLandUtil.getTerritoriesWhereAirCantLand(player);
    assertEquals(1, cantLand.size());
    assertEquals(sz52, cantLand.iterator().next());
    airThatCantLandUtil.removeAirThatCantLand(player, false);
    // just the original american fighter, plus one that can land on the carrier
    assertEquals(2, sz52.getUnitCollection().getMatches(Matches.unitIsAir()).size());
  }

  @Test
  void testCanLandNeighborCarrier() {
    final PlayerId japanese = GameDataTestUtil.japanese(gameData);
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz44 = gameData.getMap().getTerritory("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritory("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnitCollection().size();
    final int preCountAirSz44 = sz44.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits(), gameData.getMap().getRoute(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge)
        .thenAnswer(withValues(0, 0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnitCollection().size();
    final int postCountInt = preCountSz52 + preCountAirSz44;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountInt);
  }

  @Test
  void testCanLandMultiNeighborCarriers() {
    final PlayerId japanese = GameDataTestUtil.japanese(gameData);
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz43 = gameData.getMap().getTerritory("43 Sea Zone");
    final Territory sz44 = gameData.getMap().getTerritory("44 Sea Zone");
    final Territory sz45 = gameData.getMap().getTerritory("45 Sea Zone");
    final Territory sz52 = gameData.getMap().getTerritory("52 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz45, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz44, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz44, fighterType.create(3, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz43, carrierType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountSz52 = sz52.getUnitCollection().size();
    final int preCountSz43 = sz43.getUnitCollection().size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz45.getUnits(), gameData.getMap().getRoute(sz45, sz44));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge)
        .thenAnswer(withValues(0, 0))
        .thenAnswer(withValues(0, 0, 0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountSz52 = sz52.getUnitCollection().size();
    final int expectedCountSz43 = sz43.getUnitCollection().size();
    final int postCountSz52 = preCountSz52 + 1;
    final int postCountSz43 = preCountSz43 + 2;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountSz52, postCountSz52);
    assertEquals(expectedCountSz43, postCountSz43);
  }

  @Test
  void testCanLandNeighborLandV2() {
    final PlayerId japanese = GameDataTestUtil.japanese(gameData);
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // we need to initialize the original owner
    final InitializationDelegate initDel = (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritory("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz11, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz9, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz9, fighterType.create(1, americans)));
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnitCollection().size();
    final int preCountAirSz9 = sz9.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz11.getUnits(), gameData.getMap().getRoute(sz11, sz9));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    whenGetRandom(bridge).thenAnswer(withValues(0));
    battle.start();
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnitCollection().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }

  @Test
  void testCanLandNeighborLandWithRetreatedBattleV2() {
    final PlayerId japanese = GameDataTestUtil.japanese(gameData);
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final IDelegateBridge bridge = newDelegateBridge(japanese);
    // Get necessary sea zones and unit types for this test
    final Territory sz9 = gameData.getMap().getTerritory("9 Sea Zone");
    final Territory eastCanada = gameData.getMap().getTerritory("Eastern Canada");
    final Territory sz11 = gameData.getMap().getTerritory("11 Sea Zone");
    final UnitType subType = GameDataTestUtil.submarine(gameData);
    final UnitType carrierType = GameDataTestUtil.carrier(gameData);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    final UnitType transportType = GameDataTestUtil.transport(gameData);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    // Add units for the test
    gameData.performChange(ChangeFactory.addUnits(sz11, subType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz11, transportType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz11, infantryType.create(1, japanese)));
    gameData.performChange(ChangeFactory.addUnits(sz9, carrierType.create(1, americans)));
    gameData.performChange(ChangeFactory.addUnits(sz9, fighterType.create(2, americans)));
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(bridge);
    initDel.start();
    initDel.end();
    // Get total number of defending units before the battle
    final int preCountCanada = eastCanada.getUnitCollection().size();
    final int preCountAirSz9 = sz9.getUnitCollection().getMatches(Matches.unitIsAir()).size();
    // now move to attack
    final MoveDelegate moveDelegate = (MoveDelegate) gameData.getDelegate("move");
    advanceToStep(bridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(bridge);
    moveDelegate.start();
    moveDelegate.move(sz11.getUnits(), gameData.getMap().getRoute(sz11, sz9));
    moveDelegate.move(sz9.getUnitCollection().getUnits(infantryType, 1), gameData.getMap().getRoute(sz9, eastCanada));
    moveDelegate.end();
    // fight the battle
    final BattleDelegate battle = (BattleDelegate) gameData.getDelegate("battle");
    battle.setDelegateBridgeAndPlayer(bridge);
    battle.start();
    whenGetRandom(bridge)
        .thenAnswer(withValues(0))
        .thenAnswer(withValues(0, 0));
    fight(battle, sz9, false);
    battle.end();
    // Get the total number of units that should be left after the planes retreat
    final int expectedCountCanada = eastCanada.getUnitCollection().size();
    final int postCountInt = preCountCanada + preCountAirSz9;
    // Compare the expected count with the actual number of units in landing zone
    assertEquals(expectedCountCanada, postCountInt);
  }
}
