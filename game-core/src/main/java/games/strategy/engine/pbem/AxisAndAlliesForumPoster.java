package games.strategy.engine.pbem;

import games.strategy.triplea.UrlConstants;

/**
 * Posts turn summaries to www.axisandallies.org/forums.
 *
 * <p>
 * URL format is {@code https://www.axisandallies.org/forums/api/v2/topics/<topicID>}.
 * </p>
 */
public class AxisAndAlliesForumPoster extends NodeBbForumPoster {

  public AxisAndAlliesForumPoster(final int topicId, final String username, final String password) {
    super(topicId, username, password);
  }

  @Override
  String getForumUrl() {
    return UrlConstants.AXIS_AND_ALLIES_FORUM.toString();
  }

  @Override
  public String getDisplayName() {
    return "www.axisandallies.org/forums/";
  }
}
