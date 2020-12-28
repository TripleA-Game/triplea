package games.strategy.engine.framework.ui;

import games.strategy.engine.framework.map.file.system.loader.AvailableGamesList;
import java.util.Optional;
import java.util.stream.IntStream;
import javax.swing.DefaultListModel;
import lombok.extern.slf4j.Slf4j;

/** The model for a {@link GameChooser} dialog. */
@Slf4j
public final class GameChooserModel extends DefaultListModel<DefaultGameChooserEntry> {
  private static final long serialVersionUID = -2044689419834812524L;

  public GameChooserModel(final AvailableGamesList availableGamesList) {
    availableGamesList.getSortedGameEntries().forEach(this::addElement);
  }

  /** Searches for a GameChooserEntry whose gameName matches the input parameter. */
  public Optional<DefaultGameChooserEntry> findByName(final String name) {
    return IntStream.range(0, size())
        .mapToObj(this::get)
        .filter(entry -> entry.getGameName().equals(name))
        .findAny();
  }
}
