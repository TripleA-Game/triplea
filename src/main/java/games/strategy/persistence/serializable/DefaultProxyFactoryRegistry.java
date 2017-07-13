package games.strategy.persistence.serializable;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

final class DefaultProxyFactoryRegistry implements ProxyFactoryRegistry {
  private final Map<Class<?>, ProxyFactory> proxyFactoriesByPrincipalType;

  DefaultProxyFactoryRegistry(final Collection<ProxyFactory> proxyFactories) {
    proxyFactoriesByPrincipalType = proxyFactories.stream()
        .collect(Collectors.toMap(ProxyFactory::getPrincipalType, Function.identity()));
  }

  @Override
  public Optional<ProxyFactory> getProxyFactory(final Class<?> principalType) {
    checkNotNull(principalType);

    return Optional.ofNullable(proxyFactoriesByPrincipalType.get(principalType));
  }
}
