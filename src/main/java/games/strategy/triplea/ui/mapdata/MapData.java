package games.strategy.triplea.ui.mapdata;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;

import javax.imageio.ImageIO;

import games.strategy.debug.ClientLogger;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.Territory;
import games.strategy.triplea.Constants;
import games.strategy.triplea.ResourceLoader;
import games.strategy.triplea.image.UnitImageFactory;
import games.strategy.ui.Util;
import games.strategy.util.PointFileReaderWriter;
import games.strategy.util.UrlStreams;

/**
 * contains data about the territories useful for drawing.
 */
public class MapData implements Closeable {
  public static final String PROPERTY_UNITS_SCALE = "units.scale";
  public static final String PROPERTY_UNITS_WIDTH = "units.width";
  public static final String PROPERTY_UNITS_HEIGHT = "units.height";
  public static final String PROPERTY_SCREENSHOT_TITLE_ENABLED = "screenshot.title.enabled";
  public static final String PROPERTY_SCREENSHOT_TITLE_X = "screenshot.title.x";
  public static final String PROPERTY_SCREENSHOT_TITLE_Y = "screenshot.title.y";
  public static final String PROPERTY_SCREENSHOT_TITLE_COLOR = "screenshot.title.color";
  public static final String PROPERTY_SCREENSHOT_TITLE_FONT_SIZE = "screenshot.title.font.size";
  public static final String PROPERTY_COLOR_PREFIX = "color.";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_WIDTH = "units.counter.offset.width";
  public static final String PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT = "units.counter.offset.height";
  public static final String PROPERTY_UNITS_STACK_SIZE = "units.stack.size";
  public static final String PROPERTY_MAP_WIDTH = "map.width";
  public static final String PROPERTY_MAP_HEIGHT = "map.height";
  public static final String PROPERTY_MAP_SCROLLWRAPX = "map.scrollWrapX";
  public static final String PROPERTY_MAP_SCROLLWRAPY = "map.scrollWrapY";
  public static final String PROPERTY_MAP_HASRELIEF = "map.hasRelief";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_X = "map.cursor.hotspot.x";
  public static final String PROPERTY_MAP_CURSOR_HOTSPOT_Y = "map.cursor.hotspot.y";
  public static final String PROPERTY_MAP_SHOWCAPITOLMARKERS = "map.showCapitolMarkers";
  public static final String PROPERTY_MAP_USETERRITORYEFFECTMARKERS = "map.useTerritoryEffectMarkers";
  public static final String PROPERTY_MAP_SHOWTERRITORYNAMES = "map.showTerritoryNames";
  public static final String PROPERTY_MAP_SHOWRESOURCES = "map.showResources";
  public static final String PROPERTY_MAP_SHOWCOMMENTS = "map.showComments";
  public static final String PROPERTY_MAP_SHOWSEAZONENAMES = "map.showSeaZoneNames";
  public static final String PROPERTY_MAP_DRAWNAMESFROMTOPLEFT = "map.drawNamesFromTopLeft";
  public static final String PROPERTY_MAP_USENATION_CONVOYFLAGS = "map.useNation_convoyFlags";
  public static final String PROPERTY_DONT_DRAW_TERRITORY_NAMES = "dont_draw_territory_names";
  public static final String PROPERTY_MAP_MAPBLENDS = "map.mapBlends";
  public static final String PROPERTY_MAP_MAPBLENDMODE = "map.mapBlendMode";
  public static final String PROPERTY_MAP_MAPBLENDALPHA = "map.mapBlendAlpha";

