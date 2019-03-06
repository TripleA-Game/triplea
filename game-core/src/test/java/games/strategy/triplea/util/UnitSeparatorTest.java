package games.strategy.triplea.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.GameDataTestUtil;
import games.strategy.triplea.ui.mapdata.MapData;
import games.strategy.triplea.xml.TestMapGameData;

@ExtendWith(MockitoExtension.class)
public class UnitSeparatorTest {

  @Mock
  private MapData mockMapData;

  @Test
  void testGetSortedUnitCategories() throws Exception {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final List<Unit> units = new ArrayList<>();
    final PlayerId italians = GameDataTestUtil.italy(data);
    units.addAll(GameDataTestUtil.italianInfantry(data).create(1, italians));
    units.addAll(GameDataTestUtil.italianFactory(data).create(1, italians));
    units.addAll(GameDataTestUtil.truck(data).create(1, italians));
    final PlayerId british = GameDataTestUtil.britain(data);
    units.addAll(GameDataTestUtil.britishInfantry(data).create(1, british));
    units.addAll(GameDataTestUtil.britishFactory(data).create(1, british));
    units.addAll(GameDataTestUtil.truck(data).create(1, british));
    final PlayerId germans = GameDataTestUtil.germany(data);
    units.addAll(GameDataTestUtil.germanInfantry(data).create(1, germans));
    units.addAll(GameDataTestUtil.germanFactory(data).create(1, germans));
    units.addAll(GameDataTestUtil.truck(data).create(1, germans));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(true);
    final List<UnitCategory> categories = UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    expected.add(newUnitCategory("germanFactory", germans, data));
    expected.add(newUnitCategory("Truck", germans, data));
    expected.add(newUnitCategory("germanInfantry", germans, data));
    expected.add(newUnitCategory("italianFactory", italians, data));
    expected.add(newUnitCategory("Truck", italians, data));
    expected.add(newUnitCategory("italianInfantry", italians, data));
    expected.add(newUnitCategory("britishFactory", british, data));
    expected.add(newUnitCategory("Truck", british, data));
    expected.add(newUnitCategory("britishInfantry", british, data));
    assertEquals(expected, categories);
  }

  @Test
  void testGetSortedUnitCategoriesDontDrawUnit() throws Exception {
    final GameData data = TestMapGameData.TWW.getGameData();
    final Territory northernGermany = territory("Northern Germany", data);
    northernGermany.getUnitCollection().clear();
    final PlayerId italians = GameDataTestUtil.italy(data);
    final List<Unit> units = new ArrayList<>(GameDataTestUtil.italianInfantry(data).create(1, italians));
    GameDataTestUtil.addTo(northernGermany, units);
    when(mockMapData.shouldDrawUnit(ArgumentMatchers.anyString())).thenReturn(false);
    final List<UnitCategory> categories = UnitSeparator.getSortedUnitCategories(northernGermany, mockMapData);
    final List<UnitCategory> expected = new ArrayList<>();
    assertEquals(expected, categories);
  }

  private static UnitCategory newUnitCategory(final String unitName, final PlayerId player, final GameData data) {
    return new UnitCategory(new UnitType(unitName, data), player);
  }
}
