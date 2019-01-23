package games.strategy.engine;

import java.util.List;
import java.util.Optional;

import org.triplea.common.config.product.ProductConfiguration;

import games.strategy.engine.framework.map.download.DownloadCoordinator;
import games.strategy.engine.framework.map.download.DownloadFileDescription;
import games.strategy.engine.framework.map.download.DownloadRunnable;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import games.strategy.util.Version;

/**
 * Manages the creation of objects, similar to a dependency injection framework.
 * Use this class to manage singletons and as a factory to create objects that have shared
 * dependencies already managed by this class.
 * Example usage:
 *
 * <pre>
 * <code>
 *   // before
 *   public void clientCode(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     swingStuff(sharedDependencyWiredThroughAllTheMethods);
 *     :
 *   }
 *   private void swingStuff(SharedDependency sharedDependencyWiredThroughAllTheMethods) {
 *     int preferenceValue = new UserSetting(sharedDependencyWiredThroughAllTheMethods).getNumberPreference();
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
public final class ClientContext {
  private static final ClientContext instance = new ClientContext();

  private final ProductConfiguration productConfiguration = new ProductConfiguration();
  private final DownloadCoordinator downloadCoordinator = new DownloadCoordinator();

  private ClientContext() {}

  public static DownloadCoordinator downloadCoordinator() {
    return instance.downloadCoordinator;
  }

  public static Version engineVersion() {
    return instance.productConfiguration.getVersion();
  }

  public static Optional<List<DownloadFileDescription>> getMapDownloadList() {
    final String mapDownloadListUrl = ClientSetting.mapListOverride.isSet()
        ? ClientSetting.mapListOverride.getValueOrThrow().toString()
        : UrlConstants.MAP_DOWNLOAD_LIST.toString();

    return new DownloadRunnable(mapDownloadListUrl).getDownloads();
  }
}
