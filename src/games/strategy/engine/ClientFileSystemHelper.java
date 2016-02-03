package games.strategy.engine;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.framework.GameRunner;
import games.strategy.engine.framework.GameRunner2;
import games.strategy.util.Version;

/**
 * Pure utility class, final and private constructor to enforce this
 * WARNING: do not call ClientContext.getInstance() in this class. ClientContext call this class in turn
 * during construction, depending upon ordering this can cause an infinite call loop.
 */
public final class ClientFileSystemHelper {
  private ClientFileSystemHelper() {

  }

  /** This method is available via ClientContext */
  public static File getRootFolder() {
    final String fileName = getGameRunnerFileLocation("GameRunner2.class");

    final String tripleaJarName = "triplea.jar!";
    if (fileName.contains(tripleaJarName)) {
      return getRootFolderRelativeToJar(fileName, tripleaJarName);
    }

//    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    if (fileName.contains("triplea_") && fileName.contains(".jar!")) {
      Pattern pattern = Pattern.compile("triplea_.*\\.jar!");
      Matcher matcher = pattern.matcher(fileName);

      String tripleaJarNameWithEngineVersion =   matcher.group();
      return getRootFolderRelativeToJar(fileName, tripleaJarNameWithEngineVersion);
    }

    return getRootRelativeToClassFile(fileName);
  }


  public static String getGameRunnerFileLocation(final String runnerClassName) {
    final URL url = GameRunner2.class.getResource(runnerClassName);
    String fileName = url.getFile();

    try {
      // Deal with spaces in the file name which would be url encoded
      fileName = URLDecoder.decode(fileName, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      ClientLogger.logError("Unsupported encoding of fileName: " + fileName + ", error: " + e.getMessage());
    }
    return fileName;
  }


  private static String getTripleaJarWithEngineVersionStringPath() {
    // TODO: This is begging for trouble since we call ClientFileSystem during the construction of
    // ClientContext. Though, we will at this point already have parsed the game engine version, so it is okay (but brittle)
    ClientContext context = ClientContext.getInstance();
    EngineVersion engine = context.engineVersion();
    Version version = engine.getVersion();

    return "triplea_" + version.toStringFull("_") + ".jar!";
  }

  private static File getRootFolderRelativeToJar(final String fileName, final String tripleaJarName) {
    final String subString =
        fileName.substring("file:/".length() - (GameRunner.isWindows() ? 0 : 1), fileName.indexOf(tripleaJarName) - 1);
    final File f = new File(subString).getParentFile();
    if (!f.exists()) {
      throw new IllegalStateException("File not found:" + f);
    }
    return f;
  }

  private static File getRootRelativeToClassFile(final String fileName) {
    File f = new File(fileName);

    // move up 1 directory for each package
    final int moveUpCount = GameRunner2.class.getName().split("\\.").length + 1;
    for (int i = 0; i < moveUpCount; i++) {
      f = f.getParentFile();
    }
    if (!f.exists()) {
      System.err.println("Could not find root folder, does  not exist:" + f);
      return new File(System.getProperties().getProperty("user.dir"));
    }
    return f;
  }

  public static boolean areWeOldExtraJar() {
    final URL url = GameRunner2.class.getResource("GameRunner2.class");
    String fileName = url.getFile();
    try {
      fileName = URLDecoder.decode(fileName, "UTF-8");
    } catch (final UnsupportedEncodingException e) {
      e.printStackTrace();
    }
    final String tripleaJarNameWithEngineVersion = getTripleaJarWithEngineVersionStringPath();
    if (fileName.contains(tripleaJarNameWithEngineVersion)) {
      final String subString = fileName.substring("file:/".length() - (GameRunner.isWindows() ? 0 : 1),
          fileName.indexOf(tripleaJarNameWithEngineVersion) - 1);
      final File f = new File(subString);
      if (!f.exists()) {
        throw new IllegalStateException("File not found:" + f);
      }
      String path;
      try {
        path = f.getCanonicalPath();
      } catch (final IOException e) {
        path = f.getPath();
      }
      return path.contains("old");
    }
    return false;
  }

}
