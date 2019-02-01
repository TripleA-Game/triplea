package games.strategy.triplea.delegate;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Assertions;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitHolder;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.data.properties.BooleanProperty;
import games.strategy.engine.data.properties.IEditableProperty;
import games.strategy.triplea.Constants;
import games.strategy.triplea.attachments.TechAttachment;
import games.strategy.util.IntegerMap;
import junit.framework.AssertionFailedError;

/**
 * A utility class for GameData test classes.
 */
public final class GameDataTestUtil {
  private GameDataTestUtil() {}

  /**
   * Get the german PlayerId for the given GameData object.
   *
   * @return A german PlayerId.
   */
  public static PlayerId germans(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_GERMANS);
  }

  /**
   * Get the germany PlayerId for the given GameData object.
   *
   * @return A germany PlayerId.
   */
  public static PlayerId germany(final GameData data) {
    return data.getPlayerList().getPlayerId("Germany");
  }

  /**
   * Get the italian PlayerId for the given GameData object.
   *
   * @return A italian PlayerId.
   */
  static PlayerId italians(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_ITALIANS);
  }

  public static PlayerId italy(final GameData data) {
    return data.getPlayerList().getPlayerId("Italy");
  }

  /**
   * Get the russian PlayerId for the given GameData object.
   *
   * @return A russian PlayerId.
   */
  public static PlayerId russians(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_RUSSIANS);
  }

  /**
   * Get the american PlayerId for the given GameData object.
   *
   * @return A american PlayerId.
   */
  public static PlayerId americans(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_AMERICANS);
  }

  /**
   * Get the USA PlayerId for the given GameData object.
   *
   * @return A USA PlayerId.
   */
  public static PlayerId usa(final GameData data) {
    return data.getPlayerList().getPlayerId("Usa");
  }

  /**
   * Get the british PlayerId for the given GameData object.
   *
   * @return A british PlayerId.
   */
  public static PlayerId british(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_BRITISH);
  }

  public static PlayerId britain(final GameData data) {
    return data.getPlayerList().getPlayerId("Britain");
  }

  /**
   * Get the japanese PlayerId for the given GameData object.
   *
   * @return A japanese PlayerId.
   */
  public static PlayerId japanese(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_JAPANESE);
  }

  /**
   * Get the Japan PlayerId for the given GameData object.
   *
   * @return A Japan PlayerId.
   */
  public static PlayerId japan(final GameData data) {
    return data.getPlayerList().getPlayerId("Japan");
  }

  /**
   * Get the chinese PlayerId for the given GameData object.
   *
   * @return A chinese PlayerId.
   */
  public static PlayerId chinese(final GameData data) {
    return data.getPlayerList().getPlayerId(Constants.PLAYER_NAME_CHINESE);
  }

  /**
   * Returns a territory object for the given name in the specified GameData object.
   *
   * @return A Territory matching the given name if present, otherwise throwing an Exception.
   */
  public static Territory territory(final String name, final GameData data) {
    final Territory t = data.getMap().getTerritory(name);
    if (t == null) {
      throw new IllegalStateException("no territory:" + name);
    }
    return t;
  }

  /**
   * Returns an armor UnitType object for the specified GameData object.
   */
  public static UnitType armour(final GameData data) {
    return unitType(Constants.UNIT_TYPE_ARMOUR, data);
  }

  /**
   * Returns an aaGun UnitType object for the specified GameData object.
   */
  public static UnitType aaGun(final GameData data) {
    return unitType(Constants.UNIT_TYPE_AAGUN, data);
  }

  /**
   * Returns a transport UnitType object for the specified GameData object.
   */
  public static UnitType transport(final GameData data) {
    return unitType(Constants.UNIT_TYPE_TRANSPORT, data);
  }

  /**
   * Returns a battleship UnitType object for the specified GameData object.
   */
  public static UnitType battleship(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BATTLESHIP, data);
  }

  /**
   * Returns a germanBattleship UnitType object for the specified GameData object.
   */
  public static UnitType germanBattleship(final GameData data) {
    return unitType("germanBattleship", data);
  }

  /**
   * Returns a carrier UnitType object for the specified GameData object.
   */
  public static UnitType carrier(final GameData data) {
    return unitType(Constants.UNIT_TYPE_CARRIER, data);
  }

  /**
   * Returns a tacBomber UnitType object for the specified GameData object.
   */
  static UnitType tacBomber(final GameData data) {
    return unitType("tactical_bomber", data);
  }

  /**
   * Returns a fighter UnitType object for the specified GameData object.
   */
  public static UnitType fighter(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FIGHTER, data);
  }

  /**
   * Returns a destroyer UnitType object for the specified GameData object.
   */
  public static UnitType destroyer(final GameData data) {
    return unitType(Constants.UNIT_TYPE_DESTROYER, data);
  }

  /**
   * Returns a submarine UnitType object for the specified GameData object.
   */
  public static UnitType submarine(final GameData data) {
    return unitType(Constants.UNIT_TYPE_SUBMARINE, data);
  }

  /**
   * Returns an infantry UnitType object for the specified GameData object.
   */
  public static UnitType infantry(final GameData data) {
    return unitType(Constants.UNIT_TYPE_INFANTRY, data);
  }

  /**
   * Returns a germanInfantry UnitType object for the specified GameData object.
   */
  public static UnitType germanInfantry(final GameData data) {
    return unitType("germanInfantry", data);
  }

  public static UnitType italianInfantry(final GameData data) {
    return unitType("italianInfantry", data);
  }

  public static UnitType britishInfantry(final GameData data) {
    return unitType("britishInfantry", data);
  }

  /**
   * Returns a bomber UnitType object for the specified GameData object.
   */
  public static UnitType bomber(final GameData data) {
    return unitType(Constants.UNIT_TYPE_BOMBER, data);
  }

  /**
   * Returns a factory UnitType object for the specified GameData object.
   */
  public static UnitType factory(final GameData data) {
    return unitType(Constants.UNIT_TYPE_FACTORY, data);
  }

  /**
   * Returns a germanFactory UnitType object for the specified GameData object.
   */
  public static UnitType germanFactory(final GameData data) {
    return unitType("germanFactory", data);
  }

  public static UnitType italianFactory(final GameData data) {
    return unitType("italianFactory", data);
  }

  public static UnitType britishFactory(final GameData data) {
    return unitType("britishFactory", data);
  }

  /**
   * Returns a germanFortification UnitType object for the specified GameData object.
   */
  public static UnitType germanFortification(final GameData data) {
    return unitType("germanFortification", data);
  }

  /**
   * Returns a truck UnitType object for the specified GameData object.
   */
  public static UnitType truck(final GameData data) {
    return unitType("Truck", data);
  }

  /**
   * Returns a large truck UnitType object for the specified GameData object.
   */
  public static UnitType largeTruck(final GameData data) {
    return unitType("LargeTruck", data);
  }

  /**
   * Returns a germanTrain UnitType object for the specified GameData object.
   */
  public static UnitType germanTrain(final GameData data) {
    return unitType("germanTrain", data);
  }

  /**
   * Returns a germanRail UnitType object for the specified GameData object.
   */
  public static UnitType germanRail(final GameData data) {
    return unitType("germanRail", data);
  }

  /**
   * Returns a germanMine UnitType object for the specified GameData object.
   */
  public static UnitType germanMine(final GameData data) {
    return unitType("germanMine", data);
  }

  /**
   * Returns a germanAntiTankGun UnitType object for the specified GameData object.
   */
  public static UnitType germanAntiTankGun(final GameData data) {
    return unitType("germanAntiTankGun", data);
  }

  /**
   * Returns a germanATSupport UnitType object for the specified GameData object.
   */
  public static UnitType germanAtSupport(final GameData data) {
    return unitType("germanATSupport", data);
  }

  /**
   * Returns a americanTank UnitType object for the specified GameData object.
   */
  public static UnitType americanAtCounter(final GameData data) {
    return unitType("americanATCounter", data);
  }

  /**
   * Returns a americanTank UnitType object for the specified GameData object.
   */
  public static UnitType americanTank(final GameData data) {
    return unitType("americanTank", data);
  }

  /**
   * Returns a americanCruiser UnitType object for the specified GameData object.
   */
  public static UnitType americanCruiser(final GameData data) {
    return unitType("americanCruiser", data);
  }

  /**
   * Returns a americanStrategicBomber UnitType object for the specified GameData object.
   */
  public static UnitType americanStrategicBomber(final GameData data) {
    return unitType("americanStrategicBomber", data);
  }

  /**
   * Returns a factory_upgrade UnitType object for the specified GameData object.
   */
  public static UnitType factoryUpgrade(final GameData data) {
    return unitType("factory_upgrade", data);
  }

  /**
   * Returns a UnitType object matching the given name for the specified GameData object if present.
   */
  private static UnitType unitType(final String name, final GameData data) {
    return data.getUnitTypeList().getUnitType(name);
  }

  /**
   * Removes all units from the given Collection from the given Territory.
   */
  static void removeFrom(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.removeUnits(t, units));
  }

  /**
   * Adds all units from the given Collection to the given Territory.
   */
  public static void addTo(final Territory t, final Collection<Unit> units) {
    t.getData().performChange(ChangeFactory.addUnits(t, units));
  }

  /**
   * Adds all units from the given Collection to the given PlayerId.
   */
  static void addTo(final PlayerId t, final Collection<Unit> units, final GameData data) {
    data.performChange(ChangeFactory.addUnits(t, units));
  }

  /**
   * Returns a PlaceDelegate from the given GameData object.
   */
  static PlaceDelegate placeDelegate(final GameData data) {
    return (PlaceDelegate) data.getDelegateList().getDelegate("place");
  }

  /**
   * Returns a BattleDelegate from the given GameData object.
   */
  static BattleDelegate battleDelegate(final GameData data) {
    return (BattleDelegate) data.getDelegateList().getDelegate("battle");
  }

  /**
   * Returns a MoveDelegate from the given GameData object.
   */
  static MoveDelegate moveDelegate(final GameData data) {
    return (MoveDelegate) data.getDelegateList().getDelegate("move");
  }

  /**
   * Returns a TechnologyDelegate from the given GameData object.
   */
  static TechnologyDelegate techDelegate(final GameData data) {
    return (TechnologyDelegate) data.getDelegateList().getDelegate("tech");
  }

  /**
   * Returns a PurchaseDelegate from the given GameData object.
   */
  static PurchaseDelegate purchaseDelegate(final GameData data) {
    return (PurchaseDelegate) data.getDelegateList().getDelegate("purchase");
  }

  /**
   * Returns a BidPlaceDelegate from the given GameData object.
   */
  static BidPlaceDelegate bidPlaceDelegate(final GameData data) {
    return (BidPlaceDelegate) data.getDelegateList().getDelegate("placeBid");
  }

  static void load(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final MoveDelegate moveDelegate = moveDelegate(route.getStart().getData());
    final Collection<Unit> transports =
        route.getEnd().getUnitCollection().getMatches(Matches.unitIsOwnedBy(units.iterator().next().getOwner()));
    final String error = moveDelegate.move(units, route, transports);
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  static void move(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error = moveDelegate(route.getStart().getData()).move(units, route);
    if (error != null) {
      throw new AssertionFailedError("Illegal move:" + error);
    }
  }

  static void assertMoveError(final Collection<Unit> units, final Route route) {
    if (units.isEmpty()) {
      throw new AssertionFailedError("No units");
    }
    final String error = moveDelegate(route.getStart().getData()).move(units, route);
    if (error == null) {
      throw new AssertionFailedError("Should not be Legal move");
    }
  }

  static int getIndex(final List<IExecutable> steps, final Class<?> type) {
    int indexOfType = -1;
    int index = 0;
    for (final IExecutable e : steps) {
      if (type.isInstance(e)) {
        if (indexOfType != -1) {
          throw new AssertionFailedError("More than one instance:" + steps);
        }
        indexOfType = index;
      }
      index++;
    }
    if (indexOfType == -1) {
      throw new AssertionFailedError("No instance:" + steps);
    }
    return indexOfType;
  }

  static void setSelectAaCasualties(final GameData data, final boolean val) {
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.CHOOSE_AA)) {
        ((BooleanProperty) property).setValue(val);
        return;
      }
    }
    throw new IllegalStateException();
  }

  static void makeGameLowLuck(final GameData data) {
    for (final IEditableProperty<?> property : data.getProperties().getEditableProperties()) {
      if (property.getName().equals(Constants.LOW_LUCK)) {
        ((BooleanProperty) property).setValue(true);
        return;
      }
    }
    throw new IllegalStateException();
  }

  static void givePlayerRadar(final PlayerId player) {
    TechAttachment.get(player).setAaRadar(Boolean.TRUE.toString());
  }

  /**
   * Helper method to check if a String is null and otherwise print the String.
   */
  static void assertValid(final String string) {
    Assertions.assertNull(string, string);
  }

  /**
   * Helper method to check if a String is not null.
   * In this scenario used to verify an error message exists.
   */
  static void assertError(final String string) {
    Assertions.assertNotNull(string);
  }

  /**
   * Gets a collection of units from the specified unit holder (e.g. territory, player, etc.) consisting of up to the
   * specified maximum count of each specified unit type.
   *
   * @param maxUnitCountsByType The maximum count of each type of unit to include in the returned collection. The key is
   *        the unit type. The value is the maximum unit count.
   * @param from The territory from which the units are to be collected.
   *
   * @return A collection of units from the specified unit holder.
   */
  public static Collection<Unit> getUnits(final IntegerMap<UnitType> maxUnitCountsByType, final UnitHolder from) {
    checkNotNull(maxUnitCountsByType);
    checkNotNull(from);

    return maxUnitCountsByType.entrySet().stream()
        .flatMap(entry -> from.getUnitCollection().getUnits(entry.getKey(), entry.getValue()).stream())
        .collect(Collectors.toList());
  }
}
