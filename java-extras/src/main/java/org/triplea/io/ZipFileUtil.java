package org.triplea.io;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;

@UtilityClass
@Slf4j
public class ZipFileUtil {

  /**
   * Finds all game XMLs in a zip file. More specifically, given a zip file, finds all '*.xml' files
   */
  public List<URI> findXmlFilesInZip(final File zip) {
    final List<URI> zipFiles = new ArrayList<>();

    try (ZipFile zipFile = new ZipFile(zip);
        URLClassLoader loader = new URLClassLoader(new URL[] {zip.toURI().toURL()})) {

      return zipFile.stream()
          .map(ZipEntry::getName)
          .filter(name -> name.toLowerCase().endsWith(".xml"))
          .map(loader::getResource)
          .filter(Objects::nonNull)
          .map(URL::toString)
          .map(string -> string.replace(" ", "%20"))
          .map(URI::create)
          .collect(Collectors.toList());
    } catch (final IOException e) {
      log.error("Error reading zip file in: " + zip.getAbsolutePath(), e);
    }
    return new ArrayList<>();
  }
}
