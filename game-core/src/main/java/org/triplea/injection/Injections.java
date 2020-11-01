package org.triplea.injection;

import com.google.common.base.Preconditions;
import games.strategy.engine.framework.map.download.DownloadCoordinator;
import lombok.Builder;
import lombok.Getter;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.util.Version;

/**
 * Manages the creation of objects, similar to a dependency injection framework. Use this class to
 * manage singletons and as a factory to create objects that have shared dependencies already
 * managed by this class. Example usage:
 *
 * <pre>
 * <code>
 *   // before
 *   public void clientCode(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     swingStuff(sharedDependencyWiredThroughAllTheMethods);
 *     :
 *   }
 *   private void swingStuff(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     int preferenceValue =
 *         new UserSetting(sharedDependencyWiredThroughAllTheMethods).getNumberPreference();
 *     :
 *   }
 *
 *   // after
 *   public void clientCode() {
 *     doSwingStuff(ClientContext.userSettings());
 *     :
 *   }
 *
 *   private void doSwingStuff(UserSettings settings) {
 *     int preferenceValue = settings.getNumberPreference();
 *     :
 *   }
 * </code>
 * </pre>
 */
@Builder
@Getter
public final class Injections {
  @Getter private static Injections instance;

  @Builder.Default
  private final ProductVersionReader productVersionReader = new ProductVersionReader();

  @Builder.Default
  private final DownloadCoordinator downloadCoordinator = new DownloadCoordinator();

  public static synchronized void init(final Injections injections) {
    Preconditions.checkState(getInstance() == null);
    instance = injections;
  }

  public Version getEngineVersion() {
    return productVersionReader.getVersion();
  }
}
