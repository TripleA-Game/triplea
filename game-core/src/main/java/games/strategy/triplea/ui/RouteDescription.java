package games.strategy.triplea.ui;

import java.awt.Image;
import java.awt.Point;
import java.util.Objects;

import games.strategy.engine.data.Route;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
class RouteDescription {
  private final Route route;
  // this point is in map co-ordinates, un scaled
  private final Point start;
  // this point is in map co-ordinates, un scaled
  private final Point end;
  private final Image cursorImage;

  @Override
  public int hashCode() {
    return Objects.hash(route, cursorImage);
  }

  @Override
  public boolean equals(final Object o) {
    if (o == this) {
      return true;
    } else if (!(o instanceof RouteDescription)) {
      return false;
    }

    final RouteDescription other = (RouteDescription) o;
    if ((start == null && other.start != null) || (other.start == null && start != null)
        || (start != other.start && !start.equals(other.start))) {
      return false;
    }
    if ((route == null && other.route != null) || (other.route == null && route != null)
        || (route != other.route && !route.equals(other.route))) {
      return false;
    }
    if ((end == null && other.end != null) || (other.end == null && end != null)) {
      return false;
    }
    if (cursorImage != other.cursorImage) {
      return false;
    }
    // we dont want to be updating for every small change,
    // if the end points are close enough, they are close enough
    if (other.end == null) {
      return true;
    }
    int diffX = end.x - other.end.x;
    diffX *= diffX;
    int diffY = end.y - other.end.y;
    diffY *= diffY;
    final int endDiff = (int) Math.sqrt(diffX + diffY);
    return endDiff < 6;
  }
}
