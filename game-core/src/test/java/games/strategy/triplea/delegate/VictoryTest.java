package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.MockDelegateBridge.advanceToStep;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

/**
 * "Victory" map is just a branch/mod of Pact of Steel 2. POS2 is an actual game with good gameplay
 * that we don't want to mess with, so "Victory" is more of an xml purely for testing purposes, and
 * probably should never be played.
 */
class VictoryTest {
  private GameData gameData;
  private PlayerId italians;
  private PlayerId germans;
  private IDelegateBridge testBridge;

  private IntegerMap<Resource> italianResources;
  private PurchaseDelegate purchaseDelegate;
  private Territory britishCongo;
  private Territory kenya;
  private UnitType motorized;
  private UnitType armour;
  private UnitType fighter;
  private UnitType carrier;
  private Territory frenchEastAfrica;
  private Territory frenchWestAfrica;
  private Territory angloEgypt;
  private Territory libya;
  private Territory sz29;
  private Territory sz30;
  private MoveDelegate moveDelegate;

  @BeforeEach
  void setUp() throws Exception {
    gameData = TestMapGameData.VICTORY_TEST.getGameData();
    italians = GameDataTestUtil.italians(gameData);
    germans = GameDataTestUtil.germans(gameData);
    testBridge = MockDelegateBridge.newInstance(gameData, italians);
    // we need to initialize the original owner
    final InitializationDelegate initDel =
        (InitializationDelegate) gameData.getDelegate("initDelegate");
    initDel.setDelegateBridgeAndPlayer(testBridge);
    initDel.start();
    initDel.end();

    italianResources = italians.getResources().getResourcesCopy();
    purchaseDelegate = (PurchaseDelegate) gameData.getDelegate("purchase");
    moveDelegate = (MoveDelegate) gameData.getDelegate("move");

    britishCongo = gameData.getMap().getTerritory("Belgian Congo");
    kenya = gameData.getMap().getTerritory("Kenya");
    motorized = gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_MOTORIZED);
    armour = GameDataTestUtil.armour(gameData);
    fighter = GameDataTestUtil.fighter(gameData);
    carrier = GameDataTestUtil.carrier(gameData);
    frenchEastAfrica = gameData.getMap().getTerritory("French Equatorial Africa");
    frenchWestAfrica = gameData.getMap().getTerritory("French West Africa");
    angloEgypt = gameData.getMap().getTerritory("Anglo Egypt");
    libya = gameData.getMap().getTerritory("Libya");
    sz29 = gameData.getMap().getTerritory("29 Sea Zone");
    sz30 = gameData.getMap().getTerritory("30 Sea Zone");
  }

  @Test
  void testNoBlitzThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(libya.getUnits(), gameData.getMap().getRoute(libya, britishCongo));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testBlitzNormal() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(
            frenchWestAfrica.getUnits(),
            gameData.getMap().getRoute(frenchWestAfrica, britishCongo));
    moveDelegate.end();
    assertNull(error);
  }

  @Test
  void testNoBlitzWithStopThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error =
        moveDelegate.move(libya.getUnits(), gameData.getMap().getRoute(libya, angloEgypt));
    // first step is legal
    assertNull(error);
    // second step isn't legal because we lost blitz even though we took the mountain
    error =
        moveDelegate.move(
            angloEgypt.getUnits(), gameData.getMap().getRoute(angloEgypt, britishCongo));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testBlitzWithStop() {
    gameData.performChange(ChangeFactory.addUnits(frenchWestAfrica, armour.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error =
        moveDelegate.move(
            frenchWestAfrica.getUnits(),
            gameData.getMap().getRoute(frenchWestAfrica, frenchEastAfrica));
    assertNull(error);
    error =
        moveDelegate.move(
            frenchEastAfrica.getUnits(),
            gameData.getMap().getRoute(frenchEastAfrica, britishCongo));
    moveDelegate.end();
    assertNull(error);
  }

  @Test
  void testMotorizedThroughMountain() {
    gameData.performChange(ChangeFactory.addUnits(libya, motorized.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final String error =
        moveDelegate.move(libya.getUnits(), gameData.getMap().getRoute(libya, britishCongo));
    moveDelegate.end();
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
  }

  @Test
  void testMotorizedNoBlitzBlitzedTerritory() {
    gameData.performChange(ChangeFactory.changeOwner(frenchEastAfrica, italians));
    gameData.performChange(ChangeFactory.addUnits(frenchEastAfrica, armour.create(1, italians)));
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    String error =
        moveDelegate.move(
            frenchEastAfrica.getUnits(),
            gameData.getMap().getRoute(frenchEastAfrica, britishCongo));
    assertNull(error);
    error = moveDelegate.move(kenya.getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertNull(error);
    error =
        moveDelegate.move(
            britishCongo.getUnits(), gameData.getMap().getRoute(britishCongo, frenchEastAfrica));
    assertEquals(MoveValidator.NOT_ALL_UNITS_CAN_BLITZ, error);
    moveDelegate.end();
  }

  @Test
  void testFuelCostAndFuelFlatCost() {
    gameData.performChange(ChangeFactory.changeOwner(kenya, italians));
    gameData.performChange(ChangeFactory.changeOwner(britishCongo, italians));
    gameData.performChange(ChangeFactory.changeOwner(frenchEastAfrica, italians));
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final int fuelAmount = italians.getResources().getQuantity("Fuel");
    final int puAmount = italians.getResources().getQuantity("PUs");
    final int oreAmount = italians.getResources().getQuantity("Ore");

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(1, italians)));
    moveDelegate.move(kenya.getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 2, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, armour.create(1, italians)));
    moveDelegate.move(kenya.getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 2, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 1, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    moveDelegate.move(
        britishCongo.getUnits(), gameData.getMap().getRoute(britishCongo, frenchEastAfrica));
    assertEquals(fuelAmount - 3, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 2, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 2, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(5, italians)));
    moveDelegate.move(kenya.getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertEquals(fuelAmount - 13, italians.getResources().getQuantity("Fuel"));
    assertEquals(puAmount - 7, italians.getResources().getQuantity("PUs"));
    assertEquals(oreAmount - 12, italians.getResources().getQuantity("Ore"));

    gameData.performChange(ChangeFactory.addUnits(kenya, motorized.create(50, italians)));
    final String error =
        moveDelegate.move(kenya.getUnits(), gameData.getMap().getRoute(kenya, britishCongo));
    assertTrue(error.startsWith("Not enough resources to perform this move"));
    moveDelegate.end();
  }

  @Test
  void testFuelForCarriers() {
    advanceToStep(testBridge, "CombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();
    final int fuelAmount = italians.getResources().getQuantity("Fuel");

    // Combat move where air is always charged fuel
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    moveDelegate.move(sz29.getUnits(), gameData.getMap().getRoute(sz29, sz30));
    assertEquals(fuelAmount - 7, italians.getResources().getQuantity("Fuel"));

    // Rest of the cases use non-combat move
    moveDelegate.end();
    advanceToStep(testBridge, "NonCombatMove");
    moveDelegate.setDelegateBridgeAndPlayer(testBridge);
    moveDelegate.start();

    // Non-combat move where air isn't charged fuel
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    moveDelegate.move(sz29.getUnits(), gameData.getMap().getRoute(sz29, sz30));
    assertEquals(fuelAmount - 8, italians.getResources().getQuantity("Fuel"));
    gameData.performChange(ChangeFactory.removeUnits(sz30, sz30.getUnitCollection()));

    // Move onto carrier, move with carrier, move off carrier
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz30, fighter.create(1, italians)));
    moveDelegate.move(sz30.getUnits(), gameData.getMap().getRoute(sz30, sz29));
    assertEquals(fuelAmount - 11, italians.getResources().getQuantity("Fuel"));
    moveDelegate.move(sz29.getUnits(), gameData.getMap().getRoute(sz29, sz30));
    assertEquals(fuelAmount - 12, italians.getResources().getQuantity("Fuel"));
    moveDelegate.move(
        sz30.getUnitCollection().getMatches(Matches.unitIsAir()),
        gameData.getMap().getRoute(sz30, sz29));
    assertEquals(fuelAmount - 16, italians.getResources().getQuantity("Fuel"));
    gameData.performChange(ChangeFactory.removeUnits(sz29, sz29.getUnitCollection()));
    gameData.performChange(ChangeFactory.removeUnits(sz30, sz30.getUnitCollection()));

    // Too many fighters for carrier
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(1, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(3, italians)));
    moveDelegate.move(sz29.getUnits(), gameData.getMap().getRoute(sz29, sz30));
    assertEquals(fuelAmount - 20, italians.getResources().getQuantity("Fuel"));

    // Allied and owned fighters
    gameData.performChange(ChangeFactory.addUnits(sz29, carrier.create(2, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(2, italians)));
    gameData.performChange(ChangeFactory.addUnits(sz29, fighter.create(3, germans)));
    moveDelegate.move(sz29.getUnits(), gameData.getMap().getRoute(sz29, sz30));
    assertEquals(fuelAmount - 25, italians.getResources().getQuantity("Fuel"));
  }

  @Test
  void testMultipleResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  void testNotEnoughMultipleResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule armourtest =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest2");
    assertNotNull(armourtest);
    italianResources.subtract(armourtest.getCosts());
    purchaseList.add(armourtest, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertEquals(PurchaseDelegate.NOT_ENOUGH_RESOURCES, error);
  }

  @Test
  void testPuOnlyResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour =
        gameData.getProductionRuleList().getProductionRule("buyArmour");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }

  @Test
  void testNoPuResourcesToPurchase() {
    advanceToStep(testBridge, "italianPurchase");
    purchaseDelegate.setDelegateBridgeAndPlayer(testBridge);
    purchaseDelegate.start();
    final IntegerMap<ProductionRule> purchaseList = new IntegerMap<>();
    final ProductionRule buyArmour =
        gameData.getProductionRuleList().getProductionRule("buyArmourtest3");
    assertNotNull(buyArmour);
    italianResources.subtract(buyArmour.getCosts());
    purchaseList.add(buyArmour, 1);
    final String error = purchaseDelegate.purchase(purchaseList);
    assertNull(error);
    assertEquals(italianResources, italians.getResources().getResourcesCopy());
  }
}
