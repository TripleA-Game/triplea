package games.strategy.engine.lobby.server.db;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import games.strategy.util.Util;

public class BadWordControllerTest {

  @Test
  public void testCrud() {
    final BadWordController controller = new BadWordController();
    final String word = Util.createUniqueTimeStamp();
    controller.addBadWord(word);
    assertTrue(controller.list().contains(word));
    controller.removeBannedWord(word);
    assertFalse(controller.list().contains(word));
  }
}
