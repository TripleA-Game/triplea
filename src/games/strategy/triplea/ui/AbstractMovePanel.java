package games.strategy.triplea.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;

import com.google.common.collect.Sets;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Unit;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.triplea.delegate.UndoableMove;
import games.strategy.triplea.delegate.dataObjects.MoveDescription;
import games.strategy.triplea.delegate.remote.IAbstractMoveDelegate;
import games.strategy.triplea.util.JFXUtils;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;

public abstract class AbstractMovePanel extends ActionPanel {
  private static final long serialVersionUID = -4153574987414031433L;
  private static final String s_MOVE_PANEL_CANCEL = "movePanel.cancel";
  private static final Logger s_logger = Logger.getLogger(MovePanel.class.getName());
  private static final int s_entryPadding = 15;
  private final TripleAFrame m_frame;
  private boolean m_listening = false;
  private final Label m_actionLabel = new Label();
  protected MoveDescription m_moveMessage;
  protected List<UndoableMove> m_undoableMoves;
  protected EventHandler<ActionEvent> m_doneMove = e -> {
      if (doneMoveAction()) {
        m_moveMessage = null;
        release();
      }
  };
  private final EventHandler<ActionEvent> m_DONE_MOVE_ACTION = new WeakAction("Done", m_doneMove);
  private final EventHandler<ActionEvent> m_cancelMove = e -> {
      cancelMoveAction();
      if (m_frame != null) {
        m_frame.clearStatusMessage();
      }
  };

  public AbstractMovePanel(final GameData data, final MapPanel map, final TripleAFrame frame) {
    super(data, map);
    m_frame = frame;
    m_undoableMoves = Collections.emptyList();
  }

  /*
   * sub-classes method for done handling
   */
  abstract protected boolean doneMoveAction();

  /*
   * sub-classes method for cancel handling
   */
  abstract protected void cancelMoveAction();

  private final EventHandler<ActionEvent> m_CANCEL_MOVE_ACTION = new WeakAction("Cancel", m_cancelMove);
  protected AbstractUndoableMovesPanel m_undoableMovesPanel;
  private IPlayerBridge m_bridge;

  protected IPlayerBridge getPlayerBridge() {
    return m_bridge;
  }

  // m_frame methods
  protected final void clearStatusMessage() {
    m_frame.clearStatusMessage();
  }

  protected final void setStatusErrorMessage(final String message) {
    m_frame.setStatusErrorMessage(message);
  }

  protected final void setStatusWarningMessage(final String message) {
    m_frame.setStatusWarningMessage(message);
  }

  protected final boolean getListening() {
    return m_listening;
  }

  protected final void setMoveMessage(final MoveDescription message) {
    m_moveMessage = message;
  }

  protected final List<UndoableMove> getUndoableMoves() {
    return m_undoableMoves;
  }

  protected final void enableCancelButton() {
    //TODO
  }

  /**
   * @return m_bridge.getGameData()
   */
  protected final GameData getGameData() {
    return m_bridge.getGameData();
  }

  private IAbstractMoveDelegate getMoveDelegate() {
    return (IAbstractMoveDelegate) m_bridge.getRemoteDelegate();
  }

  @SuppressWarnings("unchecked")
  protected final void updateMoves() {
    m_undoableMoves = (List<UndoableMove>) getMoveDelegate().getMovesMade();
    m_undoableMovesPanel.setMoves(new ArrayList<>(m_undoableMoves));
  }

  public final void cancelMove() {
    m_CANCEL_MOVE_ACTION.handle(null);
  }

  public final String undoMove(final int moveIndex) {
    return undoMove(moveIndex, false);
  }

  /**
   * Executes an undo move for any of the units passed in as a parameter.
   *
   * "Cannot undo" Error messages are suppressed if any moves cannot be undone
   * (at least until we come up with a way to deal with "n" reasons for an undo
   * failure rather than just one)
   */
  public void undoMoves(final Set<Unit> units) {
    @SuppressWarnings("unchecked")
    final Set<UndoableMove> movesToUndo = getMovesToUndo(units, (List<Object>) getMoveDelegate().getMovesMade());

    if (movesToUndo.size() == 0) {
      final String error =
          "Could not undo any moves, check that the unit has moved and that you can undo the move normally";
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
      return;
    }

    undoMovesInReverseOrder(movesToUndo);
  }

