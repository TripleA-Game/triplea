package games.strategy.engine.data;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.ImmutableMultimap;
import com.google.common.collect.ImmutableSet;

import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;

public class AllianceTrackerTest {
  private GameData gameData;

  @BeforeEach
  public void setUp() throws Exception {
    gameData = TestMapGameData.TEST.getGameData();
  }

  @Test
  public void testAddAlliance() {
    final PlayerID bush = gameData.getPlayerList().getPlayerId("bush");
    final PlayerID castro = gameData.getPlayerList().getPlayerId("castro");
    final AllianceTracker allianceTracker = gameData.getAllianceTracker();
    final RelationshipTracker relationshipTracker = gameData.getRelationshipTracker();
    assertFalse(relationshipTracker.isAllied(bush, castro));
    // the alliance tracker now only keeps track of GUI elements like the stats panel alliance TUV totals, and does not
    // affect gameplay
    allianceTracker.addToAlliance(bush, "natp");
    // the relationship tracker is the one that keeps track of actual relationships between players, affecting gameplay.
    // Note that changing
    // the relationship between bush and castro, does not change the relationship between bush and chretian
    relationshipTracker.setRelationship(bush, castro,
        gameData.getRelationshipTypeList().getRelationshipType(Constants.RELATIONSHIP_TYPE_DEFAULT_ALLIED));
    assertTrue(relationshipTracker.isAllied(bush, castro));
  }

  @Test
  public void getPlayersInAlliance_ShouldDifferentiateAllianceNamesThatAreSubstringsOfOtherAllianceNames() {
    final PlayerID player1 = new PlayerID("Player1", gameData);
    final PlayerID player2 = new PlayerID("Player2", gameData);
    final PlayerID player3 = new PlayerID("Player3", gameData);
    final PlayerID player4 = new PlayerID("Player4", gameData);
    final String alliance1Name = "Alliance";
    final String alliance2Name = "Anti" + alliance1Name;
    final AllianceTracker allianceTracker = new AllianceTracker(ImmutableMultimap.<PlayerID, String>builder()
        .put(player1, alliance1Name)
        .put(player2, alliance1Name)
        .put(player3, alliance2Name)
        .put(player4, alliance2Name)
        .build());

    assertThat(allianceTracker.getPlayersInAlliance(alliance1Name), is(ImmutableSet.of(player1, player2)));
    assertThat(allianceTracker.getPlayersInAlliance(alliance2Name), is(ImmutableSet.of(player3, player4)));
  }
}
