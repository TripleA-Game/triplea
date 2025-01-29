package games.strategy.engine.data;

import games.strategy.triplea.Constants;
import java.util.Map.Entry;
import org.triplea.java.collections.IntegerMap;

/** A production rule. */
public class ProductionRule extends DefaultNamed implements Rule {
  private static final long serialVersionUID = -6598296283127741307L;

  protected IntegerMap<Resource> costs = new IntegerMap<>();
  protected IntegerMap<NamedAttachable> results = new IntegerMap<>();

  public ProductionRule(final String name, final GameData data) {
    super(name, data);
  }

  public ProductionRule(
      final String name,
      final GameData data,
      final IntegerMap<NamedAttachable> results,
      final IntegerMap<Resource> costs) {
    super(name, data);
    this.results = results;
    this.costs = costs;
  }

  public void addCost(final Resource resource, final int quantity) {
    costs.put(resource, quantity);
  }

  @Override
  public IntegerMap<Resource> getCosts() {
    return new IntegerMap<>(costs);
  }

  @Override
  public IntegerMap<NamedAttachable> getResults() {
    return results;
  }

  @Override
  public String toString() {
    return "ProductionRule: " + getName();
  }

  /**
   * Returns a string representing the total resource cost for this production rule.
   *
   * @return A string with the format {@code <resource1Cost> <resource1Name>[; <resource2Cost>
   *     <resource2Name>[; ...]]}.
   */
  public String toStringCosts() {
    final StringBuilder sb = new StringBuilder();
    final Resource pus;
    try (GameData.Unlocker ignored = getData().acquireReadLock()) {
      pus = getData().getResourceList().getResource(Constants.PUS);
    }
    if (costs.getInt(pus) != 0) {
      sb.append("; ");
      sb.append(costs.getInt(pus));
      sb.append(" ").append(pus.getName());
    }
    for (final Entry<Resource, Integer> entry : costs.entrySet()) {
      final Resource r = entry.getKey();
      if (r.equals(pus)) {
        continue;
      }
      final int c = entry.getValue();
      sb.append("; ");
      sb.append(c);
      sb.append(" ").append(r.getName());
    }
    return sb.toString().replaceFirst("; ", "");
  }
}
