package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Arrays;
import java.util.Collection;
import java.util.function.Predicate;

/**
 * A utility for seeing which elements in a collection satisfy a given condition.
 *
 * <p>
 * An instance of match allows you to test that an object matches some condition.
 * </p>
 *
 * <p>
 * Static utility methods allow you to find what elements in a collection satisfy a match,
 * count the number of matches, see if any elements match etc.
 * </p>
 *
 * <p>
 * Instances of this class are immutable.
 * </p>
 *
 * @param <T> The type of object that is tested by the match condition.
 */
public final class Match<T> implements Predicate<T> {
  private final Predicate<T> condition;

  private Match(final Predicate<T> condition) {
    this.condition = condition;
  }

  /**
   * Returns true if the object matches some condition.
   */
  @Override
  public boolean test(final T value) {
    return condition.test(value);
  }

  /**
   * Creates a new match for the specified condition.
   *
   * @param condition The match condition; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Match<T> of(final Predicate<T> condition) {
    checkNotNull(condition);

    return new Match<>(condition);
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches all of the specified conditions.
   *
   * @param matches An array of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  @SafeVarargs
  @SuppressWarnings("varargs")
  public static <T> Predicate<T> allOf(final Predicate<T>... matches) {
    checkNotNull(matches);

    return allOf(Arrays.asList(matches));
  }

  /**
   * Creates a new match whose condition is satisfied if the test object matches all of the specified conditions.
   *
   * @param matches A collection of matches; must not be {@code null}.
   *
   * @return A new match; never {@code null}.
   */
  public static <T> Predicate<T> allOf(final Collection<Predicate<T>> matches) {
    checkNotNull(matches);

    return Match.of(value -> matches.stream().allMatch(match -> match.test(value)));
  }
}
