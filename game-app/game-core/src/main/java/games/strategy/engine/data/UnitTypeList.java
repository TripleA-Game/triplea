package games.strategy.engine.data;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.triplea.attachments.UnitSupportAttachment;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nullable;

/**
 * A collection of unit types.
 *
 * <p>The content of the UnitTypeList doesn't change during a game.
 */
public class UnitTypeList extends GameDataComponent implements Iterable<UnitType> {
  private static final long serialVersionUID = 9002927658524651749L;

  private final Map<String, UnitType> unitTypes = new LinkedHashMap<>();
  // Cached support rules. Marked transient as the cache does not need to be part of saved games.
  private transient @Nullable Set<UnitSupportAttachment> supportRules;
  private transient @Nullable Set<UnitSupportAttachment> supportAaRules;
  private transient @Nullable Set<UnitSupportAttachment> supportAirRules;

  public UnitTypeList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addUnitType(final UnitType type) {
    unitTypes.put(type.getName(), type);
  }

  public UnitType getUnitType(final String name) {
    return unitTypes.get(name);
  }

  /** Will return null if even a single name is not on the unit list. */
  public Set<UnitType> getUnitTypes(final String[] names) {
    final Set<UnitType> types = new HashSet<>();
    for (final String name : names) {
      final UnitType type = unitTypes.get(name);
      if (type == null) {
        return null;
      }
      types.add(type);
    }
    return types;
  }

  /**
   * Returns the unit support rules for the unit types. Computed once and cached afterwards.
   *
   * @return The unit support rules.
   */
  public Set<UnitSupportAttachment> getSupportRules() {
    if (supportRules == null) {
      supportRules =
          UnitSupportAttachment.get(this).stream()
              .filter(usa -> (usa.getRoll() || usa.getStrength()))
              .collect(Collectors.toSet());
    }
    return supportRules;
  }

  /**
   * Returns the unit support rules for the unit types. Computed once and cached afterwards.
   *
   * @return The unit support rules.
   */
  public Set<UnitSupportAttachment> getSupportAaRules() {
    if (supportAaRules == null) {
      supportAaRules =
          UnitSupportAttachment.get(this).stream()
              .filter(usa -> (usa.getAaRoll() || usa.getAaStrength()))
              .collect(Collectors.toSet());
    }
    return supportAaRules;
  }

  /**
   * Returns the unit support rules for the unit types. Computed once and cached afterwards.
   *
   * @return The unit support rules.
   */
  public Set<UnitSupportAttachment> getSupportAirRules() {
    if (supportAirRules == null) {
      supportAirRules =
          UnitSupportAttachment.get(this).stream()
               .filter(usa -> (usa.getAirRoll() || usa.getAirStrength()))
               .collect(Collectors.toSet());
    }
    return supportAirRules;
  }

  public int size() {
    return unitTypes.size();
  }

  @Override
  public Iterator<UnitType> iterator() {
    return unitTypes.values().iterator();
  }

  public Stream<UnitType> stream() {
    return unitTypes.values().stream();
  }

  public Set<UnitType> getAllUnitTypes() {
    return new LinkedHashSet<>(unitTypes.values());
  }
}