  private static final String CENTERS_FILE = "centers.txt";
  private static final String POLYGON_FILE = "polygons.txt";
  private static final String PLACEMENT_FILE = "place.txt";
  private static final String TERRITORY_EFFECT_FILE = "territory_effects.txt";
  private static final String MAP_PROPERTIES = "map.properties";
  private static final String CAPITAL_MARKERS = "capitols.txt";
  private static final String CONVOY_MARKERS = "convoy.txt";
  private static final String COMMENT_MARKERS = "comments.txt";
  private static final String VC_MARKERS = "vc.txt";
  private static final String BLOCKADE_MARKERS = "blockade.txt";
  private static final String PU_PLACE_FILE = "pu_place.txt";
  private static final String TERRITORY_NAME_PLACE_FILE = "name_place.txt";
  private static final String KAMIKAZE_FILE = "kamikaze_place.txt";
  private static final String DECORATIONS_FILE = "decorations.txt";

  private final List<Color> defaultColors = Arrays.asList(Color.RED, Color.MAGENTA, Color.YELLOW, Color.ORANGE,
      Color.CYAN, Color.GREEN, Color.PINK, Color.GRAY);
  private final Map<String, Color> playerColors = new HashMap<>();
  private Map<String, List<Point>> place;
  private Map<String, List<Polygon>> polys;
  private Map<String, Point> centers;
  private Map<String, Point> vcPlace;
  private Map<String, Point> blockadePlace;
  private Map<String, Point> convoyPlace;
  private Map<String, Point> commentPlace;
  private Map<String, Point> puPlace;
  private Map<String, Point> namePlace;
  private Map<String, Point> kamikazePlace;
  private Map<String, Point> capitolPlace;
  private Map<String, List<String>> contains;
  private Properties mapProperties;
  private Map<String, List<Point>> territoryEffects;
  private Set<String> undrawnTerritoriesNames;
  private Map<Image, List<Point>> decorations;
  private Map<String, Image> territoryNameImages;
  private final Map<String, Image> effectImages = new HashMap<>();
  private final ResourceLoader resourceLoader;

