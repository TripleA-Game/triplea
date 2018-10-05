package games.strategy.engine.history;

import games.strategy.engine.data.PlayerID;

class StepHistorySerializer implements SerializationWriter {
  private static final long serialVersionUID = 3546486775516371557L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final String m_stepName;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final String m_delegateName;
  @SuppressWarnings({
      "checkstyle:AbbreviationAsWordInName", // rename upon next incompatible release
      "checkstyle:MemberName" // rename upon next incompatible release
  })
  private final PlayerID m_playerID;
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final String m_displayName;

  public StepHistorySerializer(final String stepName, final String delegateName, final PlayerID playerId,
      final String displayName) {
    m_stepName = stepName;
    m_delegateName = delegateName;
    m_playerID = playerId;
    m_displayName = displayName;
  }

  @Override
  public void write(final HistoryWriter writer) {
    writer.startNextStep(m_stepName, m_delegateName, m_playerID, m_displayName);
  }
}
