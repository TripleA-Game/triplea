package games.strategy.engine.framework.mapDownload;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.List;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner2;

public class DownloadRunnable implements Runnable {
  private final String urlString;
  private final boolean parse;
  private volatile byte[] contents;
  private volatile String error;
  private volatile List<DownloadFileDescription> downloads;

  public DownloadRunnable(final String urlString) {
    super();
    this.urlString = urlString;
    parse = false;
  }

  public DownloadRunnable(final String urlString, final boolean parseToo) {
    super();
    this.urlString = urlString;
    parse = parseToo;
  }

  public byte[] getContents() {
    return contents;
  }

  public void setContents(final byte[] contents) {
    this.contents = contents;
  }

  public List<DownloadFileDescription> getDownloads() {
    return downloads;
  }

  public void setDownloads(final List<DownloadFileDescription> downloads) {
    this.downloads = downloads;
  }

  public String getError() {
    return error;
  }

  public void setError(final String error) {
    this.error = error;
  }

  @Override
  public void run() {
    if (urlString.startsWith("http")) {
      downloadFile();
    } else {
      readLocalFile();
    }
  }

  private void downloadFile() {
    URL url;
    try {
      url = new URL(urlString.trim());
    } catch (final MalformedURLException e1) {
      error = "invalid url";
      return;
    }

    try (InputStream stream = url.openStream();
        ByteArrayOutputStream sink = new ByteArrayOutputStream()) {
      InstallMapDialog.copy(sink, stream);
      contents = sink.toByteArray();
    } catch (final Exception e) {
      error = e.getMessage();
    }
    if (parse && getError() == null) {
      try {
        downloads = new DownloadFileParser().parse(new ByteArrayInputStream(getContents()), urlString);
        if (downloads == null || downloads.isEmpty()) {
          error = "No games listed.";
        }
      } catch (final Exception e) {
        error = e.getMessage();
      }
    }
  }

  private void readLocalFile() {
    File targetFile = new File(GameRunner2.getRootFolder(), urlString);
    try {
      contents = Files.readAllBytes(targetFile.toPath());
      downloads = new DownloadFileParser().parse(new ByteArrayInputStream(getContents()), urlString);
    } catch (IOException e) {
      ClientLogger.logError("Failed to read file at: " + targetFile.getAbsolutePath(), e);
    }
  }

}
