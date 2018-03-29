package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.awt.Point;
import java.awt.Polygon;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import games.strategy.util.function.ThrowingConsumer;
import org.apache.commons.io.input.CloseShieldInputStream;
import org.apache.commons.io.output.CloseShieldOutputStream;

/**
 * Utility to read and write files in the form of
 * String -> a list of points, or string-> list of polygons.
 */
public final class PointFileReaderWriter {
  private PointFileReaderWriter() {}

  /**
   * Returns a map of the form String -> Point.
   */
  public static Map<String, Point> readOneToOne(final InputStream stream) throws IOException {
    checkNotNull(stream);

    final Map<String, Point> mapping = new HashMap<>();
    readStream(stream, current -> readSingle(current, mapping));
    return mapping;
  }

  private static void readSingle(final String line, final Map<String, Point> mapping) throws IOException {
    final StringTokenizer tokens = new StringTokenizer(line, "", false);
    final String name = tokens.nextToken("(").trim();
    if (mapping.containsKey(name)) {
      throw new IOException("name found twice:" + name);
    }
    final int x = Integer.parseInt(tokens.nextToken("(, "));
    final int y = Integer.parseInt(tokens.nextToken(",) "));
    mapping.put(name, new Point(x, y));
  }

  /**
   * Writes the specified one-to-one mapping between names and points to the specified stream.
   *
   * @param sink The stream to which the name-to-point mappings will be written.
   * @param mapping The name-to-point mapping to be written.
   *
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToOne(final OutputStream sink, final Map<String, Point> mapping) throws IOException {
    checkNotNull(sink);
    checkNotNull(mapping);

    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final Point point = mapping.get(name);
      out.append(" (").append(point.x).append(",").append(point.y).append(")");
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out.toString(), sink);
  }

  /**
   * Writes the specified one-to-many mapping between names and polygons to the specified stream.
   *
   * @param sink The stream to which the name-to-polygons mappings will be written.
   * @param mapping The name-to-polygons mapping to be written.
   *
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToManyPolygons(final OutputStream sink, final Map<String, List<Polygon>> mapping)
      throws IOException {
    checkNotNull(sink);
    checkNotNull(mapping);

    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final List<Polygon> points = mapping.get(name);
      for (Polygon polygon : points) {
        out.append(" < ");
        for (int i = 0; i < polygon.npoints; i++) {
          out.append(" (").append(polygon.xpoints[i]).append(",").append(polygon.ypoints[i]).append(")");
        }
        out.append(" > ");
      }
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out.toString(), sink);
  }

  private static void write(final String string, final OutputStream sink) throws IOException {
    try (Writer out = new OutputStreamWriter(new CloseShieldOutputStream(sink), StandardCharsets.UTF_8)) {
      out.write(string);
    }
  }

  /**
   * Writes the specified one-to-many mapping between names and points to the specified stream.
   *
   * @param sink The stream to which the name-to-points mappings will be written.
   * @param mapping The name-to-points mapping to be written.
   *
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToMany(final OutputStream sink, final Map<String, List<Point>> mapping)
      throws IOException {
    checkNotNull(sink);
    checkNotNull(mapping);

    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final Collection<Point> points = mapping.get(name);
      final Iterator<Point> pointIter = points.iterator();
      while (pointIter.hasNext()) {
        final Point point = pointIter.next();
        out.append(" (").append(point.x).append(",").append(point.y).append(")");
        if (pointIter.hasNext()) {
          out.append(" ");
        }
      }
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out.toString(), sink);
  }

  /**
   * Returns a map of the form String -> Collection of points.
   */
  public static Map<String, List<Point>> readOneToMany(final InputStream stream) throws IOException {
    checkNotNull(stream);

    final Map<String, List<Point>> mapping = new HashMap<>();
    readStream(stream, current -> readMultiple(current, mapping));
    return mapping;
  }

