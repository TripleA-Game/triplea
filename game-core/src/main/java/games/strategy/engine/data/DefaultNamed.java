package games.strategy.engine.data;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Objects;

import com.google.common.base.MoreObjects;

/**
 * Default implementation of {@link Named} for game data components.
 */
public class DefaultNamed extends GameDataComponent implements Named {
  private static final long serialVersionUID = -5737716450699952621L;

  private final String name;

  public DefaultNamed(final String name, final GameData data) {
    super(data);
    if (name == null || name.length() == 0) {
      throw new IllegalArgumentException("Name must not be null");
    }
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object other) {
    if (other instanceof Named) {
      return Objects.equals(name, ((Named) other).getName());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(name);
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(getClass()).add("name", name).toString();
  }

  // Workaround for JDK-8199664
  @SuppressWarnings("static-method")
  private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException {
    in.defaultReadObject();
  }
}
