package games.strategy.engine.data.changefactory;

import static com.google.common.base.Preconditions.checkNotNull;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerId;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.triplea.delegate.TechAdvance;

class RemoveAvailableTech extends Change {
  private static final long serialVersionUID = 6131447662760022521L;

  private final TechAdvance tech;
  private final TechnologyFrontier frontier;
  private final PlayerId player;

  RemoveAvailableTech(final TechnologyFrontier front, final TechAdvance tech, final PlayerId player) {
    checkNotNull(front);
    checkNotNull(tech);

    this.tech = tech;
    frontier = front;
    this.player = player;
  }

  @Override
  public void perform(final GameData data) {
    final TechnologyFrontier front = player.getTechnologyFrontierList().getTechnologyFrontier(frontier.getName());
    front.removeAdvance(tech);
  }

  @Override
  public Change invert() {
    return new AddAvailableTech(frontier, tech, player);
  }
}