  /**
   * Writes the specified one-to-many mapping between names and (points, overflowToLeft) to the specified stream.
   *
   * @param sink The stream to which the name-to-points mappings will be written.
   * @param mapping The name-to-points mapping to be written.
   *
   * @throws IOException If an I/O error occurs while writing to the stream.
   */
  public static void writeOneToManyPlacements(final OutputStream sink,
      final Map<String, Tuple<List<Point>, Boolean>> mapping)
      throws IOException {
    checkNotNull(sink);
    checkNotNull(mapping);

    final StringBuilder out = new StringBuilder();
    final Iterator<String> keyIter = mapping.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      out.append(name).append(" ");
      final Collection<Point> points = mapping.get(name).getFirst();
      final boolean overflowToLeft = mapping.get(name).getSecond();
      final Iterator<Point> pointIter = points.iterator();
      while (pointIter.hasNext()) {
        final Point point = pointIter.next();
        out.append(" (").append(point.x).append(",").append(point.y).append(")");
        if (pointIter.hasNext()) {
          out.append(" ");
        }
      }
      out.append(" | overflowToLeft=").append(overflowToLeft);
      if (keyIter.hasNext()) {
        out.append("\r\n");
      }
    }
    write(out.toString(), sink);
  }

  /**
   * Returns a map of the form String -> (points, overflowToLeft).
   */
  public static Map<String, Tuple<List<Point>, Boolean>> readOneToManyPlacements(final InputStream stream)
      throws IOException {
    checkNotNull(stream);

    final Map<String, List<Point>> mapping = new HashMap<>();
    final Map<String, Tuple<List<Point>, Boolean>> result = new HashMap<>();
    readStream(stream, current -> {
      final String[] s = current.split(" \\| ");
      final Tuple<String, List<Point>> tuple = readMultiple(s[0], mapping);
      final boolean overflowToLeft = s.length == 2 && Boolean.parseBoolean(s[1].split("=")[1]);
      result.put(tuple.getFirst(), Tuple.of(tuple.getSecond(), overflowToLeft));
    });
    return result;
  }

  /**
   * Returns a map of the form String -> Collection of polygons.
   */
  public static Map<String, List<Polygon>> readOneToManyPolygons(final InputStream stream) throws IOException {
    checkNotNull(stream);

    final Map<String, List<Polygon>> mapping = new HashMap<>();
    readStream(stream, current -> readMultiplePolygons(current, mapping));
    return mapping;
  }

  private static void readMultiplePolygons(final String line, final Map<String, List<Polygon>> mapping)
      throws IOException {
    try {
      // this loop is executed a lot when loading games
      // so it is hand optimized
      final String name = line.substring(0, line.indexOf('<')).trim();
      int index = name.length();
      final List<Polygon> polygons = new ArrayList<>(64);
      final List<Point> points = new ArrayList<>();
      final int length = line.length();
      while (index < length) {
        char current = line.charAt(index);
        if (current == '<') {
          int x = 0;
          int base = 0;
          // inside a poly
          while (true) {
            current = line.charAt(++index);
            final int y;
            switch (current) {
              case '0':
              case '1':
              case '2':
              case '3':
              case '4':
              case '5':
              case '6':
              case '7':
              case '8':
              case '9':
                base *= 10;
                base += current - '0';
                break;
              case ',':
                x = base;
                base = 0;
                break;
              case ')':
                y = base;
                base = 0;
                points.add(new Point(x, y));
                break;
              default:
                break;
            }
            if (current == '>') {
              // end poly
              createPolygonFromPoints(polygons, points);
              points.clear();
              // break from while(true)
              break;
            }
          }
        }
        index++;
      }
      if (mapping.containsKey(name)) {
        throw new IOException("name found twice:" + name);
      }
      mapping.put(name, polygons);
    } catch (final StringIndexOutOfBoundsException e) {
      throw new IllegalStateException("Invalid line:" + line, e);
    }
  }

  private static void createPolygonFromPoints(final Collection<Polygon> polygons, final List<Point> points) {
    final int[] pointsX = new int[points.size()];
    final int[] pointsY = new int[points.size()];
    for (int i = 0; i < points.size(); i++) {
      final Point p = points.get(i);
      pointsX[i] = p.x;
      pointsY[i] = p.y;
    }
    polygons.add(new Polygon(pointsX, pointsY, pointsX.length));
  }

  private static Tuple<String, List<Point>> readMultiple(final String line, final Map<String, List<Point>> mapping)
      throws IOException {
    final StringTokenizer tokens = new StringTokenizer(line, "");
    final String name = tokens.nextToken("(").trim();
    if (mapping.containsKey(name)) {
      throw new IOException("name found twice:" + name);
    }
    final List<Point> points = new ArrayList<>();
    while (tokens.hasMoreTokens()) {
      final String stringX = tokens.nextToken(",(), ");
      if (!tokens.hasMoreTokens()) {
        continue;
      }
      final String stringY = tokens.nextToken(",() ");
      final int x = Integer.parseInt(stringX);
      final int y = Integer.parseInt(stringY);
      points.add(new Point(x, y));
    }
    mapping.put(name, points);
    return Tuple.of(name, points);
  }

  private static void readStream(final InputStream stream, ThrowingConsumer<String, IOException> lineParser) throws IOException {
    try (Reader inputStreamReader = new InputStreamReader(new CloseShieldInputStream(stream), StandardCharsets.UTF_8);
        BufferedReader reader = new BufferedReader(inputStreamReader)) {
      for (String current = reader.readLine(); current != null; current = reader.readLine()) {
        if (current.trim().length() != 0) {
          lineParser.accept(current);
        }
      }
    }
  }
}
