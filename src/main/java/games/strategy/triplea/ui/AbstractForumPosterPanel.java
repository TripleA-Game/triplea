package games.strategy.triplea.ui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.gamePlayer.IPlayerBridge;
import games.strategy.engine.history.HistoryNode;
import games.strategy.engine.history.Round;
import games.strategy.engine.pbem.ForumPosterComponent;
import games.strategy.engine.pbem.PBEMMessagePoster;
import games.strategy.triplea.delegate.GameStepPropertiesHelper;
import games.strategy.triplea.delegate.remote.IAbstractForumPosterDelegate;
import games.strategy.ui.SwingAction;

public abstract class AbstractForumPosterPanel extends ActionPanel {
  private static final long serialVersionUID = -5084680807785728744L;
  protected JLabel m_actionLabel;
  protected IPlayerBridge m_bridge;
  protected PBEMMessagePoster m_poster;
  protected TripleAFrame m_frame;
  protected ForumPosterComponent m_forumPosterComponent;

  public AbstractForumPosterPanel(final GameData data, final MapPanel map) {
    super(data, map);
    m_actionLabel = new JLabel();
  }

  private int getRound() {
    int round = 0;
    final Object pathFromRoot[] = getData().getHistory().getLastNode().getPath();
    final Object arr$[] = pathFromRoot;
    final int len$ = arr$.length;
    int i$ = 0;
    do {
      if (i$ >= len$) {
        break;
      }
      final Object pathNode = arr$[i$];
      final HistoryNode curNode = (HistoryNode) pathNode;
      if (curNode instanceof Round) {
        round = ((Round) curNode).getRoundNo();
        break;
      }
      i$++;
    } while (true);
    return round;
  }

  @Override
  public void display(final PlayerID id) {
    super.display(id);
    SwingUtilities.invokeLater(() -> {
      m_actionLabel.setText(id.getName() + " " + getTitle());
      // defer componenet layout until waitForEndTurn()
    });
  }

  abstract protected boolean allowIncludeTerritorySummary();

  abstract protected boolean allowIncludeTerritoryAllPlayersSummary();

  abstract protected boolean allowIncludeProductionSummary();

  abstract protected boolean allowDiceBattleDetails();

  abstract protected boolean allowDiceStatistics();

  abstract protected IAbstractForumPosterDelegate getForumPosterDelegate();

  abstract protected boolean postTurnSummary(final PBEMMessagePoster poster, final boolean includeSaveGame);

  abstract protected boolean getHasPostedTurnSummary();

  abstract protected void setHasPostedTurnSummary(boolean posted);

  abstract protected boolean skipPosting();

  abstract protected String getTitle();

  @Override
  abstract public String toString();

  protected void waitForDone(final TripleAFrame frame, final IPlayerBridge bridge) {
    m_frame = frame;
    m_bridge = bridge;
    // Nothing to do if there are no PBEM messengers
    m_poster = new PBEMMessagePoster(getData(), getCurrentPlayer(), getRound(), getTitle());
    if (!m_poster.hasMessengers()) {
      return;
    }
    if (skipPosting() || GameStepPropertiesHelper.isSkipPosting(getData())) {
      return;
    }
    final boolean hasPosted = getHasPostedTurnSummary();
    SwingUtilities.invokeLater(() -> {
      removeAll();
      add(m_actionLabel);
      add(m_forumPosterComponent.layoutComponents(m_poster, getForumPosterDelegate(), m_bridge, m_frame, hasPosted,
          allowIncludeTerritorySummary(), allowIncludeTerritoryAllPlayersSummary(), allowIncludeProductionSummary(),
          allowDiceBattleDetails(), allowDiceStatistics()));
      validate();
    });
    waitForRelease();
  }
}
