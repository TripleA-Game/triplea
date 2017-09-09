package games.strategy.internal.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Map;

import javax.annotation.concurrent.Immutable;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.IAttachment;
import games.strategy.persistence.serializable.Proxy;
import games.strategy.persistence.serializable.ProxyFactory;
import games.strategy.triplea.delegate.AARadarAdvance;

/**
 * A serializable proxy for the {@link AARadarAdvance} class.
 */
@Immutable
public final class AaRadarAdvanceProxy implements Proxy {
  private static final long serialVersionUID = 4097155483091551144L;

  public static final ProxyFactory FACTORY = ProxyFactory.newInstance(AARadarAdvance.class, AaRadarAdvanceProxy::new);

  private final Map<String, IAttachment> attachments;
  private final GameData gameData;

  public AaRadarAdvanceProxy(final AARadarAdvance aaRadarAdvance) {
    checkNotNull(aaRadarAdvance);

    attachments = aaRadarAdvance.getAttachments();
    gameData = aaRadarAdvance.getData();
  }

  @Override
  public Object readResolve() {
    final AARadarAdvance aaRadarAdvance = new AARadarAdvance(gameData);
    attachments.forEach(aaRadarAdvance::addAttachment);
    return aaRadarAdvance;
  }
}
