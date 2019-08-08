package games.strategy.engine.chat;

import games.strategy.net.INode;
import java.util.List;
import javax.swing.Action;

/** Factory for creating actions in response to a player (node) event. */
@FunctionalInterface
public interface IPlayerActionFactory {

  /** The mouse has been clicked on a player, create a list of actions to be displayed. */
  List<Action> mouseOnPlayer(INode clickedOn);
}
