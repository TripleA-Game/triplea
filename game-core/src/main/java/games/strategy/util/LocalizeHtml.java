package games.strategy.util;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.annotations.VisibleForTesting;

import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.ui.AbstractUiContext;
import lombok.extern.java.Log;

/**
 * Provides methods that convert relative links within a game description into absolute links that will work on the
 * local system.
 */
@Log
public final class LocalizeHtml {
  private static final String ASSET_IMAGE_FOLDER = "doc/images/";
  private static final String ASSET_IMAGE_NOT_FOUND = "notFound.png";

  // Match the <img /> tag
  public static final String PATTERN_HTML_IMG_TAG = "(?i)<img([^>]+)/>";
  // Match the <img> src
  private static final Pattern PATTERN_HTML_IMG_SRC_TAG = Pattern
      .compile("(<img[^>]*src\\s*=\\s*)(?:\"([^\"]*)\"|'([^']*)')([^>]*/?>)", Pattern.CASE_INSENSITIVE);

  private LocalizeHtml() {}

  /**
   * This is only useful once we are IN a game. Before we go into the game, resource loader will either be null, or be
   * the last game's resource loader.
   */
  public static String localizeImgLinksInHtml(final String htmlText) {
    return localizeImgLinksInHtml(htmlText, AbstractUiContext.getResourceLoader());
  }

  /**
   * Replaces relative image links within the HTML document {@code htmlText} with absolute links that point to the
   * correct location on the local file system.
   */
  public static String localizeImgLinksInHtml(final String htmlText, final String mapNameDir) {
    if (htmlText == null || mapNameDir == null || mapNameDir.trim().isEmpty()) {
      return htmlText;
    }
    return localizeImgLinksInHtml(htmlText, ResourceLoader.getMapResourceLoader(mapNameDir));
  }

  @VisibleForTesting
  static String localizeImgLinksInHtml(final String htmlText, final ResourceLoader loader) {
    final StringBuffer result = new StringBuffer();
    final Map<String, String> cache = new HashMap<>();
    final Matcher matcher = PATTERN_HTML_IMG_SRC_TAG.matcher(htmlText);
    while (matcher.find()) {
      final String link = Optional.ofNullable(matcher.group(2)).orElseGet(() -> matcher.group(3));
      if (link != null && !link.isEmpty()) {
        final String localized = cache.computeIfAbsent(link, l -> getLocalizedLink(l, loader));
        final char quote = matcher.group(2) != null ? '"' : '\'';
        matcher.appendReplacement(result, matcher.group(1) + quote + localized + quote + matcher.group(4));
      }
    }
    matcher.appendTail(result);

    return result.toString();
  }

  private static String getLocalizedLink(
      final String link,
      final ResourceLoader loader) {
    // remove full parent path
    final String imageFileName = link.substring(Math.max(link.lastIndexOf("/") + 1, 0));

    // replace when testing with: "REPLACEMENTPATH/" + imageFileName;
    final String firstOption = ASSET_IMAGE_FOLDER + imageFileName;
    URL replacementUrl = loader.getResource(firstOption);

    if (replacementUrl == null) {
      log.severe(String.format("Could not find: %s/%s", loader.getMapName(), firstOption));
      final String secondFallback = ASSET_IMAGE_FOLDER + ASSET_IMAGE_NOT_FOUND;
      replacementUrl = loader.getResource(secondFallback);
      if (replacementUrl == null) {
        log.severe(String.format("Could not find: %s", secondFallback));
        return link;
      }
    }
    return replacementUrl.toString();
  }
}
