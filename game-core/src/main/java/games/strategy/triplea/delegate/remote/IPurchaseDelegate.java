package games.strategy.triplea.delegate.remote;

import games.strategy.engine.data.ProductionRule;
import games.strategy.engine.data.RepairRule;
import games.strategy.engine.data.Unit;
import java.util.Map;
import org.triplea.java.collections.IntegerMap;

/** Logic for purchasing and repairing units. */
public interface IPurchaseDelegate extends IAbstractForumPosterDelegate {
  /**
   * Purchases the specified units.
   *
   * @param productionRules - units maps ProductionRule -> count.
   * @return null if units bought, otherwise an error message
   */
  String purchase(IntegerMap<ProductionRule> productionRules);

  /** Returns an error code, or null if all is good. */
  String purchaseRepair(Map<Unit, IntegerMap<RepairRule>> productionRules);
}
