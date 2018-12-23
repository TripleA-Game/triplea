package games.strategy.engine.pbem;

import games.strategy.triplea.UrlConstants;

/**
 * Posts turn summaries to forums.triplea-game.org.
 *
 * <p>
 * URL format is {@code https://forums.triplea-game.org/api/v2/topics/<topicID>}.
 * </p>
 */
public class TripleAForumPoster extends NodeBbForumPoster {

  public TripleAForumPoster(final int topicId, final String username, final String password) {
    super(topicId, username, password);
  }

  @Override
  String getForumUrl() {
    return UrlConstants.TRIPLEA_FORUM.toString();
  }

  @Override
  public String getDisplayName() {
    return "forums.triplea-game.org";
  }
}
