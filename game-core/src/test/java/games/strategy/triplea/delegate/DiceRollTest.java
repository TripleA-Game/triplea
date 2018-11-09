package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.bomber;
import static games.strategy.triplea.delegate.GameDataTestUtil.british;
import static games.strategy.triplea.delegate.MockDelegateBridge.thenGetRandomShouldHaveBeenCalled;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TerritoryEffect;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.CollectionUtils;

public class DiceRollTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.LHTR.getGameData();
  }

  private IDelegateBridge newDelegateBridge(final PlayerId player) {
    return MockDelegateBridge.newInstance(gameData, player);
  }

  @Test
  public void testSimple() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // infantry defends and hits at 1 (0 based)
        .thenAnswer(withValues(2)) // infantry does not hit at 2 (0 based)
        .thenAnswer(withValues(0)) // infantry attacks and hits at 0 (0 based)
        .thenAnswer(withValues(1)); // infantry attack does not hit at 1 (0 based)
    // infantry defends
    final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll.getHits(), is(1));
    // infantry
    final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll2.getHits(), is(0));
    // infantry attacks
    final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll3.getHits(), is(1));
    // infantry attack
    final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  public void testSimpleLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> infantry = infantryType.create(1, russians);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(westRussia);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // infantry defends and hits at 1 (0 based)
        .thenAnswer(withValues(2)) // infantry does not hit at 2 (0 based)
        .thenAnswer(withValues(0)) // infantry attacks and hits at 0 (0 based)
        .thenAnswer(withValues(1)); // infantry attack does not hit at 1 (0 based)
    // infantry defends
    final DiceRoll roll = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll.getHits(), is(1));
    // infantry
    final DiceRoll roll2 = DiceRoll.rollDice(infantry, true, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll2.getHits(), is(0));
    // infantry attacks
    final DiceRoll roll3 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll3.getHits(), is(1));
    // infantry attack
    final DiceRoll roll4 = DiceRoll.rollDice(infantry, false, russians, bridge, battle, "", territoryEffects, null);
    assertThat(roll4.getHits(), is(0));
  }

  @Test
  public void testArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(1, russians);
    final UnitType artillery = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    units.addAll(artillery.create(1, russians));
    // artillery supported infantry and art attack at 1 (0 based)
    whenGetRandom(bridge).thenAnswer(withValues(1, 1));
    final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(2));
  }

  @Test
  public void testVariableArtillerySupport() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // Add 1 artillery
    final UnitType artillery = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_ARTILLERY);
    final List<Unit> units = artillery.create(1, russians);
    // Set the supported unit count
    for (final Unit unit : units) {
      final UnitAttachment ua = UnitAttachment.get(unit.getType());
      ua.setUnitSupportCount("2");
    }
    // Now add the infantry
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    units.addAll(infantryType.create(2, russians));
    // artillery supported infantry and art attack at 1 (0 based)
    whenGetRandom(bridge).thenAnswer(withValues(1, 1, 1));
    final DiceRoll roll = DiceRoll.rollDice(units, false, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(3));
  }

  @Test
  public void testLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final IBattle battle = mock(IBattle.class);
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final UnitType infantryType = GameDataTestUtil.infantry(gameData);
    final List<Unit> units = infantryType.create(3, russians);
    // 3 infantry on defense should produce exactly one hit, without rolling the dice
    final DiceRoll roll = DiceRoll.rollDice(units, true, russians, bridge, battle, "",
        TerritoryEffectHelper.getEffects(westRussia), null);
    assertThat(roll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  public void testMarineAttackPlus1() throws Exception {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(1, americans);
    final IDelegateBridge bridge = newDelegateBridge(americans);
    whenGetRandom(bridge).thenAnswer(withValues(1));
    final IBattle battle = mock(IBattle.class);
    when(battle.getAmphibiousLandAttackers()).thenReturn(attackers);
    when(battle.isAmphibious()).thenReturn(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(1));
  }

  @Test
  public void testMarineAttackPlus1LowLuck() throws Exception {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(3, americans);
    final IDelegateBridge bridge = newDelegateBridge(americans);
    final IBattle battle = mock(IBattle.class);
    when(battle.getAmphibiousLandAttackers()).thenReturn(attackers);
    when(battle.isAmphibious()).thenReturn(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  public void testMarineAttacNormalIfNotAmphibious() throws Exception {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final Territory algeria = gameData.getMap().getTerritory("Algeria");
    final PlayerId americans = GameDataTestUtil.americans(gameData);
    final UnitType marine = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MARINE);
    final List<Unit> attackers = marine.create(1, americans);
    final IDelegateBridge bridge = newDelegateBridge(americans);
    whenGetRandom(bridge).thenAnswer(withValues(1));
    final IBattle battle = mock(IBattle.class);
    when(battle.getAmphibiousLandAttackers()).thenReturn(Collections.emptyList());
    when(battle.isAmphibious()).thenReturn(true);
    final DiceRoll roll = DiceRoll.rollDice(attackers, false, americans, bridge, battle, "",
        TerritoryEffectHelper.getEffects(algeria), null);
    assertThat(roll.getHits(), is(0));
  }

  @Test
  public void testAa() {
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final PlayerId germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    final List<Unit> bombers = bomber(gameData).create(1, british(gameData));
    whenGetRandom(bridge)
        .thenAnswer(withValues(0)) // aa hits at 0 (0 based)
        .thenAnswer(withValues(1)); // aa misses at 1 (0 based)
    // aa hits
    final DiceRoll hit =
        DiceRoll.rollAa(bomber(gameData).create(1, british(gameData)), aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss = DiceRoll.rollAa(bombers, aaGunList, bridge, westRussia, true);
    assertThat(miss.getHits(), is(0));
  }

  @Test
  public void testAaLowLuck() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final PlayerId germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    whenGetRandom(bridge)
        .thenAnswer(withValues(0)) // aa hits at 0 (0 based)
        .thenAnswer(withValues(1)); // aa misses at 1 (0 based)
    // aa hits
    final DiceRoll hit = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, westRussia, true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 1 should hit, and nothing should be rolled
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, westRussia, true);
    assertThat(hitNoRoll.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
  }

  @Test
  public void testAaLowLuckDifferentMovement() {
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory westRussia = gameData.getMap().getTerritory("West Russia");
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final PlayerId germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(westRussia, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    final List<Unit> fighterList = fighterType.create(6, russians);
    TripleAUnit.get(fighterList.get(0)).setAlreadyMoved(1);
    final IDelegateBridge bridge = newDelegateBridge(russians);
    // aa hits at 0 (0 based)
    final DiceRoll hit = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, westRussia, true);
    assertThat(hit.getHits(), is(1));
    thenGetRandomShouldHaveBeenCalled(bridge, never());
  }

  @Test
  public void testAaLowLuckWithRadar() throws Exception {
    gameData = TestMapGameData.WW2V3_1941.getGameData();
    GameDataTestUtil.makeGameLowLuck(gameData);
    final Territory finnland = gameData.getMap().getTerritory("Finland");
    final PlayerId russians = GameDataTestUtil.russians(gameData);
    final PlayerId germans = GameDataTestUtil.germans(gameData);
    final UnitType aaGunType = GameDataTestUtil.aaGun(gameData);
    final List<Unit> aaGunList = aaGunType.create(1, germans);
    GameDataTestUtil.addTo(finnland, aaGunList);
    final UnitType fighterType = GameDataTestUtil.fighter(gameData);
    List<Unit> fighterList = fighterType.create(1, russians);
    TechAttachment.get(germans).setAaRadar("true");
    final IDelegateBridge bridge = newDelegateBridge(russians);
    whenGetRandom(bridge)
        .thenAnswer(withValues(1)) // aa radar hits at 1 (0 based)
        .thenAnswer(withValues(2)); // aa misses at 2 (0 based)
    // aa radar hits
    final DiceRoll hit = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, finnland, true);
    assertThat(hit.getHits(), is(1));
    // aa misses
    final DiceRoll miss = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, finnland, true);
    assertThat(miss.getHits(), is(0));
    // 6 bombers, 2 should hit, and nothing should be rolled
    fighterList = fighterType.create(6, russians);
    final DiceRoll hitNoRoll = DiceRoll.rollAa(CollectionUtils.getMatches(fighterList,
        Matches.unitIsOfTypes(UnitAttachment.get(aaGunList.iterator().next().getType()).getTargetsAa(gameData))),
        aaGunList, bridge, finnland, true);
    assertThat(hitNoRoll.getHits(), is(2));
    thenGetRandomShouldHaveBeenCalled(bridge, times(2));
  }

  @Test
  public void testHeavyBombers() throws Exception {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final PlayerId british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(2, 3, 2));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testHeavyBombersDefend() throws Exception {
    gameData = TestMapGameData.IRON_BLITZ.getGameData();
    final PlayerId british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(1).size(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testLhtrBomberDefend() {
    final PlayerId british = GameDataTestUtil.british(gameData);
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, true);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);

    assertThat(dice.getRolls(1).size(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
  }

  @Test
  public void testHeavyBombersLhtr() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerId british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(2, 3));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);

    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  public void testHeavyBombersLhtr2() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerId british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(3, 2));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, false, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(4).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(4).get(1).getType(), is(Die.DieType.IGNORED));
    assertThat(dice.getHits(), is(1));
  }

  @Test
  public void testHeavyBombersDefendLhtr() {
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    final PlayerId british = GameDataTestUtil.british(gameData);
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    final List<Unit> bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber());
    whenGetRandom(testDelegateBridge).thenAnswer(withValues(0, 1));
    final Territory germany = gameData.getMap().getTerritory("Germany");
    final DiceRoll dice = DiceRoll.rollDice(bombers, true, british, testDelegateBridge, mock(IBattle.class), "",
        TerritoryEffectHelper.getEffects(germany), null);
    assertThat(dice.getRolls(1).size(), is(2));
    assertThat(dice.getHits(), is(1));
    assertThat(dice.getRolls(1).get(0).getType(), is(Die.DieType.HIT));
    assertThat(dice.getRolls(1).get(1).getType(), is(Die.DieType.IGNORED));
  }

  @Test
  public void testDiceRollCount() {
    final PlayerId british = GameDataTestUtil.british(gameData);
    final Territory location = gameData.getMap().getTerritory("United Kingdom");
    final Unit bombers =
        gameData.getMap().getTerritory("United Kingdom").getUnits().getMatches(Matches.unitIsStrategicBomber()).get(0);
    final Collection<TerritoryEffect> territoryEffects = TerritoryEffectHelper.getEffects(location);
    // default 1 roll
    assertThat(BattleCalculator.getRolls(bombers, british, false, true, territoryEffects), is(1));
    assertThat(BattleCalculator.getRolls(bombers, british, true, true, territoryEffects), is(1));
    // hb, for revised 2 on attack, 1 on defence
    final IDelegateBridge testDelegateBridge = newDelegateBridge(british);
    TechTracker.addAdvance(british, testDelegateBridge,
        TechAdvance.findAdvance(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER, gameData, british));
    // lhtr hb, 2 for both
    gameData.getProperties().set(Constants.LHTR_HEAVY_BOMBERS, Boolean.TRUE);
    assertThat(BattleCalculator.getRolls(bombers, british, false, true, territoryEffects), is(2));
    assertThat(BattleCalculator.getRolls(bombers, british, true, true, territoryEffects), is(2));
  }
}