  public boolean scrollWrapX() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPX, "true"));
  }

  public boolean scrollWrapY() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SCROLLWRAPY, "false"));
  }

  public MapData(final String mapNameDir) {
    this(ResourceLoader.getMapResourceLoader(mapNameDir));
  }

  /**
   * Constructor TerritoryData(java.lang.String)
   * Sets the map directory for this instance of TerritoryData
   *
   * @param loader
   *        .lang.String
   *        mapNameDir the given map directory
   */
  public MapData(final ResourceLoader loader) {
    resourceLoader = loader;
    try {
      final String prefix = "";
      place = PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + PLACEMENT_FILE));
      territoryEffects =
          PointFileReaderWriter.readOneToMany(loader.getResourceAsStream(prefix + TERRITORY_EFFECT_FILE));

      if (loader.getResourceAsStream(prefix + POLYGON_FILE) == null) {
        throw new IllegalStateException(
            "Error in resource loading. Unable to load expected resource: " + prefix + POLYGON_FILE + ", the error"
                + " is that either we did not find the correct path to load. Check the resource loader to make"
                + " sure the map zip or dir was added. Failing that, the path in this error message should be available"
                + " relative to the map folder, or relative to the root of the map zip");
      }
      
      polys = PointFileReaderWriter.readOneToManyPolygons(loader.getResourceAsStream(prefix + POLYGON_FILE));
      centers = PointFileReaderWriter.readOneToOneCenters(loader.getResourceAsStream(prefix + CENTERS_FILE));
      vcPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + VC_MARKERS));
      convoyPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CONVOY_MARKERS));
      commentPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + COMMENT_MARKERS));
      blockadePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + BLOCKADE_MARKERS));
      capitolPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + CAPITAL_MARKERS));
      puPlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + PU_PLACE_FILE));
      namePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + TERRITORY_NAME_PLACE_FILE));
      kamikazePlace = PointFileReaderWriter.readOneToOne(loader.getResourceAsStream(prefix + KAMIKAZE_FILE));
      mapProperties = new Properties();
      decorations = loadDecorations();
      territoryNameImages = territoryNameImages();
      try {
        final URL url = loader.getResource(prefix + MAP_PROPERTIES);
        if (url == null) {
          throw new IllegalStateException("No map.properties file defined");
        }
        final Optional<InputStream> inputStream = UrlStreams.openStream(url);
        if (inputStream.isPresent()) {
          mapProperties.load(inputStream.get());
        }
      } catch (final Exception e) {
        System.out.println("Error reading map.properties:" + e);
      }
      initializeContains();
    } catch (final IOException ex) {
      ClientLogger.logQuietly(ex);
    }
  }

  @Override
  public void close() {
    resourceLoader.close();
  }

  private Map<String, Image> territoryNameImages() {
    if (!resourceLoader.hasPath("territoryNames/")) {
      return new HashMap<>();
    }

    final Map<String, Image> territoryNameImages = new HashMap<>();
    for (final String name : centers.keySet()) {
      final Optional<Image> territoryNameImage = loadTerritoryNameImage(name);

      if (territoryNameImage.isPresent()) {
        territoryNameImages.put(name, territoryNameImage.get());
      }
    }
    return territoryNameImages;
  }

  private Optional<Image> loadTerritoryNameImage(final String imageName) {
    Optional<Image> img;
    try {
      // try first file names that have underscores instead of spaces
      final String normalizedName = imageName.replace(' ', '_');
      img = loadImage(constructTerritoryNameImagePath(normalizedName));
      if (!img.isPresent()) {
        img = loadImage(constructTerritoryNameImagePath(imageName));
      }
      return img;
    } catch (final Exception e) {
      // TODO: this is checking for IllegalStateException - we should bubble up the Optional image load and just
      // check instead if the optional is empty.
      ClientLogger.logQuietly("Image loading failed: " + imageName, e);
      return Optional.empty();
    }
  }

  private static String constructTerritoryNameImagePath(final String baseName) {
    return "territoryNames/" + baseName + ".png";
  }

  private Map<Image, List<Point>> loadDecorations() {
    final URL decorationsFileUrl = resourceLoader.getResource(DECORATIONS_FILE);
    if (decorationsFileUrl == null) {
      return Collections.emptyMap();
    }
    final Map<Image, List<Point>> decorations = new HashMap<>();
    final Optional<InputStream> inputStream = UrlStreams.openStream(decorationsFileUrl);
    if (inputStream.isPresent()) {
      final Map<String, List<Point>> points = PointFileReaderWriter.readOneToMany(inputStream.get());
      for (final String name : points.keySet()) {
        final Optional<Image> img = loadImage("misc/" + name);
        if (img.isPresent()) {
          decorations.put(img.get(), points.get(name));
        }
      }
    }
    return decorations;
  }

  public double getDefaultUnitScale() {
    if (mapProperties.getProperty(PROPERTY_UNITS_SCALE) == null) {
      return 1.0;
    }
    try {
      return Double.parseDouble(mapProperties.getProperty(PROPERTY_UNITS_SCALE));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return 1.0;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitWidth() {
    if (mapProperties.getProperty(PROPERTY_UNITS_WIDTH) == null) {
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
    try {
      return Integer.parseInt(mapProperties.getProperty(PROPERTY_UNITS_WIDTH));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitHeight() {
    if (mapProperties.getProperty(PROPERTY_UNITS_HEIGHT) == null) {
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
    try {
      return Integer.parseInt(mapProperties.getProperty(PROPERTY_UNITS_HEIGHT));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return UnitImageFactory.DEFAULT_UNIT_ICON_SIZE;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetWidth() {
    // if it is not set, divide by 4 so that it is roughly centered
    if (mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH) == null) {
      return getDefaultUnitWidth() / 4;
    }
    try {
      return Integer.parseInt(mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_WIDTH));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return getDefaultUnitWidth() / 4;
    }
  }

  /**
   * Does not take account of any scaling.
   */
  public int getDefaultUnitCounterOffsetHeight() {
    // put at bottom of unit, if not set
    if (mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT) == null) {
      return getDefaultUnitHeight();
    }
    try {
      return Integer.parseInt(mapProperties.getProperty(PROPERTY_UNITS_COUNTER_OFFSET_HEIGHT));
    } catch (final NumberFormatException e) {
      ClientLogger.logQuietly(e);
      return getDefaultUnitHeight();
    }
  }

  public int getDefaultUnitsStackSize() {
    // zero = normal behavior
    final String stack = mapProperties.getProperty(PROPERTY_UNITS_STACK_SIZE, "0");
    return Math.max(0, Integer.parseInt(stack));
  }

  public boolean shouldDrawTerritoryName(final String territoryName) {
    if (undrawnTerritoriesNames == null) {
      final String property = mapProperties.getProperty(PROPERTY_DONT_DRAW_TERRITORY_NAMES, "");
      undrawnTerritoriesNames = new HashSet<>(Arrays.asList(property.split(",")));
    }
    return !undrawnTerritoriesNames.contains(territoryName);
  }

  public boolean getHasRelief() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_HASRELIEF, "true"));
  }

  public int getMapCursorHotspotX() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_X, "0"))));
  }

  public int getMapCursorHotspotY() {
    return Math.max(0,
        Math.min(256, Integer.parseInt(mapProperties.getProperty(PROPERTY_MAP_CURSOR_HOTSPOT_Y, "0"))));
  }

  public boolean getHasMapBlends() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDS, "false"));
  }

  public String getMapBlendMode() {
    return String.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDMODE, "normal"));
  }

  public float getMapBlendAlpha() {
    return Float.valueOf(mapProperties.getProperty(PROPERTY_MAP_MAPBLENDALPHA, "0.5f"));
  }

  public boolean drawCapitolMarkers() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWCAPITOLMARKERS, "true"));
  }

  public boolean drawTerritoryNames() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWTERRITORYNAMES, "true"));
  }

  public boolean drawResources() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWRESOURCES, "true"));
  }

  public boolean drawComments() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWCOMMENTS, "true"));
  }

  public boolean drawSeaZoneNames() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_SHOWSEAZONENAMES, "false"));
  }

  public boolean drawNamesFromTopLeft() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_DRAWNAMESFROMTOPLEFT, "false"));
  }

  public boolean useNation_convoyFlags() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_USENATION_CONVOYFLAGS, "false"));
  }

  public boolean useTerritoryEffectMarkers() {
    return Boolean.valueOf(mapProperties.getProperty(PROPERTY_MAP_USETERRITORYEFFECTMARKERS, "false"));
  }

  private void initializeContains() {
    contains = new HashMap<>();
    final Iterator<String> seaIter = getTerritories().iterator();
    while (seaIter.hasNext()) {
      final List<String> contained = new ArrayList<>();
      final String seaTerritory = seaIter.next();
      if (!Util.isTerritoryNameIndicatingWater(seaTerritory)) {
        continue;
      }
      final Iterator<String> landIter = getTerritories().iterator();
      while (landIter.hasNext()) {
        final String landTerritory = landIter.next();
        if (Util.isTerritoryNameIndicatingWater(landTerritory)) {
          continue;
        }
        final Polygon landPoly = getPolygons(landTerritory).iterator().next();
        final Polygon seaPoly = getPolygons(seaTerritory).iterator().next();
        if (seaPoly.contains(landPoly.getBounds())) {
          contained.add(landTerritory);
        }
      }
      if (!contained.isEmpty()) {
        contains.put(seaTerritory, contained);
      }
    }
  }

  public boolean getBooleanProperty(final String propertiesKey) {
    return Boolean.valueOf(mapProperties.getProperty(propertiesKey, "true"));
  }

  public Color getColorProperty(final String propertiesKey) throws IllegalStateException {
    String colorString;
    if (mapProperties.getProperty(propertiesKey) != null) {
      colorString = mapProperties.getProperty(propertiesKey);
      if (colorString.length() != 6) {
        throw new IllegalStateException("Colors must be a 6 digit hex number, eg FF0011, not:" + colorString);
      }
      try {
        return new Color(Integer.decode("0x" + colorString));
      } catch (final NumberFormatException nfe) {
        throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
      }
    }
    return null;
  }

  public Color getPlayerColor(final String playerName) {
    // already loaded, just return
    if (playerColors.containsKey(playerName)) {
      return playerColors.get(playerName);
    }
    // look in map.properties
    final String propertiesKey = PROPERTY_COLOR_PREFIX + playerName;
    Color color = null;
    try {
      color = getColorProperty(propertiesKey);
    } catch (final Exception e) {
      throw new IllegalStateException("Player colors must be a 6 digit hex number, eg FF0011");
    }
    if (color == null) {
      System.out.println("No color defined for " + playerName + ".  Edit map.properties in the map folder to set it");
      color = defaultColors.remove(0);
    }
    // dont crash, use one of our default colors
    // its ugly, but usable
    playerColors.put(playerName, color);
    return color;
  }

  /**
   * returns the named property, or null.
   */
  public String getProperty(final String propertiesKey) {
    return mapProperties.getProperty(propertiesKey);
  }

  /**
   * returns the color for impassable territories.
   */
  public Color impassableColor() {
    // just use getPlayerColor, since it parses the properties
    return getPlayerColor(Constants.PLAYER_NAME_IMPASSABLE);
  }

  /**
   * @return a Set of territory names as Strings. generally this shouldnt be
   *         used, instead you should use aGameData.getMap().getTerritories()
   */
  public Set<String> getTerritories() {
    return polys.keySet();
  }

  /**
   * Does this territory have any territories contained within it.
   */
  public boolean hasContainedTerritory(final String territoryName) {
    return contains.containsKey(territoryName);
  }

  /**
   * returns the name of the territory contained in the given territory. This
   * applies to islands within sea zones.
   *
   * @return possiblly null
   */
  public List<String> getContainedTerritory(final String territoryName) {
    return contains.get(territoryName);
  }

  public void verify(final GameData data) {
    verifyKeys(data, centers, "centers");
    verifyKeys(data, polys, "polygons");
    verifyKeys(data, place, "place");
  }

  private static void verifyKeys(final GameData data, final Map<String, ?> map, final String dataTypeForErrorMessage)
      throws IllegalStateException {
    final StringBuilder errors = new StringBuilder();
    final Iterator<String> iter = map.keySet().iterator();
    while (iter.hasNext()) {
      final String name = iter.next();
      final Territory terr = data.getMap().getTerritory(name);
      // allow loading saved games with missing territories; just ignore them
      if (terr == null) {
        iter.remove();
      }
    }
    final Iterator<Territory> territories = data.getMap().getTerritories().iterator();
    final Set<String> keySet = map.keySet();
    while (territories.hasNext()) {
      final Territory terr = territories.next();
      if (!keySet.contains(terr.getName())) {
        errors.append("No data of type ").append(dataTypeForErrorMessage).append(" for territory:")
            .append(terr.getName()).append("\n");
      }
    }
    if (errors.length() > 0) {
      throw new IllegalStateException(errors.toString());
    }
  }

  public List<Point> getPlacementPoints(final Territory terr) {
    return place.get(terr.getName());
  }

  public List<Polygon> getPolygons(final String terr) {
    return polys.get(terr);
  }

  public List<Polygon> getPolygons(final Territory terr) {
    return getPolygons(terr.getName());
  }

  public Point getCenter(final String terr) {
    if (centers.get(terr) == null) {
      throw new IllegalStateException("Missing " + CENTERS_FILE + " data for " + terr);
    }
    return new Point(centers.get(terr));
  }

  public Point getCenter(final Territory terr) {
    return getCenter(terr.getName());
  }

  public Point getCapitolMarkerLocation(final Territory terr) {
    if (capitolPlace.containsKey(terr.getName())) {
      return capitolPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getConvoyMarkerLocation(final Territory terr) {
    if (convoyPlace.containsKey(terr.getName())) {
      return convoyPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getCommentMarkerLocation(final Territory terr) {
    return Optional.ofNullable(commentPlace.get(terr.getName()));
  }

  public Point getKamikazeMarkerLocation(final Territory terr) {
    if (kamikazePlace.containsKey(terr.getName())) {
      return kamikazePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getVCPlacementPoint(final Territory terr) {
    if (vcPlace.containsKey(terr.getName())) {
      return vcPlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Point getBlockadePlacementPoint(final Territory terr) {
    if (blockadePlace.containsKey(terr.getName())) {
      return blockadePlace.get(terr.getName());
    }
    return getCenter(terr);
  }

  public Optional<Point> getPUPlacementPoint(final Territory terr) {
    return Optional.ofNullable(puPlace.get(terr.getName()));
  }

  public Optional<Point> getNamePlacementPoint(final Territory terr) {
    return Optional.ofNullable(namePlace.get(terr.getName()));
  }

  /**
   * Get the territory at the x,y co-ordinates could be null.
   */
  public String getTerritoryAt(final double x, final double y) {
    String seaName = null;
    // try to find a land territory.
    // sea zones often surround a land territory
    final Iterator<String> keyIter = polys.keySet().iterator();
    while (keyIter.hasNext()) {
      final String name = keyIter.next();
      final Collection<Polygon> polygons = polys.get(name);
      final Iterator<Polygon> polyIter = polygons.iterator();
      while (polyIter.hasNext()) {
        final Polygon poly = polyIter.next();
        if (poly.contains(x, y)) {
          if (Util.isTerritoryNameIndicatingWater(name)) {
            seaName = name;
          } else {
            return name;
          }
        }
      }
    }
    return seaName;
  }

  public Dimension getMapDimensions() {
    final String widthProperty = mapProperties.getProperty(PROPERTY_MAP_WIDTH);
    final String heightProperty = mapProperties.getProperty(PROPERTY_MAP_HEIGHT);
    if (widthProperty == null || heightProperty == null) {
      throw new IllegalStateException(
          "Missing " + PROPERTY_MAP_WIDTH + " or " + PROPERTY_MAP_HEIGHT + " in " + MAP_PROPERTIES);
    }
    final int width = Integer.parseInt(widthProperty.trim());
    final int height = Integer.parseInt(heightProperty.trim());
    return new Dimension(width, height);
  }

  public Rectangle getBoundingRect(final Territory terr) {
    final String name = terr.getName();
    return getBoundingRect(name);
  }

  public Rectangle getBoundingRect(final String name) {
    final List<Polygon> polys = this.polys.get(name);
    if (polys == null) {
      throw new IllegalStateException("No polygons found for:" + name + " All territories:" + this.polys.keySet());
    }
    final Iterator<Polygon> polyIter = polys.iterator();
    final Rectangle bounds = polyIter.next().getBounds();
    while (polyIter.hasNext()) {
      bounds.add(polyIter.next().getBounds());
    }
    // if we have a territory that straddles the map divide, ie: which has polygons on both the left and right sides of
    // the map,
    // then the polygon's width or height could be almost equal to the map width or height
    // this can cause lots of problems, like when we want to get the tiles for the territory we would end up getting all
    // the tiles for the
    // map (and a java heap space error)
    final Dimension mapDimensions = getMapDimensions();
    if ((scrollWrapX() && bounds.width > 1800 && bounds.width > mapDimensions.width * 0.9)
        || (scrollWrapY() && bounds.height > 1200 && bounds.height > mapDimensions.height * 0.9)) {
      return getBoundingRectWithTranslate(polys, mapDimensions);
    }
    return bounds;
  }

  private Rectangle getBoundingRectWithTranslate(final List<Polygon> polys, final Dimension mapDimensions) {
    Rectangle boundingRect = null;
    final int mapWidth = mapDimensions.width;
    final int mapHeight = mapDimensions.height;
    final int closeToMapWidth = (int) (mapWidth * 0.9);
    final int closeToMapHeight = (int) (mapHeight * 0.9);
    final boolean scrollWrapX = this.scrollWrapX();
    final boolean scrollWrapY = this.scrollWrapY();
    for (final Polygon item : polys) {
      // if our rectangle is on the right side (mapscrollx) then we push it to be on the negative left side, so that the
      // bounds.x will be
      // negative
      // this solves the issue of maps that have a territory where polygons were on both sides of the map divide
      // (so our bounds.x was 0, and our bounds.y would be the entire map width)
      // (when in fact it should actually be bounds.x = -10 or something, and bounds.width = 20 or something)
      // we use map dimensions.width * 0.9 because the polygon may not actually touch the side of the map (like if the
      // territory borders are
      // thick)
      final Rectangle itemRect = item.getBounds();
      if (scrollWrapX && itemRect.getMaxX() >= closeToMapWidth) {
        itemRect.translate(-mapWidth, 0);
      }
      if (scrollWrapY && itemRect.getMaxY() >= closeToMapHeight) {
        itemRect.translate(0, -mapHeight);
      }
      if (boundingRect == null) {
        boundingRect = itemRect;
      } else {
        boundingRect.add(itemRect);
      }
    }
    // if the polygon is completely negative, we can make translate it back to normal
    if (boundingRect.x < 0 && boundingRect.getMaxX() <= 0) {
      boundingRect.translate(mapWidth, 0);
    }
    if (boundingRect.y < 0 && boundingRect.getMaxY() <= 0) {
      boundingRect.translate(0, mapHeight);
    }
    return boundingRect;
  }

  public Optional<Image> getVCImage() {
    return loadImage("misc/vc.png");
  }

  public Optional<Image> getBlockadeImage() {
    return loadImage("misc/blockade.png");
  }

  public Optional<Image> getErrorImage() {
    return loadImage("misc/error.gif");
  }

  public Optional<Image> getWarningImage() {
    return loadImage("misc/warning.gif");
  }

  public Optional<Image> getInfoImage() {
    return loadImage("misc/information.gif");
  }

  public Optional<Image> getHelpImage() {
    return loadImage("misc/help.gif");
  }

  private Optional<Image> loadImage(final String imageName) {
    final URL url = resourceLoader.getResource(imageName);
    if (url == null) {
      // this is actually pretty common that we try to read images that are not there. Let the caller
      // decide if this is an error or not.
      return Optional.empty();
    }
    try {
      return Optional.of(ImageIO.read(url));
    } catch (final IOException e) {
      ClientLogger.logQuietly(e);
      throw new IllegalStateException(e.getMessage());
    }
  }

  public Map<String, Image> getTerritoryNameImages() {
    return Collections.unmodifiableMap(territoryNameImages);
  }

  public Map<Image, List<Point>> getDecorations() {
    return Collections.unmodifiableMap(decorations);
  }

  public List<Point> getTerritoryEffectPoints(final Territory territory) {
    if (territoryEffects.get(territory.getName()) == null) {
      return Arrays.asList(getCenter(territory));
    }
    return territoryEffects.get(territory.getName());
  }

  public Optional<Image> getTerritoryEffectImage(final String effectName) {
    // TODO: what does this cache buy us? should we still keep it?
    if (effectImages.get(effectName) != null) {
      return Optional.of(effectImages.get(effectName));
    }
    final Optional<Image> effectImage = loadImage("territoryEffects/" + effectName + ".png");
    effectImages.put(effectName, effectImage.orElse(null));
    return effectImage;
  }
}
