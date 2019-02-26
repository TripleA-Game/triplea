package games.strategy.engine.lobby.client.ui;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.triplea.lobby.common.ILobbyGameBroadcaster;
import org.triplea.lobby.common.ILobbyGameController;
import org.triplea.util.Tuple;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.engine.lobby.server.GameDescription;
import games.strategy.engine.message.IChannelMessenger;
import games.strategy.engine.message.IRemoteMessenger;
import games.strategy.engine.message.MessageContext;
import games.strategy.net.GUID;
import games.strategy.net.IMessenger;

class LobbyGameTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 6399458368730633993L;

  enum Column {
    Host, Name, GV, Round, Players, P, Status, Comments, Started, GUID
  }

  private final IMessenger messenger;
  private final boolean admin;

  // these must only be accessed in the swing event thread
  private final List<Tuple<GUID, GameDescription>> gameList = new ArrayList<>();
  private final ILobbyGameBroadcaster lobbyGameBroadcaster = new ILobbyGameBroadcaster() {
    @Override
    public void gameUpdated(final GUID gameId, final GameDescription description) {
      assertSentFromServer();
      updateGame(gameId, description);
    }

    @Override
    public void gameRemoved(final GUID gameId) {
      assertSentFromServer();
      removeGame(gameId);
    }
  };

  LobbyGameTableModel(final boolean admin, final IMessenger messenger, final IChannelMessenger channelMessenger,
      final IRemoteMessenger remoteMessenger) {
    this.messenger = messenger;
    this.admin = admin;
    channelMessenger.registerChannelSubscriber(lobbyGameBroadcaster, ILobbyGameBroadcaster.REMOTE_NAME);

    final Map<GUID, GameDescription> games =
        ((ILobbyGameController) remoteMessenger.getRemote(ILobbyGameController.REMOTE_NAME)).listGames();
    for (final Map.Entry<GUID, GameDescription> entry : games.entrySet()) {
      updateGame(entry.getKey(), entry.getValue());
    }
  }

  private void removeGame(final GUID gameId) {
    SwingUtilities.invokeLater(() -> {
      if (gameId == null) {
        return;
      }

      final Tuple<GUID, GameDescription> gameToRemove = findGame(gameId);
      if (gameToRemove != null) {
        final int index = gameList.indexOf(gameToRemove);
        gameList.remove(gameToRemove);
        fireTableRowsDeleted(index, index);
      }
    });
  }

  private Tuple<GUID, GameDescription> findGame(final GUID gameId) {
    return gameList.stream()
        .filter(game -> game.getFirst().equals(gameId))
        .findFirst()
        .orElse(null);
  }


  protected ILobbyGameBroadcaster getLobbyGameBroadcaster() {
    return lobbyGameBroadcaster;
  }

  GameDescription get(final int i) {
    return gameList.get(i).getSecond();
  }

  private void assertSentFromServer() {
    if (!MessageContext.getSender().equals(messenger.getServerNode())) {
      throw new IllegalStateException("Invalid sender");
    }
  }

  private void updateGame(final GUID gameId, final GameDescription description) {
    if (gameId == null) {
      return;
    }
    SwingUtilities.invokeLater(() -> {
      final Tuple<GUID, GameDescription> toReplace = findGame(gameId);
      if (toReplace == null) {
        gameList.add(Tuple.of(gameId, description));
        fireTableRowsInserted(getRowCount() - 1, getRowCount() - 1);
      } else {
        final int replaceIndex = gameList.indexOf(toReplace);
        gameList.set(replaceIndex, Tuple.of(gameId, description));
        fireTableRowsUpdated(replaceIndex, replaceIndex);
      }
    });
  }

  @Override
  public String getColumnName(final int column) {
    return Column.values()[column].toString();
  }

  int getColumnIndex(final Column column) {
    return column.ordinal();
  }

  @Override
  public int getColumnCount() {
    final int adminHiddenColumns = admin ? 0 : -1;
    // -1 so we don't display the guid
    // -1 again if we are not admin to hide the 'started' column
    return Column.values().length - 1 + adminHiddenColumns;
  }

  @Override
  public int getRowCount() {
    return gameList.size();
  }

  @Override
  public Object getValueAt(final int rowIndex, final int columnIndex) {
    final Column column = Column.values()[columnIndex];
    final GameDescription description = gameList.get(rowIndex).getSecond();
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
      case GV:
        return description.getGameVersion();
      case Status:
        return description.getStatus();
      case Comments:
        return description.getComment();
      case Started:
        return formatBotStartTime(description.getStartDateTime());
      case GUID:
        return gameList.get(rowIndex).getFirst();
      default:
        throw new IllegalStateException("Unknown column:" + column);
    }
  }

  @VisibleForTesting
  static String formatBotStartTime(final Instant instant) {
    return new DateTimeFormatterBuilder().appendLocalized(null, FormatStyle.SHORT).toFormatter()
        .format(LocalDateTime.ofInstant(instant, ZoneOffset.systemDefault()));
  }
}
