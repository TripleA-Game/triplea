package games.strategy.engine.data;

import java.util.ArrayList;
import java.util.List;

/** A measurement of value used by players to purchase units. */
public class Resource extends NamedAttachable {
  private static final long serialVersionUID = 7471431759007499935L;

  private final List<PlayerId> players;

  public Resource(final String resourceName, final GameData data) {
    this(resourceName, data, List.of());
  }

  public Resource(final String resourceName, final GameData data, final List<PlayerId> players) {
    super(resourceName, data);
    this.players = new ArrayList<>(players);
  }

  public boolean isDisplayedFor(final PlayerId player) {
    return players.contains(player);
  }
}
