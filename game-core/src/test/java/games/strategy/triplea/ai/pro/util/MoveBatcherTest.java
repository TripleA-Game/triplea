package games.strategy.triplea.ai.pro.util;

import static games.strategy.triplea.delegate.GameDataTestUtil.americans;
import static games.strategy.triplea.delegate.GameDataTestUtil.armour;
import static games.strategy.triplea.delegate.GameDataTestUtil.infantry;
import static games.strategy.triplea.delegate.GameDataTestUtil.territory;
import static games.strategy.triplea.delegate.GameDataTestUtil.transport;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.common.collect.Maps;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.xml.TestMapGameData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class MoveBatcherTest {
  private final GameData gameData = TestMapGameData.REVISED.getGameData();
  private final Territory brazil = territory("Brazil", gameData);
  private final Territory sz19 = territory("19 Sea Zone", gameData);
  private final Territory sz18 = territory("18 Sea Zone", gameData);
  private final Territory sz12 = territory("12 Sea Zone", gameData);
  private final Territory algeria = territory("Algeria", gameData);
  private final Route brazilToSz18 = new Route(brazil, sz18);
  private final Route sz19ToSz18 = new Route(sz19, sz18);
  private final Route sz18ToSz12 = new Route(sz18, sz12);
  private final Route sz12ToAlgeria = new Route(sz12, algeria);
  private final Unit inf1 = infantry(gameData).create(americans(gameData));
  private final Unit inf2 = infantry(gameData).create(americans(gameData));
  private final Unit tank1 = armour(gameData).create(americans(gameData));
  private final Unit tank2 = armour(gameData).create(americans(gameData));
  private final Unit transport1 = transport(gameData).create(americans(gameData));
  private final Unit transport2 = transport(gameData).create(americans(gameData));

  public MoveBatcherTest() throws Exception {}

  private static ArrayList<Unit> unitList(final Unit... units) {
    return new ArrayList<>(List.of(units));
  }

  @Test
  public void testMoveUnitsWithMultipleTransports() {
    final MoveBatcher moves = new MoveBatcher();

    // Load two units onto transport 1 then move and unload them.
    moves.newSequence();
    moves.addTransportLoad(inf1, brazilToSz18, transport1);
    moves.addTransportLoad(tank1, brazilToSz18, transport1);
    moves.addMove(unitList(transport1, tank1, inf1), sz18ToSz12);
    moves.addMove(unitList(tank1, inf1), sz12ToAlgeria);

    // Load two units onto transport 2 then move and unload them, in
    // the same territories as the previous sequence.
    moves.newSequence();
    moves.addTransportLoad(tank2, brazilToSz18, transport2);
    moves.addTransportLoad(inf2, brazilToSz18, transport2);
    moves.addMove(unitList(transport2, inf2, tank2), sz18ToSz12);
    moves.addMove(unitList(inf2, tank2), sz12ToAlgeria);

    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Map<Unit, Unit>> unitsToTransports = new ArrayList<>();
    moves.batchAndEmit(moveUnits, moveRoutes, unitsToTransports);

    // After batching, there should be 3 moves:
    //   Move to load all the land units onto transports.
    //   Move transporting all the units.
    //   Move unloading the land units from the transports.
    assertEquals(3, moveUnits.size());
    assertEquals(3, moveRoutes.size());
    assertEquals(3, unitsToTransports.size());

    // Check move to load all the land units onto transports.
    assertEquals(unitList(inf1, tank1, tank2, inf2), moveUnits.get(0));
    assertEquals(brazilToSz18, moveRoutes.get(0));
    final var expected =
        Map.of(inf1, transport1, tank1, transport1, inf2, transport2, tank2, transport2);
    assertTrue(Maps.difference(expected, unitsToTransports.get(0)).areEqual());

    // Check move transporting all the units.
    assertEquals(unitList(transport1, tank1, inf1, transport2, inf2, tank2), moveUnits.get(1));
    assertEquals(sz18ToSz12, moveRoutes.get(1));
    assertEquals(null, unitsToTransports.get(1));

    // Check move unloading the land units from the transports.
    assertEquals(unitList(tank1, inf1, inf2, tank2), moveUnits.get(2));
    assertEquals(sz12ToAlgeria, moveRoutes.get(2));
    assertEquals(null, unitsToTransports.get(2));
  }

  @Test
  public void testTransportsPickingUpUnitsOnTheWay() {
    final MoveBatcher moves = new MoveBatcher();

    // Move transport1, load a tank, move transport + tank, unload tank.
    moves.newSequence();
    moves.addMove(unitList(transport1), sz19ToSz18);
    moves.addTransportLoad(tank1, brazilToSz18, transport1);
    moves.addMove(unitList(transport1, tank1), sz18ToSz12);
    moves.addMove(unitList(tank1), sz12ToAlgeria);

    // Move transport2, load two infantry, move transport + 2 infantry, unload infantry.
    moves.newSequence();
    moves.addMove(unitList(transport2), sz19ToSz18);
    moves.addTransportLoad(inf1, brazilToSz18, transport2);
    moves.addTransportLoad(inf2, brazilToSz18, transport2);
    moves.addMove(unitList(transport2, inf1, inf2), sz18ToSz12);
    moves.addMove(unitList(inf1, inf2), sz12ToAlgeria);

    final List<Collection<Unit>> moveUnits = new ArrayList<>();
    final List<Route> moveRoutes = new ArrayList<>();
    final List<Map<Unit, Unit>> unitsToTransports = new ArrayList<>();
    moves.batchAndEmit(moveUnits, moveRoutes, unitsToTransports);

    // After batching, there should be 4 moves:
    //   Move transports 1 and 2 into position.
    //   Load tank and two infantry onto the transports.
    //   Move transporting all the units together.
    //   Move to unload the loaded units from the transports.
    assertEquals(4, moveUnits.size());
    assertEquals(4, moveRoutes.size());
    assertEquals(4, unitsToTransports.size());

    // Check move of transports 1 and 2 into position.
    assertEquals(unitList(transport1, transport2), moveUnits.get(0));
    assertEquals(sz19ToSz18, moveRoutes.get(0));
    assertEquals(null, unitsToTransports.get(0));

    // Check load of tank and two infantry onto the transports.
    assertEquals(unitList(tank1, inf1, inf2), moveUnits.get(1));
    assertEquals(brazilToSz18, moveRoutes.get(1));
    final var expected = Map.of(tank1, transport1, inf1, transport2, inf2, transport2);
    assertTrue(Maps.difference(expected, unitsToTransports.get(1)).areEqual());

    // Check move transporting all the units together.
    assertEquals(unitList(transport1, tank1, transport2, inf1, inf2), moveUnits.get(2));
    assertEquals(sz18ToSz12, moveRoutes.get(2));
    assertEquals(null, unitsToTransports.get(2));

    // Check move to unload the loaded units from the transports.
    assertEquals(unitList(tank1, inf1, inf2), moveUnits.get(3));
    assertEquals(sz12ToAlgeria, moveRoutes.get(3));
    assertEquals(null, unitsToTransports.get(3));
  }
}
