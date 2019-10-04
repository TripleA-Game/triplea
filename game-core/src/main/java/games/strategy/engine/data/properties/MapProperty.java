package games.strategy.engine.data.properties;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.not;

import java.awt.Color;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.swing.JComponent;
import lombok.extern.java.Log;

/**
 * Basically creates a map of other properties.
 *
 * @param <V> parameters can be: Boolean, String, Integer, Double, Color, File, Collection, Map
 */
@Log
public class MapProperty<V> extends AbstractEditableProperty<Map<String, V>> {
  private static final long serialVersionUID = -8021039503574228146L;

  private Map<String, V> map;
  private final List<IEditableProperty<?>> properties = new ArrayList<>();

  public MapProperty(final String name, final String description, final Map<String, V> map) {
    super(name, description);

    checkNotNull(map);

    this.map = map;
    resetProperties(map, properties, name, description);
  }

  private static void resetProperties(
      final Map<String, ?> map,
      final List<IEditableProperty<?>> properties,
      final String name,
      final String description) {
    properties.clear();
    map.forEach(
        (key, value) -> {
          if (value instanceof Boolean) {
            properties.add(new BooleanProperty(key, description, ((Boolean) value)));
          } else if (value instanceof Color) {
            properties.add(new ColorProperty(key, description, ((Color) value)));
          } else if (value instanceof File) {
            properties.add(new FileProperty(key, description, ((File) value)));
          } else if (value instanceof String) {
            properties.add(new StringProperty(key, description, ((String) value)));
          } else if (value instanceof Collection) {
            properties.add(new CollectionProperty<>(name, description, ((Collection<?>) value)));
          } else if (value instanceof Integer) {
            properties.add(
                new NumberProperty(
                    key, description, Integer.MAX_VALUE, Integer.MIN_VALUE, ((Integer) value)));
          } else if (value instanceof Double) {
            properties.add(
                new DoubleProperty(
                    key, description, Double.MAX_VALUE, Double.MIN_VALUE, ((Double) value), 5));
          } else {
            final String valueTypeName =
                (value != null) ? value.getClass().getCanonicalName() : "<null>";
            throw new IllegalArgumentException(
                "cannot instantiate MapProperty with value type: " + valueTypeName);
          }
        });
  }

  @Override
  public int getRowsNeeded() {
    return Math.max(1, properties.size());
  }

  @Override
  public Map<String, V> getValue() {
    return map;
  }

  @Override
  public void setValue(final Map<String, V> value) {
    checkNotNull(value);

    map = value;
    resetProperties(map, properties, getName(), getDescription());
  }

  @Override
  public JComponent getEditorComponent() {
    return new PropertiesUi(properties, true);
  }

  @Override
  public JComponent getViewComponent() {
    return new PropertiesUi(properties, false);
  }

  @Override
  public boolean validate(final Object value) {
    if (!(value instanceof Map)) {
      return false;
    }

    final Map<?, ?> otherMap = (Map<?, ?>) value;
    if (!areAllElementsNullOrInstanceOf(otherMap.keySet(), String.class)
        || !areAllElementsNullOrInstanceOf(otherMap.values(), getMapValueType())) {
      return false;
    }

    // verify setting new values will not trigger an error
    try {
      @SuppressWarnings("unchecked")
      final Map<String, ?> typedOtherMap = (Map<String, ?>) otherMap;
      resetProperties(typedOtherMap, new ArrayList<>(), getName(), getDescription());
    } catch (final IllegalArgumentException e) {
      log.warning("Validation failed: " + e.getMessage());
      return false;
    }

    return true;
  }

  private static boolean areAllElementsNullOrInstanceOf(
      final Collection<?> collection, final Class<?> type) {
    return collection.stream().filter(not(Objects::isNull)).allMatch(type::isInstance);
  }

  private Class<?> getMapValueType() {
    return map.values().stream().<Class<?>>map(Object::getClass).findAny().orElse(Object.class);
  }
}
