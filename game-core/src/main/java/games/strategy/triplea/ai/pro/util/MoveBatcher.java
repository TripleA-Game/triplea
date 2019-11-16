package games.strategy.triplea.ai.pro.util;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Unit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * MoveBatcher implements an algorithm that batches AI moves together, reducing the number of
 * distinct moves the AI does (and thus the visual delay) by combining moves together.
 */
public class MoveBatcher {
  private static class Move {
    private final ArrayList<Unit> units;
    private final Route route;
    private final @Nullable HashMap<Unit, Unit> unitsToTransports;

    private Move(
        final ArrayList<Unit> units,
        final Route route,
        final @Nullable HashMap<Unit, Unit> unitsToTransports) {
      this.units = units;
      this.route = route;
      this.unitsToTransports = unitsToTransports;
    }

    Move(final ArrayList<Unit> units, final Route route) {
      this(units, route, null);
    }

    Move(final Unit unit, final Route route, final Unit transportToLoad) {
      this(new ArrayList<>(List.of(unit)), route, new HashMap<>(Map.of(unit, transportToLoad)));
    }

    private boolean isTransportLoad() {
      return this.unitsToTransports != null;
    }

    boolean canMergeWith(final Move other) {
      return other.isTransportLoad() == isTransportLoad() && route.equals(other.route);
    }

    boolean mergeWith(final Move other) {
      if (canMergeWith(other)) {
        // Merge units and transports.
        units.addAll(other.units);
        if (isTransportLoad()) {
          unitsToTransports.putAll(other.unitsToTransports);
        }
        return true;
      }
      return false;
    }

    void addTo(
        final List<Collection<Unit>> moveUnits,
        final List<Route> moveRoutes,
        final List<Map<Unit, Unit>> unitsToTransports) {
      moveUnits.add(units);
      moveRoutes.add(route);
      unitsToTransports.add(this.unitsToTransports);
    }
  }

  private final ArrayList<ArrayList<Move>> moveSequences = new ArrayList<>();

  /**
   * Starts a new sequence. This must be called before calling any add*() methods. A sequence
   * indicates a logical dependency relationship between the moves in that sequence.
   */
  public void newSequence() {
    moveSequences.add(new ArrayList<>());
  }

  /**
   * Adds a transport load move.
   *
   * @param unit The unit to move.
   * @param route The route to move on.
   * @param transport The transport to load.
   */
  public void addTransportLoad(final Unit unit, final Route route, final Unit transport) {
    addMove(new Move(unit, route, transport));
  }

  /**
   * Adds a move for the specified units. The units list must not be changed after adding the move.
   *
   * @param units The units to move. Note that type is ArrayList explicitly to prevent an
   *     unmodifiable List.of() being passed.
   * @param route The route to move on.
   */
  public void addMove(final ArrayList<Unit> units, final Route route) {
    addMove(new Move(units, route));
  }

  private void addMove(final Move newMove) {
    final ArrayList<Move> sequence = moveSequences.get(moveSequences.size() - 1);
    if (!sequence.isEmpty() && sequence.get(sequence.size() - 1).mergeWith(newMove)) {
      return;
    }
    sequence.add(newMove);
  }

  /**
   * Emits the accumulated moves to the passed output params.
   *
   * <p>TODO: #5416 Refactor the IMoveDelegate and ProMoveUtils code to use a List of Move objects,
   * instead of these parallel lists.
   *
   * @param moveUnits The units to move.
   * @param moveRoutes The routes to move along
   * @param unitsToTransports The map from units to transports to be loaded.
   */
  public void batchAndEmit(
      final List<Collection<Unit>> moveUnits,
      final List<Route> moveRoutes,
      final List<Map<Unit, Unit>> unitsToTransports) {
    int i = 0;
    for (final var sequence : moveSequences) {
      if (!sequence.isEmpty()) {
        mergeSequences(sequence, moveSequences.subList(i + 1, moveSequences.size()));
        for (final Move move : sequence) {
          move.addTo(moveUnits, moveRoutes, unitsToTransports);
        }
      }
      i++;
    }
  }

  /**
   * Attempts to merge a sequence of moves with other sequences. Any sequence merged into this
   * sequence will have its moves cleared.
   *
   * @param sequence The sequence to merge other sequences into.
   * @param sequences The sequences to try to merge into the sequence.
   */
  private static void mergeSequences(
      final ArrayList<Move> sequence, final List<ArrayList<Move>> sequences) {
    for (final var otherSequence : sequences) {
      boolean merge = (otherSequence.size() == sequence.size());
      for (int i = 0; merge && i < sequence.size(); i++) {
        merge = sequence.get(i).canMergeWith(otherSequence.get(i));
      }
      if (!merge) {
        continue;
      }
      for (int i = 0; i < sequence.size(); i++) {
        if (!sequence.get(i).mergeWith(otherSequence.get(i))) {
          throw new IllegalStateException(
              "Could not merge move despite checking canMergeWith() earlier.");
        }
      }
      otherSequence.clear();
    }
  }
}
