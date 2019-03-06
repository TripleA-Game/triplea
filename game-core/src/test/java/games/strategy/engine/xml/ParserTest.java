package games.strategy.engine.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.ProductionFrontier;
import games.strategy.engine.data.RelationshipTracker;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.ResourceList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.TestAttachment;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.delegate.IDelegate;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.xml.TestMapGameData;

public class ParserTest {
  private GameData gameData;

  @BeforeEach
  void setUp() throws Exception {
    gameData = TestMapGameData.GAME_EXAMPLE.getGameData();
  }

  @Test
  void testCanCreateData() {
    assertNotNull(gameData);
  }

  @Test
  void testTerritoriesCreated() {
    final GameMap map = gameData.getMap();
    final Collection<Territory> territories = map.getTerritories();
    assertEquals(3, territories.size());
  }

  @Test
  void testWater() {
    final Territory atl = gameData.getMap().getTerritory("atlantic");
    assertTrue(atl.isWater());
    final Territory can = gameData.getMap().getTerritory("canada");
    assertFalse(can.isWater());
  }

  @Test
  void testTerritoriesConnected() {
    final GameMap map = gameData.getMap();
    assertEquals(1, map.getDistance(map.getTerritory("canada"), map.getTerritory("us")));
  }

  @Test
  void testResourcesAdded() {
    final ResourceList resources = gameData.getResourceList();
    assertEquals(2, resources.size());
  }

  @Test
  void testUnitTypesAdded() {
    final UnitTypeList units = gameData.getUnitTypeList();
    assertEquals(1, units.size());
  }

  @Test
  void testPlayersAdded() {
    final PlayerList players = gameData.getPlayerList();
    assertEquals(3, players.size());
  }

  @Test
  void testAllianceMade() {
    final PlayerList players = gameData.getPlayerList();
    final PlayerId castro = players.getPlayerId("castro");
    final PlayerId chretian = players.getPlayerId("chretian");
    final RelationshipTracker alliances = gameData.getRelationshipTracker();
    assertTrue(alliances.isAllied(castro, chretian));
  }

  @Test
  void testDelegatesCreated() {
    final Collection<IDelegate> delegates = gameData.getDelegates();
    assertEquals(2, delegates.size());
  }

  @Test
  void testStepsCreated() {
    gameData.getSequence();
  }

  @Test
  void testProductionFrontiersCreated() {
    assertEquals(2, gameData.getProductionFrontierList().size());
  }

  @Test
  void testProductionRulesCreated() {
    assertEquals(3, gameData.getProductionRuleList().size());
  }

  @Test
  void testPlayerProduction() {
    final ProductionFrontier cf = gameData.getProductionFrontierList().getProductionFrontier("canProd");
    final PlayerId can = gameData.getPlayerList().getPlayerId("chretian");
    assertEquals(cf, can.getProductionFrontier());
  }

  @Test
  void testAttachments() {
    TestAttachment att = (TestAttachment) gameData.getResourceList().getResource("gold")
        .getAttachment(Constants.RESOURCE_ATTACHMENT_NAME);
    assertEquals("gold", att.getValue());
    final UnitAttachment ua = (UnitAttachment) gameData.getUnitTypeList().getUnitType(Constants.UNIT_TYPE_INF)
        .getAttachment(Constants.UNIT_ATTACHMENT_NAME);
    assertEquals(1, ua.getTransportCost());
    att = (TestAttachment) gameData.getMap().getTerritory("us").getAttachment(Constants.TERRITORY_ATTACHMENT_NAME);
    assertEquals("us of a", att.getValue());
    att = (TestAttachment) gameData.getPlayerList().getPlayerId("chretian")
        .getAttachment(Constants.PLAYER_ATTACHMENT_NAME);
    assertEquals("liberal", att.getValue());
  }

  @Test
  void testOwnerInitialze() {
    final Territory can = gameData.getMap().getTerritory("canada");
    assertNotNull(can, "couldnt find country");
    assertNotNull(can.getOwner(), "owner null");
    assertEquals("chretian", can.getOwner().getName());
    final Territory us = gameData.getMap().getTerritory("us");
    assertEquals("bush", us.getOwner().getName());
  }

  @Test
  void testUnitsHeldInitialized() {
    final PlayerId bush = gameData.getPlayerList().getPlayerId("bush");
    assertEquals(20, bush.getUnitCollection().getUnitCount());
  }

  @Test
  void testUnitsPlacedInitialized() {
    final Territory terr = gameData.getMap().getTerritory("canada");
    assertEquals(5, terr.getUnitCollection().getUnitCount());
  }

  @Test
  void testResourcesGiven() {
    final PlayerId chretian = gameData.getPlayerList().getPlayerId("chretian");
    final Resource resource = gameData.getResourceList().getResource("silver");
    assertEquals(200, chretian.getResources().getQuantity(resource));
  }
}
