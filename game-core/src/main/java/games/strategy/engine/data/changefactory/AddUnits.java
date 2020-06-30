package games.strategy.engine.data.changefactory;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitCollection;
import games.strategy.engine.data.UnitHolder;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/** Add units. */
public class AddUnits extends Change {
  private static final long serialVersionUID = 2694342784633196289L;

  private final String name;
  private final Collection<Unit> units;
  private final String type;
  private Map<UUID, String> unitPlayerMap;

  AddUnits(final UnitCollection collection, final Collection<Unit> units) {
    this.units = units;
    unitPlayerMap =
        units.stream().collect(Collectors.toMap(Unit::getId, unit -> unit.getOwner().getName()));
    name = collection.getHolder().getName();
    type = collection.getHolder().getType();
  }

  AddUnits(final String name, final String type, final Collection<Unit> units) {
    this.units = units;
    unitPlayerMap =
        units.stream().collect(Collectors.toMap(Unit::getId, unit -> unit.getOwner().getName()));
    this.type = type;
    this.name = name;
  }

  @Override
  public Change invert() {
    return new RemoveUnits(name, type, units);
  }

  @Override
  protected void perform(final GameData data) {
    final UnitHolder holder = data.getUnitHolder(name, type);
    final Collection<Unit> unitsWithCorrectOwner = getUnitsWithOwner(unitPlayerMap, data);
    holder.getUnitCollection().addAll(unitsWithCorrectOwner);
  }

  private Collection<Unit> getUnitsWithOwner(
      final Map<UUID, String> unitPlayerMap, final GameData data) {
    return unitPlayerMap.entrySet().stream()
        .map(
            entry -> {
              final Unit unit = data.getUnits().get(entry.getKey());
              final GamePlayer player = data.getPlayerList().getPlayerId(entry.getValue());
              unit.setOwner(player);
              return unit;
            })
        .collect(Collectors.toList());
  }

  @Override
  public String toString() {
    return "Add unit change.  Add to:" + name + " units:" + units;
  }

  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
    if (unitPlayerMap == null) {
      unitPlayerMap =
          units.stream().collect(Collectors.toMap(Unit::getId, unit -> unit.getOwner().getName()));
    }
  }
}
