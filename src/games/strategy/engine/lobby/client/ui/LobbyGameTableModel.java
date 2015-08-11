package games.strategy.engine.lobby.client.ui;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.lobby.server.ILobbyGameBroadcaster;
import games.strategy.engine.lobby.server.ILobbyGameController;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;

public class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host, Name, GV, Round, Players, P, B, EV, Started, Status, Comments, GUID
  }

  private final IMessenger m_messenger;
  private final IChannelMessenger m_channelMessenger;
  private final IRemoteMessenger m_remoteMessenger;
  // these must only be accessed in the swing event thread
  private final List<GUID> m_gameIDs = new ArrayList<GUID>();
  private final List<GameDescription> m_games = new ArrayList<GameDescription>();

  public LobbyGameTableModel(final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger) {
    m_messenger = messenger;
    m_channelMessenger = channelMessenger;
    m_remoteMessenger = remoteMessenger;
    m_channelMessenger.registerChannelSubscriber(new ILobbyGameBroadcaster() {
      @Override
      public void gameUpdated(final GUID gameId, final GameDescription description) {
        assertSentFromServer();
        updateGame(gameId, description);
      }

      @Override
      public void gameAdded(final GUID gameId, final GameDescription description) {
        assertSentFromServer();
        addGame(gameId, description);
      }

      @Override
      public void gameRemoved(final GUID gameId) {
        assertSentFromServer();
        removeGame(gameId);
      }
    }, ILobbyGameBroadcaster.GAME_BROADCASTER_CHANNEL);
    final Map<GUID, GameDescription> games =
        ((ILobbyGameController) m_remoteMessenger.getRemote(ILobbyGameController.GAME_CONTROLLER_REMOTE)).listGames();
    for (final GUID id : games.keySet()) {
      addGame(id, games.get(id));
    }
  }

  public GameDescription get(final int i) {
    return m_games.get(i);
  }

  @Override
  public Class<?> getColumnClass(final int columnIndex) {
    if (columnIndex == getColumnIndex(Column.Started)) {
      return Date.class;
    }
    return Object.class;
  }

  private void removeGame(final GUID gameId) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final int index = m_gameIDs.indexOf(gameId);
        m_gameIDs.remove(index);
        m_games.remove(index);
        fireTableRowsDeleted(index, index);
      }
    });
  }

  private void addGame(final GUID gameId, final GameDescription description) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        m_gameIDs.add(gameId);
        m_games.add(description);
        fireTableRowsInserted(m_gameIDs.size() - 1, m_gameIDs.size() - 1);
      }
    });
  }

  private void assertSentFromServer() {
    if (!MessageContext.getSender().equals(m_messenger.getServerNode())) {
      throw new IllegalStateException("Invalid sender");
    }
  }

  private void updateGame(final GUID gameId, final GameDescription description) {
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        final int index = m_gameIDs.indexOf(gameId);
        m_games.set(index, description);
        fireTableRowsUpdated(index, index);
      }
    });
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
  }

  public int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public int getColumnCount() {
    // -1 so we don't display the guid
    return Column.values().length - 1;
  }

  @Override
  public int getRowCount() {
    return m_gameIDs.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final GameDescription description = m_games.get(rowIndex);
    switch (column) {
      case Host:
        return description.getHostName();
      case Round:
        return description.getRound();
      case Name:
        return description.getGameName();
      case Players:
        return description.getPlayerCount();
      case P:
        return (description.getPassworded() ? "*" : "");
      case B:
        return (description.getBotSupportEmail() != null && description.getBotSupportEmail().length() > 0 ? "-" : "");
      case GV:
        return description.getGameVersion();
      case EV:
        return description.getEngineVersion();
      case Status:
        return description.getStatus();
      case Comments:
        return description.getComment();
      case Started:
        return description.getStartDateTime();
      case GUID:
        return m_gameIDs.get(rowIndex);
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }
}
