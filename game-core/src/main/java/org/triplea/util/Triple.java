package org.triplea.util;

import com.google.common.base.MoreObjects;
import java.io.Serializable;
import lombok.EqualsAndHashCode;

/**
 * A heterogeneous container of three values.
 *
 * @param <F> The type of the first value.
 * @param <S> The type of the second value.
 * @param <T> The type of the third value.
 */
@EqualsAndHashCode
public final class Triple<F, S, T> implements Serializable {
  private static final long serialVersionUID = -8188046743232005918L;
  private final Tuple<F, S> tuple;
  private final T third;

  private Triple(final F first, final S second, final T third) {
    tuple = Tuple.of(first, second);
    this.third = third;
  }

  /**
   * Static creation method to create a new instance of a triple with the parameters provided.
   *
   * <p>This method allows for nicer triple creation syntax, namely:
   *
   * <pre>
   * Triple&lt;String, Integer, String> myTriple = Triple.of("abc", 123, "xyz");
   * </pre>
   *
   * <p>Instead of:
   *
   * <pre>
   * Triple&lt;String, Integer, String> myTriple =
   *     new Triple&lt;String, Integer, String>("abc", 123, "xyz");
   * </pre>
   */
  public static <F, S, T> Triple<F, S, T> of(final F first, final S second, final T third) {
    return new Triple<>(first, second, third);
  }

  public F getFirst() {
    return tuple.getFirst();
  }

  public S getSecond() {
    return tuple.getSecond();
  }

  public T getThird() {
    return third;
  }

  @Override
  public String toString() {
    return MoreObjects.toStringHelper(this)
        .add("first", getFirst())
        .add("second", getSecond())
        .add("third", getThird())
        .toString();
  }
}
