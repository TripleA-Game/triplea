package games.strategy.triplea.delegate;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TerritoryEffectHelperTest extends AbstractDelegateTestCase {

  private GameData twwGameData;
  private PlayerId germanPlayer;
  private Territory sicily;

  @BeforeEach
  void setup() throws Exception {
    twwGameData = TestMapGameData.TWW.getGameData();
    germanPlayer = GameDataTestUtil.germany(twwGameData);
    sicily = territory("Sicily", twwGameData);
  }

  @Test
  void testGetMaxMovementCostZero() throws Exception {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germanPlayer));
    assertThat(
        "Expect German infantry to have 0 movement cost for Sicily island territory effect",
        result.compareTo(BigDecimal.ZERO),
        is(0));
  }

  @Test
  void testGetMaxMovementCostDecimal() throws Exception {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germanPlayer));
    assertThat(
        "Expect German alpine to have 0.5 movement cost for Sicily island territory effect",
        result.compareTo(new BigDecimal("0.5")),
        is(0));
  }

  @Test
  void testGetMaxMovementCostTwo() throws Exception {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily,
            GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germanPlayer));
    assertThat(
        "Expect German combat engineer to have 2 movement cost for Sicily island territory effect",
        result.compareTo(new BigDecimal("2")),
        is(0));
  }

  @Test
  void testGetMaxMovementCostNoEffect() throws Exception {
    final BigDecimal result =
        TerritoryEffectHelper.getMaxMovementCost(
            sicily, GameDataTestUtil.unitType("germanMarine", twwGameData).create(1, germanPlayer));
    assertThat(
        "Expect German marine to have 1 movement cost for no territory effects",
        result.compareTo(BigDecimal.ONE),
        is(0));
  }

  @Test
  void testGetMaxMovementCostMultipleUnits() throws Exception {
    final List<Unit> units = new ArrayList<>();
    units.addAll(GameDataTestUtil.unitType("germanInfantry", twwGameData).create(1, germanPlayer));
    units.addAll(
        GameDataTestUtil.unitType("germanAlpineInfantry", twwGameData).create(1, germanPlayer));
    units.addAll(
        GameDataTestUtil.unitType("germanCombatEngineer", twwGameData).create(1, germanPlayer));
    units.addAll(GameDataTestUtil.unitType("germanMarine", twwGameData).create(1, germanPlayer));
    final BigDecimal result = TerritoryEffectHelper.getMaxMovementCost(sicily, units);
    assertThat(
        "Expect German units to have 2 movement cost as that is max across all units",
        result.compareTo(new BigDecimal("2")),
        is(0));
  }

  @Test
  void testGetMaxMovementCostForNoUnits() throws Exception {
    final BigDecimal result = TerritoryEffectHelper.getMaxMovementCost(sicily, Set.of());
    assertThat(
        "Expect 1 movement cost when no units are passed in",
        result.compareTo(BigDecimal.ONE),
        is(0));
  }
}
