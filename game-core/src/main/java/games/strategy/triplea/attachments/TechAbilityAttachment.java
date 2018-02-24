package games.strategy.triplea.attachments;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.AttachmentProperty;
import games.strategy.engine.data.DefaultAttachment;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.annotations.GameProperty;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.delegate.GenericTechAdvance;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TechTracker;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;

/**
 * Attaches to technologies.
 * Also contains static methods of interpreting data from all technology attachments that a player has.
 */
@MapSupport
public class TechAbilityAttachment extends DefaultAttachment {
  private static final long serialVersionUID = 1866305599625384294L;

  /**
   * Convenience method.
   */
  public static TechAbilityAttachment get(final TechAdvance type) {
    if (type instanceof GenericTechAdvance) {
      // generic techs can name a hardcoded tech, therefore if it exists we should use the hard coded tech's attachment.
      // (if the map maker doesn't want to use the hardcoded tech's attachment, they should not name a hardcoded tech)
      final TechAdvance hardCodedAdvance = ((GenericTechAdvance) type).getAdvance();
      if (hardCodedAdvance != null) {
        final TechAbilityAttachment hardCodedTechAttachment =
            (TechAbilityAttachment) hardCodedAdvance.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
        return hardCodedTechAttachment;
      }
    }
    return (TechAbilityAttachment) type.getAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME);
  }

  /**
   * Convenience method.
   */
  public static TechAbilityAttachment get(final TechAdvance type, final String nameOfAttachment) {
    return getAttachment(type, nameOfAttachment, TechAbilityAttachment.class);
  }

  // unitAbilitiesGained Static Strings
  public static final String ABILITY_CAN_BLITZ = "canBlitz";
  public static final String ABILITY_CAN_BOMBARD = "canBombard";
  // attachment fields
  private IntegerMap<UnitType> m_attackBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_defenseBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_movementBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_radarBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_airAttackBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_airDefenseBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_productionBonus = new IntegerMap<>();
  // -1 means not set
  private int m_minimumTerritoryValueForProductionBonus = -1;
  // -1 means not set
  private int m_repairDiscount = -1;
  // -1 means not set
  private int m_warBondDiceSides = -1;
  private int m_warBondDiceNumber = 0;
  // -1 means not set // not needed because this is controlled in the unit attachment with
  // private int m_rocketDiceSides = -1;
  // bombingBonus and bombingMaxDieSides
  private IntegerMap<UnitType> m_rocketDiceNumber = new IntegerMap<>();
  private int m_rocketDistance = 0;
  private int m_rocketNumberPerTerritory = 0;
  private Map<UnitType, Set<String>> m_unitAbilitiesGained = new HashMap<>();
  private boolean m_airborneForces = false;
  private IntegerMap<UnitType> m_airborneCapacity = new IntegerMap<>();
  private Set<UnitType> m_airborneTypes = new HashSet<>();
  private int m_airborneDistance = 0;
  private Set<UnitType> m_airborneBases = new HashSet<>();
  private Map<String, Set<UnitType>> m_airborneTargettedByAA = new HashMap<>();
  private IntegerMap<UnitType> m_attackRollsBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_defenseRollsBonus = new IntegerMap<>();
  private IntegerMap<UnitType> m_bombingBonus = new IntegerMap<>();

  public TechAbilityAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private UnitType getUnitType(final String value) throws GameParseException {
    return Optional.ofNullable(getData().getUnitTypeList().getUnitType(value))
        .orElseThrow(() -> new GameParseException("No unit called:" + value + thisErrorMsg()));
  }

  @VisibleForTesting
  String[] splitAndValidate(final String name, final String value) throws GameParseException {
    final String[] stringArray = value.split(":");
    if (value.isEmpty() || stringArray.length > 2) {
      throw new GameParseException(
          String.format("%s cannot be empty or have more than two fields %s", name, thisErrorMsg()));
    }
    return stringArray;
  }

  private void applyCheckedValue(
      final String name,
      final String value,
      final BiConsumer<UnitType, Integer> putter) throws GameParseException {
    final String[] s = splitAndValidate(name, value);
    putter.accept(getUnitType(s[1]), getInt(s[0]));
  }

  private static int sumIntegerMap(final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper,
      final UnitType ut,
      final PlayerID player,
      final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(mapper)
        .mapToInt(m -> m.getInt(ut))
        .sum();
  }

  private static int sumNumbers(
      final ToIntFunction<TechAbilityAttachment> mapper,
      final PlayerID player,
      final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(mapper)
        .filter(i -> i > 0)
        .sum();
  }

  private int getIntInRange(final String name, final String value, final int max, final boolean allowUndefined)
      throws GameParseException {
    final int intValue = getInt(value);
    if (intValue < (allowUndefined ? -1 : 0) || intValue > max) {
      throw new GameParseException(String.format(
          "%s must be%s between 0 and %s%s",
          name,
          allowUndefined ? " -1 (no effect), or be" : "",
          max,
          thisErrorMsg()));
    }
    return intValue;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAttackBonus(final String value) throws GameParseException {
    applyCheckedValue("attackBonus", value, m_attackBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackBonus(final IntegerMap<UnitType> value) {
    m_attackBonus = value;
  }

  public IntegerMap<UnitType> getAttackBonus() {
    return m_attackBonus;
  }

  static int getAttackBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAttackBonus, ut, player, data);
  }

  public void clearAttackBonus() {
    m_attackBonus.clear();
  }

  public void resetAttackBonus() {
    m_attackBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setDefenseBonus(final String value) throws GameParseException {
    applyCheckedValue("defenseBonus", value, m_defenseBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefenseBonus(final IntegerMap<UnitType> value) {
    m_defenseBonus = value;
  }

  public IntegerMap<UnitType> getDefenseBonus() {
    return m_defenseBonus;
  }

  static int getDefenseBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getDefenseBonus, ut, player, data);
  }

  public void clearDefenseBonus() {
    m_defenseBonus.clear();
  }

  public void resetDefenseBonus() {
    m_defenseBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setMovementBonus(final String value) throws GameParseException {
    applyCheckedValue("movementBonus", value, m_movementBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMovementBonus(final IntegerMap<UnitType> value) {
    m_movementBonus = value;
  }

  public IntegerMap<UnitType> getMovementBonus() {
    return m_movementBonus;
  }

  static int getMovementBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getMovementBonus, ut, player, data);
  }

  public void clearMovementBonus() {
    m_movementBonus.clear();
  }

  public void resetMovementBonus() {
    m_movementBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRadarBonus(final String value) throws GameParseException {
    applyCheckedValue("radarBonus", value, m_radarBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRadarBonus(final IntegerMap<UnitType> value) {
    m_radarBonus = value;
  }

  public IntegerMap<UnitType> getRadarBonus() {
    return m_radarBonus;
  }

  static int getRadarBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getRadarBonus, ut, player, data);
  }

  public void clearRadarBonus() {
    m_radarBonus.clear();
  }

  public void resetRadarBonus() {
    m_radarBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirAttackBonus(final String value) throws GameParseException {
    applyCheckedValue("airAttackBonus", value, m_airAttackBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirAttackBonus(final IntegerMap<UnitType> value) {
    m_airAttackBonus = value;
  }

  public IntegerMap<UnitType> getAirAttackBonus() {
    return m_airAttackBonus;
  }

  static int getAirAttackBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAirAttackBonus, ut, player, data);
  }

  public void clearAirAttackBonus() {
    m_airAttackBonus.clear();
  }

  public void resetAirAttackBonus() {
    m_airAttackBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirDefenseBonus(final String value) throws GameParseException {
    applyCheckedValue("airDefenseBonus", value, m_airDefenseBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirDefenseBonus(final IntegerMap<UnitType> value) {
    m_airDefenseBonus = value;
  }

  public IntegerMap<UnitType> getAirDefenseBonus() {
    return m_airDefenseBonus;
  }

  static int getAirDefenseBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAirDefenseBonus, ut, player, data);
  }

  public void clearAirDefenseBonus() {
    m_airDefenseBonus.clear();
  }

  public void resetAirDefenseBonus() {
    m_airDefenseBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setProductionBonus(final String value) throws GameParseException {
    applyCheckedValue("productionBonus", value, m_productionBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setProductionBonus(final IntegerMap<UnitType> value) {
    m_productionBonus = value;
  }

  public IntegerMap<UnitType> getProductionBonus() {
    return m_productionBonus;
  }

  public static int getProductionBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getProductionBonus, ut, player, data);
  }

  public void clearProductionBonus() {
    m_productionBonus.clear();
  }

  public void resetProductionBonus() {
    m_productionBonus = new IntegerMap<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMinimumTerritoryValueForProductionBonus(final String value) throws GameParseException {
    m_minimumTerritoryValueForProductionBonus =
        getIntInRange("minimumTerritoryValueForProductionBonus", value, 10000, true);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setMinimumTerritoryValueForProductionBonus(final Integer value) {
    m_minimumTerritoryValueForProductionBonus = value;
  }

  public int getMinimumTerritoryValueForProductionBonus() {
    return m_minimumTerritoryValueForProductionBonus;
  }

  public static int getMinimumTerritoryValueForProductionBonus(final PlayerID player, final GameData data) {
    return Math.max(0, TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(TechAbilityAttachment::getMinimumTerritoryValueForProductionBonus)
        .filter(i -> i != -1)
        .min()
        .orElse(-1));
  }

  public void resetMinimumTerritoryValueForProductionBonus() {
    m_minimumTerritoryValueForProductionBonus = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRepairDiscount(final String value) throws GameParseException {
    m_repairDiscount = getIntInRange("repairDiscount", value, 100, true);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRepairDiscount(final Integer value) {
    m_repairDiscount = value;
  }

  public int getRepairDiscount() {
    return m_repairDiscount;
  }

  public static double getRepairDiscount(final PlayerID player, final GameData data) {
    return Math.max(0, 1.0 - TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(TechAbilityAttachment::getRepairDiscount)
        .filter(i -> i != -1)
        .mapToDouble(d -> d / 100.0)
        .sum());
  }

  public void resetRepairDiscount() {
    m_repairDiscount = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBondDiceSides(final String value) throws GameParseException {
    m_warBondDiceSides = getIntInRange("warBondDiceSides", value, 200, true);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBondDiceSides(final Integer value) {
    m_warBondDiceSides = value;
  }

  public int getWarBondDiceSides() {
    return m_warBondDiceSides;
  }

  public static int getWarBondDiceSides(final PlayerID player, final GameData data) {
    return sumNumbers(TechAbilityAttachment::getWarBondDiceSides, player, data);
  }

  public void resetWarBondDiceSides() {
    m_warBondDiceSides = -1;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBondDiceNumber(final String value) throws GameParseException {
    m_warBondDiceNumber = getIntInRange("warBondDiceNumber", value, 100, false);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setWarBondDiceNumber(final Integer value) {
    m_warBondDiceNumber = value;
  }

  public int getWarBondDiceNumber() {
    return m_warBondDiceNumber;
  }

  public static int getWarBondDiceNumber(final PlayerID player, final GameData data) {
    return sumNumbers(TechAbilityAttachment::getWarBondDiceNumber, player, data);
  }

  public void resetWarBondDiceNumber() {
    m_warBondDiceNumber = 0;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setRocketDiceNumber(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length != 2) {
      throw new GameParseException("rocketDiceNumber must have two fields" + thisErrorMsg());
    }
    m_rocketDiceNumber.put(getUnitType(s[1]), getInt(s[0]));
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocketDiceNumber(final IntegerMap<UnitType> value) {
    m_rocketDiceNumber = value;
  }

  public IntegerMap<UnitType> getRocketDiceNumber() {
    return m_rocketDiceNumber;
  }

  private static int getRocketDiceNumber(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getRocketDiceNumber, ut, player, data);
  }

  public static int getRocketDiceNumber(final Collection<Unit> rockets, final GameData data) {
    int rocketDiceNumber = 0;
    for (final Unit u : rockets) {
      rocketDiceNumber += getRocketDiceNumber(u.getType(), u.getOwner(), data);
    }
    return rocketDiceNumber;
  }

  public void clearRocketDiceNumber() {
    m_rocketDiceNumber.clear();
  }

  public void resetRocketDiceNumber() {
    m_rocketDiceNumber = new IntegerMap<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocketDistance(final String value) throws GameParseException {
    m_rocketDistance = getIntInRange("rocketDistance", value, 100, false);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocketDistance(final Integer value) {
    m_rocketDistance = value;
  }

  public int getRocketDistance() {
    return m_rocketDistance;
  }

  public static int getRocketDistance(final PlayerID player, final GameData data) {
    return sumNumbers(TechAbilityAttachment::getRocketDistance, player, data);
  }

  public void resetRocketDistance() {
    m_rocketDistance = 0;
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocketNumberPerTerritory(final String value) throws GameParseException {
    m_rocketNumberPerTerritory = getIntInRange("rocketNumberPerTerritory", value, 200, false);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setRocketNumberPerTerritory(final Integer value) {
    m_rocketNumberPerTerritory = value;
  }

  public int getRocketNumberPerTerritory() {
    return m_rocketNumberPerTerritory;
  }

  public static int getRocketNumberPerTerritory(final PlayerID player, final GameData data) {
    return sumNumbers(TechAbilityAttachment::getRocketNumberPerTerritory, player, data);
  }

  public void resetRocketNumberPerTerritory() {
    m_rocketNumberPerTerritory = 0;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setUnitAbilitiesGained(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length < 2) {
      throw new GameParseException(
          "unitAbilitiesGained must list the unit type, then all abilities gained" + thisErrorMsg());
    }
    final String unitType = s[0];
    // validate that this unit exists in the xml
    final UnitType ut = getUnitType(unitType);
    final Set<String> abilities = m_unitAbilitiesGained.getOrDefault(ut, new HashSet<>());
    // start at 1
    for (int i = 1; i < s.length; i++) {
      final String ability = s[i];
      if (!(ability.equals(ABILITY_CAN_BLITZ) || ability.equals(ABILITY_CAN_BOMBARD))) {
        throw new GameParseException("unitAbilitiesGained so far only supports: " + ABILITY_CAN_BLITZ + " and "
            + ABILITY_CAN_BOMBARD + thisErrorMsg());
      }
      abilities.add(ability);
    }
    m_unitAbilitiesGained.put(ut, abilities);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setUnitAbilitiesGained(final Map<UnitType, Set<String>> value) {
    m_unitAbilitiesGained = value;
  }

  public Map<UnitType, Set<String>> getUnitAbilitiesGained() {
    return m_unitAbilitiesGained;
  }

  public static boolean getUnitAbilitiesGained(final String filterForAbility, final UnitType ut, final PlayerID player,
      final GameData data) {
    Preconditions.checkNotNull(filterForAbility);
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getUnitAbilitiesGained)
        .map(m -> m.get(ut))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .anyMatch(filterForAbility::equals);
  }

  public void clearUnitAbilitiesGained() {
    m_unitAbilitiesGained.clear();
  }

  public void resetUnitAbilitiesGained() {
    m_unitAbilitiesGained = new HashMap<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneForces(final String value) {
    m_airborneForces = getBool(value);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneForces(final Boolean value) {
    m_airborneForces = value;
  }

  public boolean getAirborneForces() {
    return m_airborneForces;
  }

  public void resetAirborneForces() {
    m_airborneForces = false;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirborneCapacity(final String value) throws GameParseException {
    applyCheckedValue("airborneCapacity", value, m_airborneCapacity::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneCapacity(final IntegerMap<UnitType> value) {
    m_airborneCapacity = value;
  }

  public IntegerMap<UnitType> getAirborneCapacity() {
    return m_airborneCapacity;
  }

  public static IntegerMap<UnitType> getAirborneCapacity(final PlayerID player, final GameData data) {
    final IntegerMap<UnitType> capacityMap = new IntegerMap<>();
    for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data)) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        capacityMap.add(taa.getAirborneCapacity());
      }
    }
    return capacityMap;
  }

  public static int getAirborneCapacity(final Collection<Unit> units, final PlayerID player, final GameData data) {
    final IntegerMap<UnitType> capacityMap = getAirborneCapacity(player, data);
    int airborneCapacity = 0;
    for (final Unit u : units) {
      airborneCapacity += Math.max(0, (capacityMap.getInt(u.getType()) - ((TripleAUnit) u).getLaunched()));
    }
    return airborneCapacity;
  }

  public void clearAirborneCapacity() {
    m_airborneCapacity.clear();
  }

  public void resetAirborneCapacity() {
    m_airborneCapacity = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirborneTypes(final String value) throws GameParseException {
    for (final String unit : value.split(":")) {
      m_airborneTypes.add(getUnitType(unit));
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneTypes(final Set<UnitType> value) {
    m_airborneTypes = value;
  }

  public Set<UnitType> getAirborneTypes() {
    return m_airborneTypes;
  }

  public static Set<UnitType> getAirborneTypes(final PlayerID player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneTypes)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public void clearAirborneTypes() {
    m_airborneTypes.clear();
  }

  public void resetAirborneTypes() {
    m_airborneTypes = new HashSet<>();
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneDistance(final String value) throws GameParseException {
    m_airborneDistance = getIntInRange("airborneDistance", value, 100, false);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneDistance(final Integer value) {
    m_airborneDistance = value;
  }

  public int getAirborneDistance() {
    return m_airborneDistance;
  }

  public static int getAirborneDistance(final PlayerID player, final GameData data) {
    return Math.max(0, TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .mapToInt(TechAbilityAttachment::getAirborneDistance)
        .sum());
  }

  public void resetAirborneDistance() {
    m_airborneDistance = 0;
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirborneBases(final String value) throws GameParseException {
    for (final String u : value.split(":")) {
      m_airborneBases.add(getUnitType(u));
    }
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneBases(final Set<UnitType> value) {
    m_airborneBases = value;
  }

  public Set<UnitType> getAirborneBases() {
    return m_airborneBases;
  }

  public static Set<UnitType> getAirborneBases(final PlayerID player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getAirborneBases)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }

  public void clearAirborneBases() {
    m_airborneBases.clear();
  }

  public void resetAirborneBases() {
    m_airborneBases = new HashSet<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAirborneTargettedByAA(final String value) throws GameParseException {
    final String[] s = value.split(":");
    if (s.length < 2) {
      throw new GameParseException("airborneTargettedByAA must have at least two fields" + thisErrorMsg());
    }
    final Set<UnitType> unitTypes = new HashSet<>();
    for (int i = 1; i < s.length; i++) {
      unitTypes.add(getUnitType(s[i]));
    }
    m_airborneTargettedByAA.put(s[0], unitTypes);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAirborneTargettedByAA(final Map<String, Set<UnitType>> value) {
    m_airborneTargettedByAA = value;
  }

  public Map<String, Set<UnitType>> getAirborneTargettedByAA() {
    return m_airborneTargettedByAA;
  }

  public static HashMap<String, HashSet<UnitType>> getAirborneTargettedByAA(final PlayerID player,
      final GameData data) {
    final HashMap<String, HashSet<UnitType>> airborneTargettedByAa = new HashMap<>();
    for (final TechAdvance ta : TechTracker.getCurrentTechAdvances(player, data)) {
      final TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa != null) {
        final Map<String, Set<UnitType>> mapAa = taa.getAirborneTargettedByAA();
        if (mapAa != null && !mapAa.isEmpty()) {
          for (final Entry<String, Set<UnitType>> entry : mapAa.entrySet()) {
            final HashSet<UnitType> current = airborneTargettedByAa.getOrDefault(entry.getKey(), new HashSet<>());
            current.addAll(entry.getValue());
            airborneTargettedByAa.put(entry.getKey(), current);
          }
        }
      }
    }
    return airborneTargettedByAa;
  }

  public void clearAirborneTargettedByAA() {
    m_airborneTargettedByAA.clear();
  }

  public void resetAirborneTargettedByAA() {
    m_airborneTargettedByAA = new HashMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setAttackRollsBonus(final String value) throws GameParseException {
    applyCheckedValue("attackRollsBonus", value, m_attackRollsBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setAttackRollsBonus(final IntegerMap<UnitType> value) {
    m_attackRollsBonus = value;
  }

  public IntegerMap<UnitType> getAttackRollsBonus() {
    return m_attackRollsBonus;
  }

  static int getAttackRollsBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getAttackRollsBonus, ut, player, data);
  }

  public void clearAttackRollsBonus() {
    m_attackRollsBonus.clear();
  }

  public void resetAttackRollsBonus() {
    m_attackRollsBonus = new IntegerMap<>();
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setDefenseRollsBonus(final String value) throws GameParseException {
    applyCheckedValue("defenseRollsBonus", value, m_defenseRollsBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setDefenseRollsBonus(final IntegerMap<UnitType> value) {
    m_defenseRollsBonus = value;
  }

  public IntegerMap<UnitType> getDefenseRollsBonus() {
    return m_defenseRollsBonus;
  }

  static int getDefenseRollsBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getDefenseRollsBonus, ut, player, data);
  }

  /**
   * Adds to, not sets. Anything that adds to instead of setting needs a clear function as well.
   */
  @GameProperty(xmlProperty = true, gameProperty = true, adds = true)
  public void setBombingBonus(final String value) throws GameParseException {
    applyCheckedValue("bombingBonus", value, m_bombingBonus::put);
  }

  @GameProperty(xmlProperty = true, gameProperty = true, adds = false)
  public void setBombingBonus(final IntegerMap<UnitType> value) {
    m_bombingBonus = value;
  }

  public IntegerMap<UnitType> getBombingBonus() {
    return m_bombingBonus;
  }

  public static int getBombingBonus(final UnitType ut, final PlayerID player, final GameData data) {
    return sumIntegerMap(TechAbilityAttachment::getBombingBonus, ut, player, data);
  }

  public void clearDefenseRollsBonus() {
    m_defenseRollsBonus.clear();
  }

  public void resetDefenseRollsBonus() {
    m_defenseRollsBonus = new IntegerMap<>();
  }

  public void clearBombingBonus() {
    m_bombingBonus.clear();
  }

  public void resetBombingBonus() {
    m_bombingBonus = new IntegerMap<>();
  }

  public static boolean getAllowAirborneForces(final PlayerID player, final GameData data) {
    return TechTracker.getCurrentTechAdvances(player, data).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .anyMatch(TechAbilityAttachment::getAirborneForces);
  }

  /**
   * Must be done only in GameParser, and only after we have already parsed ALL technologies, attachments, and game
   * options/properties.
   */
  @InternalDoNotExport
  public static void setDefaultTechnologyAttachments(final GameData data) throws GameParseException {
    // loop through all technologies. any "default/hard-coded" tech that doesn't have an attachment, will get its
    // "default" attachment. any
    // non-default tech are ignored.
    for (final TechAdvance techAdvance : TechAdvance.getTechAdvances(data)) {
      final TechAdvance ta;
      if (techAdvance instanceof GenericTechAdvance) {
        final TechAdvance adv = ((GenericTechAdvance) techAdvance).getAdvance();
        if (adv != null) {
          ta = adv;
        } else {
          continue;
        }
      } else {
        ta = techAdvance;
      }
      final String propertyString = ta.getProperty();
      TechAbilityAttachment taa = TechAbilityAttachment.get(ta);
      if (taa == null) {
        // debating if we should have flags for things like "air", "land", "sea", "aaGun", "factory", "strategic
        // bomber", etc.
        // perhaps just the easy ones, of air, land, and sea?
        if (propertyString.equals(TechAdvance.TECH_PROPERTY_LONG_RANGE_AIRCRAFT)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allAir =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAir());
          for (final UnitType air : allAir) {
            taa.setMovementBonus("2:" + air.getName());
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_AA_RADAR)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allAa =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsAaForAnything());
          for (final UnitType aa : allAa) {
            taa.setRadarBonus("1:" + aa.getName());
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_SUPER_SUBS)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allSubs =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsSub());
          for (final UnitType sub : allSubs) {
            taa.setAttackBonus("1:" + sub.getName());
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_JET_POWER)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allJets = CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(),
              Matches.unitTypeIsAir().and(Matches.unitTypeIsStrategicBomber().negate()));
          final boolean ww2v3TechModel = Properties.getWW2V3TechModel(data);
          for (final UnitType jet : allJets) {
            if (ww2v3TechModel) {
              taa.setAttackBonus("1:" + jet.getName());
              taa.setAirAttackBonus("1:" + jet.getName());
            } else {
              taa.setDefenseBonus("1:" + jet.getName());
              taa.setAirDefenseBonus("1:" + jet.getName());
            }
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_INCREASED_FACTORY_PRODUCTION)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allFactories =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeCanProduceUnits());
          for (final UnitType factory : allFactories) {
            taa.setProductionBonus("2:" + factory.getName());
            taa.setMinimumTerritoryValueForProductionBonus("3");
            // means a 50% discount, which is half price
            taa.setRepairDiscount("50");
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_WAR_BONDS)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          taa.setWarBondDiceSides(Integer.toString(data.getDiceSides()));
          taa.setWarBondDiceNumber("1");
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_ROCKETS)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allRockets =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsRocket());
          for (final UnitType rocket : allRockets) {
            taa.setRocketDiceNumber("1:" + rocket.getName());
          }
          taa.setRocketDistance("3");
          taa.setRocketNumberPerTerritory("1");
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_DESTROYER_BOMBARD)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allDestroyers =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsDestroyer());
          for (final UnitType destroyer : allDestroyers) {
            taa.setUnitAbilitiesGained(destroyer.getName() + ":" + ABILITY_CAN_BOMBARD);
          }
        } else if (propertyString.equals(TechAdvance.TECH_PROPERTY_HEAVY_BOMBER)) {
          taa = new TechAbilityAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, ta, data);
          ta.addAttachment(Constants.TECH_ABILITY_ATTACHMENT_NAME, taa);
          final List<UnitType> allBombers =
              CollectionUtils.getMatches(data.getUnitTypeList().getAllUnitTypes(), Matches.unitTypeIsStrategicBomber());
          final int heavyBomberDiceRollsTotal = Properties.getHeavyBomberDiceRolls(data);
          final boolean heavyBombersLhtr = Properties.getLhtrHeavyBombers(data);
          for (final UnitType bomber : allBombers) {
            // TODO: The bomber dice rolls get set when the xml is parsed.
            // we subtract the base rolls to get the bonus
            final int heavyBomberDiceRollsBonus =
                heavyBomberDiceRollsTotal - UnitAttachment.get(bomber).getAttackRolls(PlayerID.NULL_PLAYERID);
            taa.setAttackRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
            if (heavyBombersLhtr) {
              // TODO: this all happens WHEN the xml is parsed. Which means if the user changes the game options, this
              // does not get changed.
              // (meaning, turning on LHTR bombers will not result in this bonus damage, etc. It would have to start on,
              // in the xml.)
              taa.setDefenseRollsBonus(heavyBomberDiceRollsBonus + ":" + bomber.getName());
              // LHTR adds 1 to base roll
              taa.setBombingBonus("1:" + bomber.getName());
            }
          }
        }
        // The following technologies should NOT have ability attachments for them:
        // shipyards and industrialTechnology = because it is better to use a Trigger to change player's production
        // improvedArtillerySupport = because it is already completely atomized and controlled through support
        // attachments
        // paratroopers = because it is already completely atomized and controlled through unit attachments + game
        // options
        // mechanizedInfantry = because it is already completely atomized and controlled through unit attachments
        // IF one of the above named techs changes what it does in a future version of a&a, and the change is large
        // enough or different
        // enough that it cannot be done easily with a new game option,
        // then it is better to create a new tech rather than change the old one, and give the new one a new name, like
        // paratroopers2 or
        // paratroopersAttack or Airborne_Forces, or some crap.
      }
    }
  }

  // validator
  @Override
  public void validate(final GameData data) throws GameParseException {
    final TechAdvance ta = (TechAdvance) this.getAttachedTo();
    if (ta instanceof GenericTechAdvance) {
      final TechAdvance hardCodedAdvance = ((GenericTechAdvance) ta).getAdvance();
      if (hardCodedAdvance != null) {
        throw new GameParseException(
            "A custom Generic Tech Advance naming a hardcoded tech, may not have a Tech Ability Attachment!"
                + this.thisErrorMsg());
      }
    }
  }

  private Map<String, AttachmentProperty<?>> createPropertyMap() {
    return ImmutableMap.<String, AttachmentProperty<?>>builder()
        .put("attackBonus",
            AttachmentProperty.of(
                this::setAttackBonus,
                this::setAttackBonus,
                this::getAttackBonus,
                this::resetAttackBonus))
        .put("defenseBonus",
            AttachmentProperty.of(
                this::setDefenseBonus,
                this::setDefenseBonus,
                this::getDefenseBonus,
                this::resetDefenseBonus))
        .put("movementBonus",
            AttachmentProperty.of(
                this::setMovementBonus,
                this::setMovementBonus,
                this::getMovementBonus,
                this::resetMovementBonus))
        .put("radarBonus",
            AttachmentProperty.of(
                this::setRadarBonus,
                this::setRadarBonus,
                this::getRadarBonus,
                this::resetRadarBonus))
        .put("airAttackBonus",
            AttachmentProperty.of(
                this::setAirAttackBonus,
                this::setAirAttackBonus,
                this::getAirAttackBonus,
                this::resetAirAttackBonus))
        .put("airDefenseBonus",
            AttachmentProperty.of(
                this::setAirDefenseBonus,
                this::setAirDefenseBonus,
                this::getAirDefenseBonus,
                this::resetAirDefenseBonus))
        .put("productionBonus",
            AttachmentProperty.of(
                this::setProductionBonus,
                this::setProductionBonus,
                this::getProductionBonus,
                this::resetProductionBonus))
        .put("minimumTerritoryValueForProductionBonus",
            AttachmentProperty.of(
                this::setMinimumTerritoryValueForProductionBonus,
                this::setMinimumTerritoryValueForProductionBonus,
                this::getMinimumTerritoryValueForProductionBonus,
                this::resetMinimumTerritoryValueForProductionBonus))
        .put("repairDiscount",
            AttachmentProperty.of(
                this::setRepairDiscount,
                this::setRepairDiscount,
                this::getRepairDiscount,
                this::resetRepairDiscount))
        .put("warBondDiceSides",
            AttachmentProperty.of(
                this::setWarBondDiceSides,
                this::setWarBondDiceSides,
                this::getWarBondDiceSides,
                this::resetWarBondDiceSides))
        .put("warBondDiceNumber",
            AttachmentProperty.of(
                this::setWarBondDiceNumber,
                this::setWarBondDiceNumber,
                this::getWarBondDiceNumber,
                this::resetWarBondDiceNumber))
        .put("rocketDiceNumber",
            AttachmentProperty.of(
                this::setRocketDiceNumber,
                this::setRocketDiceNumber,
                this::getRocketDiceNumber,
                this::resetRocketDiceNumber))
        .put("rocketDistance",
            AttachmentProperty.of(
                this::setRocketDistance,
                this::setRocketDistance,
                this::getRocketDistance,
                this::resetRocketDistance))
        .put("rocketNumberPerTerritory",
            AttachmentProperty.of(
                this::setRocketNumberPerTerritory,
                this::setRocketNumberPerTerritory,
                this::getRocketNumberPerTerritory,
                this::resetRocketNumberPerTerritory))
        .put("unitAbilitiesGained",
            AttachmentProperty.of(
                this::setUnitAbilitiesGained,
                this::setUnitAbilitiesGained,
                this::getUnitAbilitiesGained,
                this::resetUnitAbilitiesGained))
        .put("airborneForces",
            AttachmentProperty.of(
                this::setAirborneForces,
                this::setAirborneForces,
                this::getAirborneForces,
                this::resetAirborneForces))
        .put("airborneCapacity",
            AttachmentProperty.of(
                this::setAirborneCapacity,
                this::setAirborneCapacity,
                this::getAirborneCapacity,
                this::resetAirborneCapacity))
        .put("airborneTypes",
            AttachmentProperty.of(
                this::setAirborneTypes,
                this::setAirborneTypes,
                this::getAirborneTypes,
                this::resetAirborneTypes))
        .put("airborneDistance",
            AttachmentProperty.of(
                this::setAirborneDistance,
                this::setAirborneDistance,
                this::getAirborneDistance,
                this::resetAirborneDistance))
        .put("airborneBases",
            AttachmentProperty.of(
                this::setAirborneBases,
                this::setAirborneBases,
                this::getAirborneBases,
                this::resetAirborneBases))
        .put("airborneTargettedByAA",
            AttachmentProperty.of(
                this::setAirborneTargettedByAA,
                this::setAirborneTargettedByAA,
                this::getAirborneTargettedByAA,
                this::resetAirborneTargettedByAA))
        .put("attackRollsBonus",
            AttachmentProperty.of(
                this::setAttackRollsBonus,
                this::setAttackRollsBonus,
                this::getAttackRollsBonus,
                this::resetAttackRollsBonus))
        .put("defenseRollsBonus",
            AttachmentProperty.of(
                this::setDefenseRollsBonus,
                this::setDefenseRollsBonus,
                this::getDefenseRollsBonus,
                this::resetDefenseRollsBonus))
        .put("bombingBonus",
            AttachmentProperty.of(
                this::setBombingBonus,
                this::setBombingBonus,
                this::getBombingBonus,
                this::resetBombingBonus))
        .build();
  }

  @Override
  public Map<String, AttachmentProperty<?>> getPropertyMap() {
    return createPropertyMap();
  }
}
