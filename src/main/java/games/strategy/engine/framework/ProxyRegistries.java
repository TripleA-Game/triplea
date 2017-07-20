package games.strategy.engine.framework;

import games.strategy.internal.persistence.serializable.PropertyBagMementoProxy;
import games.strategy.internal.persistence.serializable.VersionProxy;
import games.strategy.persistence.serializable.ProxyRegistry;

final class ProxyRegistries {
  /**
   * A proxy registry that has been configured with all proxy factories required to serialize a game data memento.
   */
  static final ProxyRegistry GAME_DATA_MEMENTO = newGameDataMementoProxyRegistry();

  private ProxyRegistries() {}

  private static ProxyRegistry newGameDataMementoProxyRegistry() {
    return ProxyRegistry.newInstance(
        PropertyBagMementoProxy.FACTORY,
        VersionProxy.FACTORY);
  }
}
