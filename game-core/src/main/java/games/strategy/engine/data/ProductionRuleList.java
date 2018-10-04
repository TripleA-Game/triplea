package games.strategy.engine.data;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import com.google.common.annotations.VisibleForTesting;

/**
 * A collection of {@link ProductionRule}s keyed on the production rule name.
 */
public class ProductionRuleList extends GameDataComponent {
  private static final long serialVersionUID = -5313215563006788188L;

  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  private final Map<String, ProductionRule> m_productionRules = new HashMap<>();

  public ProductionRuleList(final GameData data) {
    super(data);
  }

  @VisibleForTesting
  public void addProductionRule(final ProductionRule pf) {
    m_productionRules.put(pf.getName(), pf);
  }

  public int size() {
    return m_productionRules.size();
  }

  public ProductionRule getProductionRule(final String name) {
    return m_productionRules.get(name);
  }

  public Collection<ProductionRule> getProductionRules() {
    return m_productionRules.values();
  }
}
