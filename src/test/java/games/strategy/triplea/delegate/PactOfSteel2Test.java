package games.strategy.triplea.delegate;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.Before;
import org.junit.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.ITestDelegateBridge;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.attachments.RulesAttachment;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.Match;

public class PactOfSteel2Test {
  private GameData gameData;

  @Before
  public void setUp() throws Exception {
    gameData = TestMapGameData.PACT_OF_STEEL_2.getGameData();
  }

  private ITestDelegateBridge getDelegateBridge(final PlayerID player) {
    return GameDataTestUtil.getDelegateBridge(player, gameData);
  }

  @Test
  public void testDirectOwnershipTerritories() {
    final Territory norway = gameData.getMap().getTerritory("Norway");
    final Territory easternEurope = gameData.getMap().getTerritory("Eastern Europe");
    final Territory eastBalkans = gameData.getMap().getTerritory("East Balkans");
    final Territory ukraineSsr = gameData.getMap().getTerritory("Ukraine S.S.R.");
    final Territory belorussia = gameData.getMap().getTerritory("Belorussia");
    final PlayerID british = GameDataTestUtil.british(gameData);
    final PlayerID germans = GameDataTestUtil.germans(gameData);
    final PlayerID russians = GameDataTestUtil.russians(gameData);
    final ITestDelegateBridge bridge = getDelegateBridge(russians);
    // this National Objective russia has to own at least 3 of the 5 territories by itself
    final RulesAttachment russianEasternEurope =
        RulesAttachment.get(russians, "objectiveAttachmentRussians1_EasternEurope");
    final Collection<Territory> terrs = new ArrayList<>();
    terrs.add(norway);
    terrs.add(easternEurope);
    terrs.add(eastBalkans);
    terrs.add(ukraineSsr);
    terrs.add(belorussia);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 5);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    norway.setOwner(british);
    easternEurope.setOwner(russians);
    eastBalkans.setOwner(russians);
    ukraineSsr.setOwner(germans);
    belorussia.setOwner(germans);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 1);
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    ukraineSsr.setOwner(british);
    belorussia.setOwner(british);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 3);
    assertFalse(russianEasternEurope.isSatisfied(null, bridge));
    norway.setOwner(russians);
    ukraineSsr.setOwner(germans);
    belorussia.setOwner(germans);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 2);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 3);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
    ukraineSsr.setOwner(russians);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 1);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 4);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
    belorussia.setOwner(russians);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(germans)), 0);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(russians)), 5);
    assertEquals(Match.countMatches(terrs, Matches.isTerritoryOwnedBy(british)), 0);
    assertTrue(russianEasternEurope.isSatisfied(null, bridge));
  }

  // TODO: Consider adding the following tests:
  //
  // testSupportAttachments
  //
  // testNationalObjectiveUses
  //
  // testBlockadeAndBlockadeZones
  //
  // testTriggers
  //
  // testConditions
  //
  // testObjectives
  //
  // testTechnologyFrontiers
  // - frontiers, renaming, generic, and new techs and adding of players to frontiers
  //
  // testIsCombatTransport
  //
  // testIsConstruction
  // - isConstruction, constructionType, constructionsPerTerrPerTypePerTurn, maxConstructionsPerTypePerTerr,
  // - "More Constructions with Factory", "More Constructions with Factory", "Unlimited Constructions"
  //
  // testMaxPlacePerTerritory
  //
  // testCapitalCapturePlayerOptions
  // - destroysPUs, retainCapitalNumber, retainCapitalProduceNumber
  //
  // testUnitPlacementRestrictions
  //
  // testRepairsUnits
  // - repairsUnits, "Two HitPoint Units Require Repair Facilities", "Units Repair Hits Start Turn"
  //
  // testProductionPerXTerritories
  //
  // testGiveUnitControl
  // - giveUnitControl, changeUnitOwners, canBeGivenByTerritoryTo, "Give Units By Territory"
  //
  // testDiceSides
  //
  // testMaxBuiltPerPlayer
  //
  // testDestroyedWhenCapturedBy
  // - "Units Can Be Destroyed Instead Of Captured", destroyedWhenCapturedBy
  //
  // testIsInfrastructure
  //
  // testCanBeDamaged
  //
  // testIsSuicide
  // - isSuicide, "Suicide and Munition Casualties Restricted",
  // - "Defending Suicide and Munition Units Do Not Fire"
}
