package games.strategy.engine.data;

import java.util.Properties;

import games.strategy.engine.display.IDisplay;
import games.strategy.engine.gamePlayer.IRemotePlayer;
import games.strategy.engine.history.DelegateHistoryWriter;
import games.strategy.engine.history.History;
import games.strategy.engine.history.HistoryWriter;
import games.strategy.engine.history.IDelegateHistoryWriter;
import games.strategy.engine.message.ChannelMessenger;
import games.strategy.engine.message.DummyMessenger;
import games.strategy.engine.message.UnifiedMessenger;
import games.strategy.engine.random.IRandomSource;
import games.strategy.engine.random.IRandomStats.DiceType;
import games.strategy.sound.DummySoundChannel;
import games.strategy.sound.ISound;
import games.strategy.triplea.ui.display.ITripleaDisplay;

/**
 *
 *
 *          Not for actual use, suitable for testing. Never returns messages, but can get
 *          random and implements changes immediately.
 */
public class TestDelegateBridge implements ITestDelegateBridge {
  private final GameData m_data;
  private PlayerID m_id;
  private String m_stepName = "no name specified";
  private IDisplay m_dummyDisplay;
  private final DummySoundChannel m_soundChannel = new DummySoundChannel();
  private IRandomSource m_randomSource;
  private final IDelegateHistoryWriter m_historyWriter;
  private IRemotePlayer m_remote;

  /** Creates new TestDelegateBridge */
  public TestDelegateBridge(final GameData data, final PlayerID id, final IDisplay dummyDisplay) {
    m_data = data;
    m_id = id;
    m_dummyDisplay = dummyDisplay;
    final History history = new History(m_data);
    final HistoryWriter historyWriter = new HistoryWriter(history);
    historyWriter.startNextStep("", "", PlayerID.NULL_PLAYERID, "");
    final ChannelMessenger channelMessenger = new ChannelMessenger(new UnifiedMessenger(new DummyMessenger()));
    m_historyWriter = new DelegateHistoryWriter(channelMessenger);
  }

  @Override
  public void setDisplay(final ITripleaDisplay display) {
    m_dummyDisplay = display;
  }

  /**
   * Delegates should not use random data that comes from any other source.
   */
  @Override
  public int getRandom(final int max, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_randomSource.getRandom(max, annotation);
  }

  @Override
  public int[] getRandom(final int max, final int count, final PlayerID player, final DiceType diceType, final String annotation) {
    return m_randomSource.getRandom(max, count, annotation);
  }

  /**
   * Changing the player has the effect of commiting the current transaction.
   * Player is initialized to the player specified in the xml data.
   */
  @Override
  public void setPlayerID(final PlayerID aPlayer) {
    m_id = aPlayer;
  }

  public boolean inTransaction() {
    return false;
  }

  @Override
  public PlayerID getPlayerID() {
    return m_id;
  }

  @Override
  public void addChange(final Change aChange) {
    final ChangePerformer changePerformer = new ChangePerformer(m_data);
    changePerformer.perform(aChange);
  }

  @Override
  public void setStepName(final String name) {
    setStepName(name, false);
  }

  @Override
  public void setStepName(final String name, final boolean doNotChangeSequence) {
    m_stepName = name;
    if (!doNotChangeSequence) {
      m_data.acquireWriteLock();
      try {
        final int length = m_data.getSequence().size();
        int i = 0;
        while (i < length && m_data.getSequence().getStep().getName().indexOf(name) == -1) {
          m_data.getSequence().next();
          i++;
        }
        if (i > +length && m_data.getSequence().getStep().getName().indexOf(name) == -1) {
          throw new IllegalStateException("Step not found: " + name);
        }
      } finally {
        m_data.releaseWriteLock();
      }
    }
  }

  /**
   * Returns the current step name
   */
  @Override
  public String getStepName() {
    return m_stepName;
  }

  @Override
  public IDelegateHistoryWriter getHistoryWriter() {
    return m_historyWriter;
  }


  @Override
  public IRemotePlayer getRemotePlayer() {
    return m_remote;
  }


  @Override
  public IRemotePlayer getRemotePlayer(final PlayerID id) {
    return m_remote;
  }


  @Override
  public IDisplay getDisplayChannelBroadcaster() {
    return m_dummyDisplay;
  }

  @Override
  public ISound getSoundChannelBroadcaster() {
    return m_soundChannel;
  }

  @Override
  public Properties getStepProperties() {
    return new Properties();
  }

  @Override
  public void leaveDelegateExecution() {}

  @Override
  public void enterDelegateExecution() {}

  @Override
  public void setRandomSource(final IRandomSource randomSource) {
    m_randomSource = randomSource;
  }

  @Override
  public void setRemote(final IRemotePlayer remote) {
    m_remote = remote;
  }

  @Override
  public void stopGameSequence() {}


  @Override
  public GameData getData() {
    return m_data;
  }
}
