package games.strategy.triplea.delegate.remote;

import games.strategy.engine.delegate.IDelegate;
import games.strategy.engine.message.IRemote;
import games.strategy.triplea.attachments.PoliticalActionAttachment;
import java.util.Collection;

/** Logic for performing political actions. */
public interface IPoliticsDelegate extends IRemote, IDelegate {
  void attemptAction(PoliticalActionAttachment actionChoice);

  Collection<PoliticalActionAttachment> getValidActions();
}
