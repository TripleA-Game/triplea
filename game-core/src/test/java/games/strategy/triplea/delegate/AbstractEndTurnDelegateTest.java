package games.strategy.triplea.delegate;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.Comparator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.xml.TestMapGameData;
import games.strategy.util.IntegerMap;

final class AbstractEndTurnDelegateTest {
  @Nested
  final class FindEstimatedIncomeTest extends AbstractDelegateTestCase {
    @Test
    void testFindEstimatedIncome() throws Exception {
      final GameData global40Data = TestMapGameData.GLOBAL1940.getGameData();
      final PlayerID germans = GameDataTestUtil.germans(global40Data);
      final IntegerMap<Resource> results = AbstractEndTurnDelegate.findEstimatedIncome(germans, global40Data);
      final int pus = results.getInt(new Resource(Constants.PUS, global40Data));
      assertEquals(40, pus);
    }
  }

  @Nested
  final class GetSingleNeighborBlockadesThenHighestToLowestProductionTest {
    private final GameData gameData = new GameData();
    private final Comparator<Territory> comparator = AbstractEndTurnDelegate
        .getSingleNeighborBlockadesThenHighestToLowestProduction(Collections.emptyList(), gameData.getMap());
    private final Territory territory = new Territory("territoryName", gameData);

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreNull() {
      assertThat(comparator.compare(null, null), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreSame() {
      assertThat(comparator.compare(territory, territory), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreEqual() {
      assertThat(comparator.compare(territory, new Territory(territory.getName(), gameData)), is(0));
    }

    @Test
    void shouldReturnLessThanZeroWhenFirstTerritoryIsNonNullAndSecondTerritoryIsNull() {
      assertThat(comparator.compare(territory, null), is(lessThan(0)));
    }

    @Test
    void shouldReturnGreaterThanZeroWhenFirstTerritoryIsNullAndSecondTerritoryIsNonNull() {
      assertThat(comparator.compare(null, territory), is(greaterThan(0)));
    }
  }

  @Nested
  final class GetSingleBlockadeThenHighestToLowestBlockadeDamageTest {
    private final GameData gameData = new GameData();
    private final Comparator<Territory> comparator = AbstractEndTurnDelegate
        .getSingleBlockadeThenHighestToLowestBlockadeDamage(Collections.emptyMap());
    private final Territory territory = new Territory("territoryName", gameData);

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreNull() {
      assertThat(comparator.compare(null, null), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreSame() {
      assertThat(comparator.compare(territory, territory), is(0));
    }

    @Test
    void shouldReturnZeroWhenBothTerritoriesAreEqual() {
      assertThat(comparator.compare(territory, new Territory(territory.getName(), gameData)), is(0));
    }

    @Test
    void shouldReturnLessThanZeroWhenFirstTerritoryIsNonNullAndSecondTerritoryIsNull() {
      assertThat(comparator.compare(territory, null), is(lessThan(0)));
    }

    @Test
    void shouldReturnGreaterThanZeroWhenFirstTerritoryIsNullAndSecondTerritoryIsNonNull() {
      assertThat(comparator.compare(null, territory), is(greaterThan(0)));
    }
  }
}
