package games.strategy.triplea.attachments;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableMap;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.NamedAttachable;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.UnitTypeList;
import games.strategy.engine.data.gameparser.GameParseException;
import games.strategy.triplea.Constants;
import games.strategy.triplea.delegate.TechAdvance;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.triplea.java.collections.IntegerMap;

class TechAbilityAttachmentTest {

  private final GameData data = mock(GameData.class);
  private final TechAbilityAttachment attachment =
      spy(new TechAbilityAttachment("", new NamedAttachable("test", data), data));
  private final UnitTypeList list = mock(UnitTypeList.class);
  private final UnitType dummyUnitType = mock(UnitType.class);
  private final String name = "Test Name";
  private final String customToString = "CustomToString";
  private final String testUnitType = "someExistentKey";

  @BeforeEach
  void setUp() {
    when(attachment.toString()).thenReturn(customToString);
    when(data.getUnitTypeList()).thenReturn(list);
    when(list.getUnitType(testUnitType)).thenReturn(dummyUnitType);
    final TechnologyFrontier fron = mock(TechnologyFrontier.class);
    when(data.getTechnologyFrontier()).thenReturn(fron);
    final TechAdvance advance = mock(TechAdvance.class);
    when(fron.getTechs()).thenReturn(List.of(advance, advance, advance, advance));
    when(advance.hasTech(any())).thenReturn(Boolean.TRUE);
    when(advance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME))
        .thenReturn(attachment, null, attachment, attachment);
  }

  @Test
  void splitAndValidateWithEmptyString() {
    final Exception e =
        assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, ""));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  void splitAndValidateWithInvalidLength() {
    final Exception e =
        assertThrows(GameParseException.class, () -> attachment.splitAndValidate(name, "a:b:c"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  void splitAndValidateWithOneValue() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a");
    assertEquals(1, result.length);
    assertEquals("a", result[0]);
  }

  @Test
  void splitAndValidateWithTwoValues() throws Exception {
    final String[] result = attachment.splitAndValidate(name, "a:b");
    assertEquals(2, result.length);
    assertEquals("a", result[0]);
    assertEquals("b", result[1]);
  }

  @Test
  void getIntInRangeWithNoInt() {
    final Exception e1 =
        assertThrows(
            IllegalArgumentException.class, () -> attachment.getIntInRange(name, "NaN", 10, false));
    final Exception e2 =
        assertThrows(
            IllegalArgumentException.class, () -> attachment.getIntInRange(name, "NaN", -10, true));
    assertTrue(e1.getCause() instanceof NumberFormatException);
    assertTrue(e2.getCause() instanceof NumberFormatException);
    assertTrue(e1.getMessage().contains("NaN"));
    assertTrue(e2.getMessage().contains("NaN"));
  }

  @Test
  void getIntInRangeWithTooHigh() {
    final Exception e =
        assertThrows(
            GameParseException.class, () -> attachment.getIntInRange(name, "20", 10, false));
    assertTrue(e.getMessage().contains("20"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  void getIntInRangeWithNegativeNoUndefined() {
    final Exception e =
        assertThrows(
            GameParseException.class, () -> attachment.getIntInRange(name, "-1", 10, false));
    assertTrue(e.getMessage().contains("-1"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  void getIntInRangeWithNegativeUndefined() {
    final Exception e =
        assertThrows(
            GameParseException.class, () -> attachment.getIntInRange(name, "-2", 10, true));
    assertTrue(e.getMessage().contains("-2"));
    assertTrue(e.getMessage().contains("10"));
    assertTrue(e.getMessage().contains(name));
    assertTrue(e.getMessage().contains(customToString));
  }

  @Test
  void getIntInRangeWithValidValues() throws Exception {
    assertEquals(-1, attachment.getIntInRange(name, "-1", 10, true));
    assertEquals(10, attachment.getIntInRange(name, "10", 10, true));
    assertEquals(0, attachment.getIntInRange(name, "0", 10, false));
    assertEquals(10, attachment.getIntInRange(name, "10", 10, false));
  }

  @Test
  void getUnitType() throws Exception {
    assertEquals(dummyUnitType, attachment.getUnitType(testUnitType));
    verify(list).getUnitType(testUnitType);
    verify(list).getUnitType(any());
  }

  @Test
  void getUnitTypeWithNoValue() {
    final String test = "someNonExistentKey";
    final Exception e = assertThrows(GameParseException.class, () -> attachment.getUnitType(test));
    verify(list).getUnitType(test);
    verify(list).getUnitType(any());
    assertTrue(e.getMessage().contains(test));
  }

  @Test
  void applyCheckedValue() throws Exception {
    final Map<UnitType, Integer> map = new HashMap<>();
    attachment.applyCheckedValue(name, "1:" + testUnitType, map::put);
    assertEquals(1, map.size());
    assertEquals(1, (int) map.get(dummyUnitType));
  }

  @Test
  void sumIntegerMap() {
    @SuppressWarnings("unchecked")
    final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper = mock(Function.class);
    doReturn(
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, -1)),
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, 20)),
            new IntegerMap<>(ImmutableMap.of(dummyUnitType, 300)))
        .when(mapper)
        .apply(attachment);
    final int result =
        TechAbilityAttachment.sumIntegerMap(
            mapper, dummyUnitType, mock(GamePlayer.class), data.getTechnologyFrontier());
    assertEquals(319, result);
  }

  @Test
  void sumNumbers() {
    final AtomicInteger counter = new AtomicInteger(1);
    final int result =
        TechAbilityAttachment.sumNumbers(
            a -> {
              assertEquals(attachment, a);
              return counter.getAndUpdate(i -> i * -10);
            },
            "NamedAttachable{name=test}",
            mock(GamePlayer.class),
            data.getTechnologyFrontier());
    assertEquals(101, result);
  }
}
