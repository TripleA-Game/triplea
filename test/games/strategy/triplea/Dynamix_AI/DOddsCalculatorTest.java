package games.strategy.triplea.Dynamix_AI;

import java.util.ArrayList;
import java.util.List;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.ai.Dynamix_AI.DUtils;
import games.strategy.triplea.ai.Dynamix_AI.Dynamix_AI;
import games.strategy.triplea.oddsCalculator.ta.AggregateResults;
import games.strategy.triplea.oddsCalculator.ta.IOddsCalculator;
import games.strategy.triplea.xml.LoadGameUtil;
import junit.framework.TestCase;

public class DOddsCalculatorTest extends TestCase {
  private GameData m_data;
  private final IOddsCalculator m_calc = new Dynamix_AI("Superior", "Dynamix (AI)").getCalc();

  @Override
  protected void setUp() throws Exception {
    m_data = LoadGameUtil.loadGame("Great Lakes War Test", "Great Lakes War v1.4 test.xml");
  }

  @Override
  protected void tearDown() throws Exception {
    m_data = null;
  }

  public void testBattleCalculator() {
    final PlayerID superior = m_data.getPlayerList().getPlayerID("Superior");
    final PlayerID huron = m_data.getPlayerList().getPlayerID("Huron");
    final Territory cIsland = m_data.getMap().getTerritory("C");
    final UnitType infantry = m_data.getUnitTypeList().getUnitType("infantry");
    final UnitType artillery = m_data.getUnitTypeList().getUnitType("artillery");
    final UnitType fighter = m_data.getUnitTypeList().getUnitType("fighter");
    final List<Unit> attacking = new ArrayList<Unit>();
    final List<Unit> defending = new ArrayList<Unit>();
    for (int i = 0; i < 30; i++) {
      attacking.add(infantry.create(superior));
      attacking.add(artillery.create(superior));
      attacking.add(fighter.create(superior));
    }
    for (int i = 0; i < 60; i++) {
      defending.add(infantry.create(huron));
    }
    m_calc.setGameData(m_data);
    final AggregateResults results = DUtils.GetBattleResults(attacking, defending, cIsland, m_data, 150, true);
    System.out.print("Time Taken To Calculate: " + results.getTime() + "\r\n");
    assertEquals(1.0D, results.getAttackerWinPercent());
    assertEquals(0.0D, results.getAverageDefendingUnitsLeft());
    assertEquals(0.0D, results.getDefenderWinPercent());
  }
}
