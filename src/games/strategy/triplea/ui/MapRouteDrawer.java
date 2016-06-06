package games.strategy.triplea.ui;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import java.util.List;
import java.util.function.Function;

import org.apache.commons.math3.analysis.interpolation.SplineInterpolator;
import org.apache.commons.math3.analysis.polynomials.PolynomialSplineFunction;

import games.strategy.engine.data.Route;
import games.strategy.engine.data.Territory;

/**
 * Draws a route on a map.
 * Could be static, is non-static for JUnit/Mockito testing purposes
 */
public class MapRouteDrawer {

  private static final SplineInterpolator splineInterpolator = new SplineInterpolator();
  /**
   * This value influences the "resolution" of the Path.
   * Too low values make the Path look edgy, too high values will cause lag and rendering errors
   * because the distance between the drawing segments is shorter than 2 pixels 
   */
  public static final double DETAIL_LEVEL = 1.0;

  /**
   * Draws the route to the screen.
   */
  public void drawRoute(final Graphics2D graphics, final RouteDescription routeDescription, final MapPanel view,
      final MapData mapData, final String maxMovement) {
    if (routeDescription == null) {
      return;
    }
    final Route route = routeDescription.getRoute();
    if (route == null) {
      return;
    }
    // set thickness and color of the future drawings
    graphics.setStroke(new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    graphics.setPaint(Color.red);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    
    final int numTerritories = route.getAllTerritories().size();
    final Point[] points = getRoutePoints(routeDescription, mapData);
    final boolean tooFewTerritories = numTerritories <= 1;
    final boolean tooFewPoints = points.length <= 2;
    final int xOffset = view.getXOffset();
    final int yOffset = view.getYOffset();
    final double scale = view.getScale();
    final int jointsize = 10;
    if (tooFewTerritories || tooFewPoints) {
      drawDirectPath(graphics, routeDescription, xOffset, yOffset, jointsize, scale);
      if(tooFewPoints && !tooFewTerritories){
        drawMoveLength(graphics, points, xOffset, yOffset, scale, numTerritories, maxMovement);
      }
    } else {
      drawCurvedPath(graphics, points, xOffset, yOffset, scale);
      drawMoveLength(graphics, points, xOffset, yOffset, scale, numTerritories, maxMovement);
    }
    drawJoints(graphics, points, xOffset, yOffset, jointsize, scale);
    drawCustomCursor(graphics, routeDescription, xOffset, yOffset, scale);
  }
  
  /**
   * Draws Points on the Map
   * 
   * @param graphics The Graphics2D Object being drawn on
   * @param points The Point array aka the "Joints" to be drawn
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param jointsize The diameter of the Points being drawn
   * @param scale The scale-factor of the Map
   */
  private void drawJoints(Graphics2D graphics, Point[] points, int xOffset, int yOffset, int jointsize,
      double scale) {
    for (Point p : points) {
      graphics.fillOval((int) (((p.x - xOffset) - jointsize / 2) * scale),
          (int) (((p.y - yOffset) - jointsize / 2) * scale), jointsize, jointsize);
    }

  }
  
  /**
   * Draws a specified CursorImage if available
   * 
   * @param graphics The graphics2D Object being drawn on
   * @param routeDescription The RouteDescription object containing the CursorImage
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawCustomCursor(Graphics2D graphics, RouteDescription routeDescription, int xOffset, int yOffset, double scale){
    final Image cursorImage = routeDescription.getCursorImage();
    if (cursorImage != null) {
      graphics.drawImage(cursorImage,
          (int) (((routeDescription.getEnd().x - xOffset) - (cursorImage.getWidth(null) / 2)) * scale),
          (int) (((routeDescription.getEnd().y - yOffset) - (cursorImage.getHeight(null) / 2)) * scale), null);
    }
    
  }
  
  /**
   * Draws a straight Line from the start to the stop of the specified <code>RouteDescription</code>
   * Also draws a small little point at the end of the Line.
   * 
   * @param graphics The graphics2D Object being drawn on
   * @param routeDescription The RouteDescription object containing the CursorImage
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param jointsize The diameter of the Points being drawn
   * @param scale The scale-factor of the Map
   */
  private void drawDirectPath(Graphics2D graphics, RouteDescription routeDescription, int xOffset, int yOffset, int jointsize, double scale){
    drawLineWithTranslate(graphics, new Line2D.Float(routeDescription.getStart(), routeDescription.getEnd()), xOffset,
        yOffset, scale);
    graphics.fillOval(
        (int) (((routeDescription.getEnd().x - xOffset) - jointsize / 2) * scale),
        (int) (((routeDescription.getEnd().y - yOffset) - jointsize / 2) * scale),
        jointsize, jointsize);
  }
  /**
   * Centripetal parameterization
   * 
   * Check http://stackoverflow.com/a/37370620/5769952 for more information
   * 
   * @param points - The Points which should be parameterized
   * @return A Parameter-Array called the "Index"
   */
  protected double[] createParameterizedIndex(Point[] points) {
    final double[] index = new double[points.length];
    if(index.length > 0){
      index[0] = 0;
    }
    for (int i = 1; i < points.length; i++) {
      index[i] = index[i - 1] + Math.sqrt(points[i - 1].distance(points[i]));
    }
    return index;
  }
  
  /**
   * Draws a line to the Screen regarding the Map-Offset and scale
   * 
   * @param graphics The Graphics2D Object to be drawn on
   * @param line The Line to be drawn
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawLineWithTranslate(Graphics2D graphics, Line2D line, double xOffset, double yOffset,
      double scale) {
    final Point2D point1 =
        new Point2D.Double((line.getP1().getX() - xOffset) * scale, (line.getP1().getY() - yOffset) * scale);
    final Point2D point2 =
        new Point2D.Double((line.getP2().getX() - xOffset) * scale, (line.getP2().getY() - yOffset) * scale);
    //Don't draw if won't be visible anyway
    if(graphics.getClip().contains(point1) || graphics.getClip().contains(point2)){
      graphics.draw(new Line2D.Double(point1, point2));
    }
  }
  
  /**
   * Creates a Point Array out of a <code>RouteDescription</code> and a <code>MapData</code> object
   * 
   * @param routeDescription <code>RouteDescription</code> containing the Route information
   * @param mapData <code>MapData</code> Object containing Information about the Map Coordinates
   * @return The Point array specified by the <code>RouteDescription</code> and <code>MapData</code> objects
   */
  protected Point[] getRoutePoints(RouteDescription routeDescription, MapData mapData) {
    final List<Territory> territories = routeDescription.getRoute().getAllTerritories();
    final int numTerritories = territories.size();
    final Point[] points = new Point[numTerritories];
    for (int i = 0; i < numTerritories; i++) {
      points[i] = mapData.getCenter(territories.get(i));
    }
    if (routeDescription.getStart() != null) {
      points[0] = routeDescription.getStart();
    }
    if (routeDescription.getEnd() != null && numTerritories > 1) {
      points[numTerritories - 1] = new Point(routeDescription.getEnd());
    }
    return points;
  }
  
  /**
   * Creates double arrays of y or x coordinates of the given Point Array
   * 
   * @param points The Point Array containing the Coordinates
   * @param extractor A function specifying which value to return
   * @return A double array with values specified by the given function
   */
  protected double[] getValues(Point[] points, Function<Point, Double> extractor){
    double[] result = new double[points.length];
    for (int i = 0; i < points.length; i++) {
      result[i] = extractor.apply(points[i]);
    }
    return result;
    
  }

  /**
   * Creates a double array containing y coordinates of a <code>PolynomialSplineFunction</code> with the above specified <code>DETAIL_LEVEL</code>
   * 
   * @param fuction The <code>PolynomialSplineFunction</code> with the values
   * @param index the parameterized array to indicate the maximum Values 
   * @return
   */
  protected double[] getCoords(PolynomialSplineFunction fuction, double[] index) {
    final double defaultCoordSize = index[index.length - 1];
    final double[] coords = new double[(int)Math.round(DETAIL_LEVEL * defaultCoordSize) + 1];
    final double stepSize = fuction.getKnots()[fuction.getKnots().length - 1] / coords.length;
    double curValue = 0;
    for (int i = 0; i < coords.length; i ++) {
      coords[i] = fuction.value(curValue);
      curValue += stepSize;
    }
    return coords;
  }

  /**
   * Draws how many moves are left
   * 
   * @param graphics The Graphics2D Object to be drawn on
   * @param points The Point array of the unit's tour
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   * @param numTerritories how many Territories the unit traveled so far
   * @param maxMovement The String indicating how man
   */
  private void drawMoveLength(Graphics2D graphics, Point[] points,
      int xOffset, int yOffset, double scale, int numTerritories,
      String maxMovement) {
    final Point cursorPos = points[points.length - 1];
    final String unitMovementLeft =
        maxMovement == null || maxMovement.trim().length() == 0 ? ""
            : "    /" + maxMovement;
    final BufferedImage movementImage = new BufferedImage(50, 20, BufferedImage.TYPE_INT_ARGB);
    createMovementLeftImage(movementImage, String.valueOf(numTerritories - 1), unitMovementLeft);
    
    final int textXOffset = -movementImage.getWidth() / 2;
    final int yDir = cursorPos.y - points[numTerritories - 2].y;
    final int textYOffset = yDir > 0 ? movementImage.getHeight() : movementImage.getHeight() * -2;
    graphics.drawImage(movementImage,
        (int) ((cursorPos.x + textXOffset - xOffset) * scale),
        (int) ((cursorPos.y + textYOffset - yOffset) * scale), null);
  }
  
  /**
   * Draws a smooth curve through the given array of points
   * This algorithm is called Spline-Interpolation
   * because the Apache-commons-math library we are using here does not accept
   * values but <code>f(x)=y</code> with x having to increase all the time
   * the idea behind this is to use a parameter array - the so called index
   * as x array and splitting the points into a x and y coordinates array.
   * 
   * Finally those 2 interpolated arrays get unified into a single point array and drawn to the Map
   * 
   * @param graphics The Graphics2D Object to be drawn on
   * @param points The Knot Points for the Spline-Interpolator aka the joints
   * @param xOffset The horizontal pixel-difference between the frame and the Map
   * @param yOffset The vertical pixel-difference between the frame and the Map
   * @param scale The scale-factor of the Map
   */
  private void drawCurvedPath(Graphics2D graphics, Point[] points, int xOffset, int yOffset, double scale) {
    final double[] index = createParameterizedIndex(points);
    final PolynomialSplineFunction xcurve = splineInterpolator.interpolate(index, getValues(points, point -> point.getX()));
    final double[] xcoords = getCoords(xcurve, index);
    final PolynomialSplineFunction ycurve = splineInterpolator.interpolate(index, getValues(points, point -> point.getY()));
    final double[] ycoords = getCoords(ycurve, index);

    for (int i = 1; i < xcoords.length; i++) {
      drawLineWithTranslate(graphics, new Line2D.Double(xcoords[i - 1], ycoords[i - 1], xcoords[i], ycoords[i]),
          xOffset, yOffset, scale);
    }
    // draws the Line to the Cursor, so that the line ends at the cursor no matter what...
    drawLineWithTranslate(graphics,
        new Line2D.Double(new Point2D.Double(xcoords[xcoords.length - 1], ycoords[ycoords.length - 1]),
            points[points.length - 1]),
        xOffset, yOffset, scale);
  }

  /**
   * This draws how many moves are left on the given <code>BufferedImage</code>
   * 
   * @param image The Image to be drawn on
   * @param curMovement How many territories the unit traveled so far
   * @param maxMovement How many territories is allowed to travel. Is empty when the unit traveled too far
   */
  private void createMovementLeftImage(BufferedImage image, String curMovement, String maxMovement) {
    final Graphics2D textG2D = image.createGraphics();
    textG2D.setColor(Color.YELLOW);
    textG2D.setFont(new Font("Dialog", Font.BOLD, 20));
    final int textThicknessOffset = textG2D.getFontMetrics().stringWidth(curMovement) / 2;
    final boolean distanceTooBig = maxMovement.equals("");
    textG2D.drawString(curMovement, distanceTooBig ? image.getWidth() / 2 - textThicknessOffset : 0, image.getHeight());
    if(!distanceTooBig){
      textG2D.setColor(new Color(33, 0, 127));
      textG2D.setFont(new Font("Dialog", Font.BOLD, 16));
      textG2D.drawString(maxMovement, 0, image.getHeight());
    }
  }
}
