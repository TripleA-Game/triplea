package games.strategy.triplea.ai.pro.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.triplea.xml.TestMapGameData;
import org.junit.jupiter.api.Test;

class ProUtilsTest {

  @Test
  void testIsPassiveNeutralPlayer() throws Exception {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final PlayerId russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isPassiveNeutralPlayer(russians));
    final PlayerId neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isPassiveNeutralPlayer(neutralTrue));
    final PlayerId pirates = data.getPlayerList().getPlayerId("Pirates");
    assertFalse(ProUtils.isPassiveNeutralPlayer(pirates));
  }

  @Test
  void testIsNeutralPlayer() throws Exception {
    final GameData data = TestMapGameData.GLOBAL1940.getGameData();
    final PlayerId russians = data.getPlayerList().getPlayerId("Russians");
    assertFalse(ProUtils.isNeutralPlayer(russians));
    final PlayerId neutralTrue = data.getPlayerList().getPlayerId("Neutral_True");
    assertTrue(ProUtils.isNeutralPlayer(neutralTrue));
    final PlayerId pirates = data.getPlayerList().getPlayerId("Pirates");
    assertTrue(ProUtils.isNeutralPlayer(pirates));
  }
}
