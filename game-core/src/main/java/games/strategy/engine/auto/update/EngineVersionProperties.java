package games.strategy.engine.auto.update;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.io.IOException;
import java.net.URL;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.swing.BorderFactory;
import javax.swing.JEditorPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.ClientContext;
import games.strategy.net.OpenFileUtility;
import games.strategy.triplea.UrlConstants;
import games.strategy.util.Version;

class EngineVersionProperties {
  private static final String TRIPLEA_VERSION_LINK =
      "https://raw.githubusercontent.com/triplea-game/triplea/master/latest_version.properties";
  private final Version latestVersionOut;
  private final String link;
  private final String changelogLink;

  private EngineVersionProperties() {
    this(getProperties());
  }

  private EngineVersionProperties(final Properties props) {
    latestVersionOut =
        new Version(props.getProperty("LATEST", ClientContext.engineVersion().toStringFull()));
    link = props.getProperty("LINK", UrlConstants.DOWNLOAD_WEBSITE.toString());
    changelogLink = props.getProperty("CHANGELOG", UrlConstants.RELEASE_NOTES.toString());
  }


  static EngineVersionProperties contactServerForEngineVersionProperties() {
    // sourceforge sometimes takes a long while to return results
    // so run a couple requests in parallel, starting with delays to try and get a response quickly
    final AtomicReference<EngineVersionProperties> ref = new AtomicReference<>();
    final CountDownLatch latch = new CountDownLatch(1);
    for (int i = 0; i < 5; i++) {
      new Thread(() -> {
        ref.set(new EngineVersionProperties());
        latch.countDown();
      }).start();
      try {
        latch.await(2, TimeUnit.SECONDS);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      if (ref.get() != null) {
        break;
      }
    }
    // we have spawned a bunch of requests
    try {
      latch.await(15, TimeUnit.SECONDS);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
    }
    return ref.get();
  }

  private static Properties getProperties() {
    final Properties props = new Properties();
    try {
      props.load(new URL(TRIPLEA_VERSION_LINK).openStream());
    } catch (final IOException e) {
      ClientLogger.logQuietly("Failed while attempting to check for a new Version", e);
    }
    return props;
  }

  public Version getLatestVersionOut() {
    return latestVersionOut;
  }

  private String getOutOfDateMessage() {
    return "<html>" + "<h2>A new version of TripleA is out.  Please Update TripleA!</h2>"
        + "<br />Your current version: " + ClientContext.engineVersion().getExactVersion()
        + "<br />Latest version available for download: " + getLatestVersionOut()
        + "<br /><br />Click to download: <a class=\"external\" href=\"" + link
        + "\">" + link + "</a>"
        + "<br /><br />Please note that installing a new version of TripleA will not remove any old copies of "
        + "TripleA."
        + "<br />So be sure to either manually uninstall all older versions of TripleA, or change your "
        + "shortcuts to the new TripleA."
        + "<br /><br />What is new:<br />"
        + "</html>";
  }

  private String getOutOfDateReleaseUpdates() {
    return "<html><body>" + "Link to full Change Log:<br /><a class=\"external\" href=\"" + changelogLink + "\">"
            + changelogLink + "</a><br />"
            + "</body></html>";
  }

  Component getOutOfDateComponent() {
    final JPanel panel = new JPanel(new BorderLayout());
    final JEditorPane intro = new JEditorPane("text/html", getOutOfDateMessage());
    intro.setEditable(false);
    intro.setOpaque(false);
    intro.setBorder(BorderFactory.createEmptyBorder());
    final HyperlinkListener hyperlinkListener = e -> {
      if (HyperlinkEvent.EventType.ACTIVATED.equals(e.getEventType())) {
        OpenFileUtility.openUrl(e.getDescription());
      }
    };
    intro.addHyperlinkListener(hyperlinkListener);
    panel.add(intro, BorderLayout.NORTH);
    final JEditorPane updates = new JEditorPane("text/html", getOutOfDateReleaseUpdates());
    updates.setEditable(false);
    updates.setOpaque(false);
    updates.setBorder(BorderFactory.createEmptyBorder());
    updates.addHyperlinkListener(hyperlinkListener);
    updates.setCaretPosition(0);
    final JScrollPane scroll = new JScrollPane(updates);
    // scroll.setBorder(BorderFactory.createEmptyBorder());
    panel.add(scroll, BorderLayout.CENTER);
    final Dimension maxDimension = panel.getPreferredSize();
    maxDimension.width = Math.min(maxDimension.width, 700);
    maxDimension.height = Math.min(maxDimension.height, 480);
    panel.setMaximumSize(maxDimension);
    panel.setPreferredSize(maxDimension);
    return panel;
  }
}
