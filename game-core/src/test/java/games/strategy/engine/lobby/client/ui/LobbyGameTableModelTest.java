package games.strategy.engine.lobby.client.ui;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.nullValue;

import games.strategy.net.GUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.triplea.lobby.common.GameDescription;
import org.triplea.util.Tuple;

// TODO: rename to GameListModelTest
final class LobbyGameTableModelTest {
  @ExtendWith(MockitoExtension.class)
  @Nested
  final class RemoveAndUpdateGameTest {
    private GameListModel gameListModel = new GameListModel();

    private Tuple<GUID, GameDescription> fakeGame =
        Tuple.of(new GUID(), GameDescription.builder().build());

    private final GameDescription newDescription =
        GameDescription.builder().comment("comment").build();

    @BeforeEach
    void setUp() {
      gameListModel.add(fakeGame.getFirst(), fakeGame.getSecond());
      assertThat("games are loaded on init", gameListModel.size(), is(1));
    }

    @Test
    void updateGame() {
      assertThat(gameListModel.getGameDescriptionByRow(0).getComment(), nullValue());

      gameListModel.update(fakeGame.getFirst(), newDescription);

      assertThat(gameListModel.size(), is(1));
      assertThat(
          gameListModel.getGameDescriptionByRow(0).getComment(), is(newDescription.getComment()));
    }

    @Test
    void containsGame() {
      assertThat(gameListModel.containsGame(new GUID()), is(false));
      assertThat(gameListModel.containsGame(fakeGame.getFirst()), is(true));
    }

    @Test
    void addGame() {
      gameListModel.add(new GUID(), GameDescription.builder().build());
      assertThat(gameListModel.size(), is(2));
    }

    @Test
    void updateGameWithNullGuidIsIgnored() {
      gameListModel.update(null, GameDescription.builder().build());
      assertThat(
          "expect row count to remain 1, null guid is bogus data", gameListModel.size(), is(1));
    }

    @Test
    void removeGame() {
      gameListModel.removeGame(fakeGame.getFirst());
      assertThat(gameListModel.size(), is(0));
    }

    @Test
    void removeGameThatDoesNotExistIsIgnored() {
      gameListModel.removeGame(new GUID());
      assertThat(gameListModel.size(), is(1));
    }
  }
}
