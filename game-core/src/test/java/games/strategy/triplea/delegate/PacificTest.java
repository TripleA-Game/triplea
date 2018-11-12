package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

public class PacificTest extends AbstractDelegateTestCase {
  UnitType armor;
  UnitType artillery;
  UnitType marine;
  UnitType sub;
  UnitType destroyer;
  UnitType battleship;
  // Define players
  PlayerId americans;
  PlayerId chinese;
  // Define territories
  Territory queensland;
  Territory unitedStates;
  Territory newBritain;
  Territory midway;
  Territory mariana;
  Territory bonin;
  // Define Sea Zones
  Territory sz4;
  Territory sz5;
  Territory sz7;
  Territory sz8;
  Territory sz10;
  Territory sz16;
  Territory sz20;
  Territory sz24;
  Territory sz25;
  Territory sz27;
  IDelegateBridge bridge;
  MoveDelegate delegate;

  @BeforeEach
  public void setupPacificTest() throws Exception {
    gameData = TestMapGameData.PACIFIC_INCOMPLETE.getGameData();
    // Define units
    infantry = GameDataTestUtil.infantry(gameData);
    armor = GameDataTestUtil.armour(gameData);
    artillery = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    fighter = GameDataTestUtil.fighter(gameData);
    bomber = GameDataTestUtil.bomber(gameData);
    sub = GameDataTestUtil.submarine(gameData);
    destroyer = GameDataTestUtil.destroyer(gameData);
    carrier = GameDataTestUtil.carrier(gameData);
    battleship = GameDataTestUtil.battleship(gameData);
    transport = GameDataTestUtil.transport(gameData);
    // Define players
    americans = GameDataTestUtil.americans(gameData);
    chinese = GameDataTestUtil.chinese(gameData);
    british = GameDataTestUtil.british(gameData);
    japanese = GameDataTestUtil.japanese(gameData);
    // Define territories
    queensland = gameData.getMap().getTerritory("Queensland");
    japan = gameData.getMap().getTerritory("Japan");
    unitedStates = gameData.getMap().getTerritory("United States");
    newBritain = gameData.getMap().getTerritory("New Britain");
    midway = gameData.getMap().getTerritory("Midway");
    mariana = gameData.getMap().getTerritory("Mariana");
    bonin = gameData.getMap().getTerritory("Bonin");
    // Define Sea Zones
    sz4 = gameData.getMap().getTerritory("4 Sea Zone");
    sz5 = gameData.getMap().getTerritory("5 Sea Zone");
    sz7 = gameData.getMap().getTerritory("7 Sea Zone");
    sz8 = gameData.getMap().getTerritory("8 Sea Zone");
    sz10 = gameData.getMap().getTerritory("10 Sea Zone");
    sz16 = gameData.getMap().getTerritory("16 Sea Zone");
    sz20 = gameData.getMap().getTerritory("20 Sea Zone");
    sz24 = gameData.getMap().getTerritory("24 Sea Zone");
    sz25 = gameData.getMap().getTerritory("25 Sea Zone");
    sz27 = gameData.getMap().getTerritory("27 Sea Zone");
    bridge = newDelegateBridge(americans);
    advanceToStep(bridge, "japaneseCombatMove");
    delegate = new MoveDelegate();
    delegate.initialize("MoveDelegate", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    delegate.start();
  }

  @Test
  public void testNonJapanAttack() {
    // this will get us to round 2
    advanceToStep(bridge, "japaneseEndTurn");
    advanceToStep(bridge, "japaneseBattle");
    final List<Unit> infantryUs = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(queensland);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // Defending US infantry hit on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending US marines hit on a 2 (0 base)
        .thenAnswer(withValues(1)); // Defending Chinese infantry hit on a 2 (0 base)
    // Defending US infantry
    DiceRoll roll =
        DiceRoll.rollDice(infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll = DiceRoll.rollDice(marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  public void testJapanAttackFirstRound() {
    advanceToStep(bridge, "japaneseBattle");
    while (!gameData.getSequence().getStep().getName().equals("japaneseBattle")) {
      gameData.getSequence().next();
    }
    // >>> After patch normal to-hits will miss <<<
    final List<Unit> infantryUs = infantry.create(1, americans);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(queensland);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // Defending US infantry miss on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending US marines miss on a 2 (0 base)
        .thenAnswer(withValues(1)) // Defending Chinese infantry still hit on a 2 (0 base)
        .thenAnswer(withValues(0)) // Defending US infantry hit on a 1 (0 base)
        .thenAnswer(withValues(0)) // Defending US marines hit on a 1 (0 base)
        .thenAnswer(withValues(1)); // Defending Chinese infantry still hit on a 2 (0 base)
    // Defending US infantry
    DiceRoll roll =
        DiceRoll.rollDice(infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Defending US marines
    final List<Unit> marineUs = marine.create(1, americans);
    roll = DiceRoll.rollDice(marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(0, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    final List<Unit> infantryChina = infantry.create(1, chinese);
    roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US infantry
    roll = DiceRoll.rollDice(infantryUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Defending US marines
    roll = DiceRoll.rollDice(marineUs, true, americans, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
    // Chinese units
    // Defending Chinese infantry
    roll = DiceRoll.rollDice(infantryChina, true, chinese, bridge, mock(IBattle.class), "", territoryEffects, null);
    assertEquals(1, roll.getHits());
  }

  @Test
  public void testCanLand2Airfields() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz4);
    route.add(sz10);
    route.add(sz16);
    route.add(sz27);
    route.add(newBritain);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(GameDataTestUtil.getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanLand1AirfieldStart() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    route.add(midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(GameDataTestUtil.getUnits(map, route.getStart()), route);
    assertValid(results);
    // assertError( results);
  }

  @Test
  public void testCanLand1AirfieldEnd() {
    advanceToStep(bridge, "americanCombatMove");
    final Route route = new Route();
    route.setStart(unitedStates);
    route.add(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    route.add(midway);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(GameDataTestUtil.getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testCanMoveNavalBase() {
    advanceToStep(bridge, "americanNonCombatMove");
    final Route route = new Route();
    route.setStart(sz5);
    route.add(sz7);
    route.add(sz8);
    route.add(sz20);
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(fighter, 1);
    final String results = delegate.move(GameDataTestUtil.getUnits(map, route.getStart()), route);
    assertValid(results);
  }

  @Test
  public void testJapaneseDestroyerTransport() {
    bridge = newDelegateBridge(japanese);
    delegate = new MoveDelegate();
    delegate.initialize("MoveDelegate", "MoveDelegate");
    delegate.setDelegateBridgeAndPlayer(bridge);
    advanceToStep(bridge, "japaneseNonCombatMove");
    delegate.start();
    final IntegerMap<UnitType> map = new IntegerMap<>();
    map.put(infantry, 1);
    final Route route = new Route();
    route.setStart(bonin);
    // movement to force boarding
    route.add(sz24);
    // verify unit counts before move
    assertEquals(2, bonin.getUnits().size());
    assertEquals(1, sz24.getUnits().size());
    // validate movement
    final String results =
        delegate.move(GameDataTestUtil.getUnits(map, route.getStart()), route, route.getEnd().getUnits().getUnits());
    assertValid(results);
    // verify unit counts after move
    assertEquals(1, bonin.getUnits().size());
    assertEquals(2, sz24.getUnits().size());
  }
}
