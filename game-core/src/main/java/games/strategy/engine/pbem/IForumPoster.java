package games.strategy.engine.pbem;

import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Util;

/**
 * An interface for classes that can post a turn summary, the summary may also include a save game if the
 * implementing class supports this.
 */
public interface IForumPoster {

  String NAME = "FORUM_POSTER_NAME";
  String TOPIC_ID = "FORUM_POSTER_TOPIC_ID";
  String INCLUDE_SAVEGAME = "FORUM_POSTER_INCLUDE_SAVEGAME";
  String POST_AFTER_COMBAT = "FORUM_POSTER_POST_AFTER_COMBAT";


  /**
   * Called when the turn summary should be posted.
   *
   * @param summary the forum summary
   * @param subject the forum subject
   * @return true if the post was successful
   */
  CompletableFuture<String> postTurnSummary(String summary, final String subject, final Path savegame);

  String getDisplayName();

  /**
   * Opens a browser and go to the forum post, identified by the forumId.
   */
  void viewPosted();

  /**
   * Each poster provides a message that is displayed on the progress bar when testing the poster.
   *
   * @return the progress bar message
   */
  String getTestMessage();

  /**
   * Creates an {@link IForumPoster} instance based on the given arguments and the configured settings.
   */
  static IForumPoster getForumPosterByName(final String name, final int topicId) {
    switch (name) {
      case TripleAForumPoster.DISPLAY_NAME:
        return new TripleAForumPoster(
            topicId,
            Util.getFromSetting(ClientSetting.tripleaForumUsername),
            Util.getFromSetting(ClientSetting.tripleaForumPassword));
      case AxisAndAlliesForumPoster.DISPLAY_NAME:
        return new AxisAndAlliesForumPoster(
            topicId,
            Util.getFromSetting(ClientSetting.aaForumUsername),
            Util.getFromSetting(ClientSetting.aaForumPassword));
      default:
        throw new IllegalArgumentException(String.format("String '%s' must be a valid name", name));
    }
  }
}
