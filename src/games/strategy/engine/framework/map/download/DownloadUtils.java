package games.strategy.engine.framework.map.download;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

public class DownloadUtils {
  public static int getDownloadLength(URL url) {
    try {
      HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
      int responseCode = httpConn.getResponseCode();
      // always check HTTP response code first
      if (responseCode == HttpURLConnection.HTTP_OK) {
        return httpConn.getContentLength();
      } else {
        return -1;
      }
    } catch (IOException e) {
      throw new IllegalStateException(e);
    }
  }

  public static URL toURL(String url) {
    try {
      return new URL(url);
    } catch (MalformedURLException e) {
      throw new IllegalStateException("Invalid URL: " + url, e);
    }
  }

  public static boolean beginsWithHttpProtocol(String urlString) {
    return urlString.startsWith("http://") || urlString.startsWith("https://");
  }

  public static void downloadFile(URL url, File targetFile) throws IOException {
    FileOutputStream fos = new FileOutputStream(targetFile);
    url = getUrlFollowingRedirects(url);
    ReadableByteChannel rbc = Channels.newChannel(url.openStream());
    fos.getChannel().transferFrom(rbc, 0, Long.MAX_VALUE);
    fos.close();
  }
  
  public static void downloadFile(String urlString, File targetFile) throws IOException {
    downloadFile(getUrlFollowingRedirects(urlString), targetFile);
  }

  private static URL getUrlFollowingRedirects(String possibleRedirectionUrl) throws IOException {
    URL url = new URL(possibleRedirectionUrl);
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }
  private static URL getUrlFollowingRedirects(URL url) throws IOException {
    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
    int status = conn.getResponseCode();
    if (status == HttpURLConnection.HTTP_MOVED_TEMP || status == HttpURLConnection.HTTP_MOVED_PERM
        || status == HttpURLConnection.HTTP_SEE_OTHER) {
      // update the URL if we were redirected
      url = new URL(conn.getHeaderField("Location"));
    }
    return url;
  }

}
