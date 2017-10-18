package games.strategy.engine.message;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collection;
import java.util.Comparator;

import org.junit.jupiter.api.Test;

import games.strategy.test.TestUtil;

public class RemoteInterfaceHelperTest {

  @Test
  public void testSimple() {
    assertEquals("compare", RemoteInterfaceHelper.getMethodInfo(0, Comparator.class).getFirst());
    assertEquals("add", RemoteInterfaceHelper.getMethodInfo(0, Collection.class).getFirst());
    assertEquals(0, RemoteInterfaceHelper.getNumber("add", TestUtil.getClassArrayFrom(Object.class), Collection.class));
    assertEquals(2, RemoteInterfaceHelper.getNumber("clear", TestUtil.getClassArrayFrom(), Collection.class));
  }
}
