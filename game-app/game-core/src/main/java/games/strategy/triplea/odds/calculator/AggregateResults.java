package games.strategy.triplea.odds.calculator;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.triplea.delegate.battle.BattleResults;
import games.strategy.triplea.util.TuvUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.math3.stat.descriptive.moment.Mean;
import org.triplea.java.collections.IntegerMap;
import org.triplea.util.Tuple;

/** A container for the results of multiple battle simulation runs. */
public class AggregateResults {
  private final List<BattleResults> results;
  @Getter @Setter private long time;

  public AggregateResults(final int expectedCount) {
    results = new ArrayList<>(expectedCount);
  }

  public AggregateResults(final List<BattleResults> results) {
    this.results = new ArrayList<>(results);
  }

  public void addResult(final BattleResults result) {
    results.add(result);
  }

  public void addResults(final Collection<BattleResults> results) {
    this.results.addAll(results);
  }

  public List<BattleResults> getResults() {
    return results;
  }

  private Optional<BattleResults> getBattleResultsClosestToAverage() {
    return results.stream()
        .min(
            Comparator.comparingDouble(
                result ->
                    Math.abs(
                            result.getRemainingAttackingUnits().size()
                                - getAverageAttackingUnitsLeft())
                        + Math.abs(
                            result.getRemainingDefendingUnits().size()
                                - getAverageDefendingUnitsLeft())));
  }

  public Collection<Unit> getAverageAttackingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingAttackingUnits)
        .orElseGet(ArrayList::new);
  }

  public Collection<Unit> getAverageDefendingUnitsRemaining() {
    return getBattleResultsClosestToAverage()
        .map(BattleResults::getRemainingDefendingUnits)
        .orElseGet(ArrayList::new);
  }

  /**
   * First is Attacker, Second is Defender.
   *
   * <p>If no battle results were added to this aggregator instance, {@code (NaN, NaN)} is returned.
   */
  public Tuple<Double, Double> getAverageTuvOfUnitsLeftOver(
      final IntegerMap<UnitType> attackerCostsForTuv,
      final IntegerMap<UnitType> defenderCostsForTuv) {
    final Mean attackerTuvMean = new Mean();
    final Mean defenderTuvMean = new Mean();
    for (final BattleResults result : results) {
      attackerTuvMean.increment(
          TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv));
      defenderTuvMean.increment(
          TuvUtils.getTuv(result.getRemainingDefendingUnits(), defenderCostsForTuv));
    }
    return Tuple.of(attackerTuvMean.getResult(), defenderTuvMean.getResult());
  }

  /**
   * Returns the average TUV swing across all simulations of the battle.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   *
   * @return A positive value indicates the defender lost more unit value, on average, than the
   *     attacker (i.e. the attacker "won"). A negative value indicates the attacker lost more unit
   *     value, on average, than the defender (i.e. the defender "won"). Zero indicates the attacker
   *     and defender lost, on average, equal unit value (i.e. a tie).
   */
  public double getAverageTuvSwing(
      final GamePlayer attacker,
      final Collection<Unit> attackers,
      final GamePlayer defender,
      final Collection<Unit> defenders,
      final GameData data) {
    // The TUV swing is defenderTuvLost - attackerTuvLost and tuvLost = startingTuv - remainingTuv.
    // Thus, the TUV swing of a singe battle is:
    // TUV swing = defenderStartingTuv - attackerStartingTuv - defenderRemainingTuv +
    // attackerRemainingTuv
    //
    // Because mean(x_i+c) = mean(x_i)+c for a constant c - the startingTuv in this case - we save
    // some computations and add the startingTuv after we have calculated the mean.
    final IntegerMap<UnitType> attackerCostsForTuv = TuvUtils.getCostsForTuv(attacker, data);
    final IntegerMap<UnitType> defenderCostsForTuv = TuvUtils.getCostsForTuv(defender, data);
    final int attackerStartingTuv = TuvUtils.getTuv(attackers, attackerCostsForTuv);
    final int defenderStartingTuv = TuvUtils.getTuv(defenders, defenderCostsForTuv);
    final Mean mean = new Mean();
    return defenderStartingTuv
        - attackerStartingTuv
        + mean.evaluate(
            results.stream()
                .mapToDouble(
                    result ->
                        TuvUtils.getTuv(result.getRemainingAttackingUnits(), attackerCostsForTuv)
                            - TuvUtils.getTuv(
                                result.getRemainingDefendingUnits(), defenderCostsForTuv))
                .toArray());
  }

  /** If no battle results were added to this aggregator instance, {@code NaN} is returned. */
  public double getAverageAttackingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * If no battle results were added to this aggregator instance or if the attacker did not win any
   * of those battles, then {@code NaN} is returned.
   */
  public double getAverageAttackingUnitsLeftWhenAttackerWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::attackerWon)
            .map(BattleResults::getRemainingAttackingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /** If no battle results were added to this aggregator instance, {@code NaN} is returned. */
  public double getAverageDefendingUnitsLeft() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /**
   * If no battle results were added to this aggregator instance or if the defender did not win any
   * of those battles, then {@code NaN} is returned.
   */
  public double getAverageDefendingUnitsLeftWhenDefenderWon() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream()
            .filter(BattleResults::defenderWon)
            .map(BattleResults::getRemainingDefendingUnits)
            .mapToDouble(Collection::size)
            .toArray());
  }

  /** If no battle results were added to this aggregator instance, {@code NaN} is returned. */
  public double getAttackerWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.attackerWon() ? 1 : 0).toArray());
  }

  /** If no battle results were added to this aggregator instance, {@code NaN} is returned. */
  public double getDefenderWinPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(result -> result.defenderWon() ? 1 : 0).toArray());
  }

  /** If no battle results were added to this aggregator instance, {@code NaN} is returned. */
  public double getDrawPercent() {
    final Mean mean = new Mean();
    return mean.evaluate(results.stream().mapToDouble(result -> result.draw() ? 1 : 0).toArray());
  }

  /**
   * Returns the average number of rounds fought across all simulations of the battle.
   *
   * <p>If no battle results were added to this aggregator instance, {@code NaN} is returned.
   */
  public double getAverageBattleRoundsFought() {
    final Mean mean = new Mean();
    return mean.evaluate(
        results.stream().mapToDouble(BattleResults::getBattleRoundsFought).toArray());
  }

  public int getRollCount() {
    return results.size();
  }
}