  private static Set<UndoableMove> getMovesToUndo(final Set<Unit> units, final List<Object> movesMade) {
    final Set<UndoableMove> movesToUndo = Sets.newHashSet();

    if (movesMade != null) {
      for (final Object undoableMoveObject : movesMade) {
        if (undoableMoveObject != null) {
          final UndoableMove move = (UndoableMove) undoableMoveObject;
          if (move.containsAnyOf(units) && move.getcanUndo()) {
            movesToUndo.add(move);
          }
        }
      }
    }
    return movesToUndo;
  }

  /*
   * Undo moves in reverse order, from largest index to smallest. Undo will reorder move index numbers, so going top
   * down avoids this renumbering.
   */
  private void undoMovesInReverseOrder(final Set<UndoableMove> movesToUndo) {
    final List<Integer> moveIndexes = getSortedMoveIndexes(movesToUndo);
    for (int i = moveIndexes.size() - 1; i >= 0; i--) {
      undoMove(moveIndexes.get(i));
    }
  }

  private static List<Integer> getSortedMoveIndexes(final Set<UndoableMove> moves) {
    final List<Integer> moveIndexes = new ArrayList<>();
    for (final UndoableMove move : moves) {
      moveIndexes.add(move.getIndex());
    }
    Collections.sort(moveIndexes);
    return moveIndexes;
  }


  protected final String undoMove(final int moveIndex, final boolean suppressError) {
    // clean up any state we may have
    m_CANCEL_MOVE_ACTION.handle(null);
    // undo the move
    final String error = getMoveDelegate().undoMove(moveIndex);
    if (error != null && !suppressError) {
      JOptionPane.showMessageDialog(getTopLevelAncestor(), error, "Could not undo move", JOptionPane.ERROR_MESSAGE);
    } else {
      updateMoves();
    }
    undoMoveSpecific();
    return error;
  }

  /**
   * sub-classes method for undo handling
   */
  abstract protected void undoMoveSpecific();

  protected final void cleanUp() {
    SwingUtilities.invokeLater(() -> {
      s_logger.fine("cleanup");
      if (!m_listening) {
        throw new IllegalStateException("Not listening");
      }
      m_listening = false;
      cleanUpSpecific();
      m_bridge = null;
      getChildren().clear();
    });
  }

  /*
   * sub-classes method for clean-up
   */
  abstract protected void cleanUpSpecific();

  @Override
  public final void setActive(final boolean active) {
    super.setActive(active);
    SwingUtilities.invokeLater(() -> m_CANCEL_MOVE_ACTION.handle(null));
  }

  protected final void display(final PlayerID id, final String actionLabel) {
    super.display(id);
    Platform.runLater(() -> {
      getChildren().clear();
      m_actionLabel.setText(id.getName() + actionLabel);
      getChildren().add(leftBox(m_actionLabel));
      if (setCancelButton()) {
        getChildren().add(leftBox(JFXUtils.getButtonWithAction(m_CANCEL_MOVE_ACTION)));
      }
      getChildren().add(leftBox(JFXUtils.getButtonWithAction(m_DONE_MOVE_ACTION)));
      addAdditionalButtons();
      getChildren().add(m_undoableMovesPanel);
    });
  }

  protected void addAdditionalButtons() {}

  abstract protected boolean setCancelButton();

  protected static HBox leftBox(final Node c) {
    final HBox b = new HBox();
    b.getChildren().add(c);
    return b;
  }

  protected final void setUp(final IPlayerBridge bridge) {
    SwingUtilities.invokeLater(() -> {
      s_logger.fine("setup");
      setUpSpecific();
      m_bridge = bridge;
      updateMoves();
      if (m_listening) {
        throw new IllegalStateException("Not listening");
      }
      m_listening = true;
      if (getRootPane() != null) {
        final String key = s_MOVE_PANEL_CANCEL;
        getRootPane().getActionMap().put(key, m_CANCEL_MOVE_ACTION);
        getRootPane().getInputMap(WHEN_ANCESTOR_OF_FOCUSED_COMPONENT)
            .put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), key);
      }
    });
  }

  /*
   * sub-classes method for set-up
   */
  abstract protected void setUpSpecific();

  protected void clearDependencies() {
    // used by some subclasses
  }

  public final MoveDescription waitForMove(final IPlayerBridge bridge) {
    setUp(bridge);
    waitForRelease();
    cleanUp();
    final MoveDescription rVal = m_moveMessage;
    m_moveMessage = null;
    clearDependencies();
    return rVal;
  }
}
