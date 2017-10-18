package org.junit.experimental.extensions;

import java.io.File;
import java.io.IOException;

/**
 * Based off
 * https://raw.githubusercontent.com/rherrmann/junit5-experiments/master/src/main/java/com/codeaffine/junit5/TemporaryFolder.java
 */
public class TemporaryFolder {
  private File rootFolder;

  public File newFile(String name) throws IOException {
    File result = new File(rootFolder, name);
    result.createNewFile();
    return result;
  }

  void prepare() {
    try {
      rootFolder = File.createTempFile("junit5-", ".tmp");
    } catch (IOException ioe) {
      throw new RuntimeException(ioe);
    }
    rootFolder.delete();
    rootFolder.mkdirs();
    rootFolder.deleteOnExit();
  }
}
