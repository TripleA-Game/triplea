package org.triplea.live.servers;

import com.google.common.annotations.VisibleForTesting;
import games.strategy.engine.framework.system.HttpProxy;
import games.strategy.triplea.UrlConstants;
import games.strategy.triplea.settings.ClientSetting;
import java.io.IOException;
import java.net.URI;
import java.util.Optional;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.triplea.config.product.ProductVersionReader;
import org.triplea.http.client.HttpInteractionException;
import org.triplea.http.client.latest.version.LatestVersionClient;
import org.triplea.injection.Injections;
import org.triplea.io.CloseableDownloader;
import org.triplea.io.ContentDownloader;
import org.triplea.java.function.ThrowingSupplier;
import org.triplea.swing.SwingComponents;
import org.triplea.util.Version;

/**
 * Fetches {@code LiveServers} information from network (or if already fetched, from cache). {@code
 * LiveServers} can be used to determine the latest TripleA version and the address of any lobbies
 * that can be connected to.
 */
@Slf4j
@AllArgsConstructor(onConstructor_ = @VisibleForTesting)
public class LiveServersFetcher {
  private final Function<LiveServers, ServerProperties> currentVersionSelector;
  private final ThrowingSupplier<LiveServers, IOException> liveServersFetcher;

  public LiveServersFetcher() {
    this(
        () -> new ContentDownloader(UrlConstants.LIVE_SERVERS_URI, HttpProxy::addProxy),
        Injections.getInstance().getEngineVersion());
  }

  @VisibleForTesting
  LiveServersFetcher(
      final ThrowingSupplier<CloseableDownloader, IOException> networkFetcher,
      final Version engineVersion) {
    this(
        new CurrentVersionSelector(new ProductVersionReader().getVersion()),
        FetchingCache.builder()
            .contentDownloader(networkFetcher)
            .yamlParser(new ServerYamlParser())
            .engineVersion(engineVersion)
            .build());
  }

  /**
   * Fetches from online configuration server properties. The online configuration is a fixed
   * configuration file at a known URI. The server properties lists which lobbies are available for
   * which game versions.
   */
  public static Optional<ServerProperties> fetch() {
    final ServerProperties serverProperties = new LiveServersFetcher().serverForCurrentVersion();
    if (serverProperties.isInactive()) {
      SwingComponents.showDialogWithLinks(
          SwingComponents.DialogWithLinksParams.builder()
              .title("Lobby Not Available")
              .dialogText(
                  String.format(
                      "Your version of TripleA is out of date, please download the latest:"
                          + "<br><a href=\"%s\">%s</a>",
                      UrlConstants.DOWNLOAD_WEBSITE, UrlConstants.DOWNLOAD_WEBSITE))
              .dialogType(SwingComponents.DialogWithLinksTypes.ERROR)
              .build());
      return Optional.empty();
    }
    return Optional.of(serverProperties);
  }

  public Optional<Version> latestVersion() {
    try {

      final String latestVersion =
          LatestVersionClient.newClient(ClientSetting.lobbyUri.getValueOrThrow())
              .fetchLatestVersion()
              .getLatestEngineVersion();
      final var version = new Version(latestVersion);
      return Optional.of(version);
    } catch (final HttpInteractionException e) {
      log.info(
          "Unable to complete engine out-of-date check. " + "(Offline or server not available?)",
          e);
      return Optional.empty();
    }
  }

  public ServerProperties serverForCurrentVersion() {
    try {
      final var liveServers = liveServersFetcher.get();
      return currentVersionSelector.apply(liveServers);
    } catch (final IOException e) {
      throw new LobbyAddressFetchException(e);
    }
  }

  @VisibleForTesting
  public static final class LobbyAddressFetchException extends RuntimeException {
    private static final long serialVersionUID = -301010780022774627L;

    LobbyAddressFetchException(final IOException e) {
      super("Failed to fetch lobby address, check network connection.", e);
    }
  }

  public Optional<URI> lobbyUriForCurrentVersion() {
    return fetchServerProperties().map(ServerProperties::getUri);
  }

  private Optional<ServerProperties> fetchServerProperties() {
    try {
      final var liveServers = liveServersFetcher.get();
      return Optional.of(currentVersionSelector.apply(liveServers));
    } catch (final IOException e) {
      log.warn("(No network connection?) Failed to get server locations", e);
      return Optional.empty();
    }
  }
}
