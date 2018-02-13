package games.strategy.util;

import static com.google.common.base.Preconditions.checkNotNull;

import java.util.Optional;

import javax.annotation.Nullable;
import javax.annotation.concurrent.Immutable;

/**
 * A collection of methods that assist working with operations that may be interrupted but it is typically awkward to
 * deal with {@link InterruptedException} in the calling context.
 */
public final class Interruptibles {
  private Interruptibles() {}

  /**
   * Executes and awaits the completion of the specified operation that produces no result. If the current thread is
   * interrupted before the operation completes, the thread will be re-interrupted, and this method will return
   * {@code true}. This method re-throws any unchecked exception thrown by {@code runnable}.
   *
   * @param runnable The operation to execute and await.
   *
   * @return {@code true} if the current thread was interrupted while waiting for the operation to complete; otherwise
   *         {@code false}.
   */
  public static boolean await(final InterruptibleRunnable runnable) {
    checkNotNull(runnable);

    return awaitResult(() -> {
      runnable.run();
      return null;
    }).interrupted;
  }

  /**
   * Executes and awaits the completion of the specified operation that produces a result. If the current thread is
   * interrupted before the operation completes, the thread will be re-interrupted, and this method will return a
   * {@link Result} whose {@code interrupted} field is {@code true}. This method re-throws any unchecked exception
   * thrown by {@code supplier}.
   *
   * @param supplier The operation to execute and await.
   *
   * @return If the operation completed without interruption, {@code interrupted} will be {@code false} and
   *         {@code result} will contain the operation's result (a {@code null} result is modeled as an empty result);
   *         if the operation was interrupted, {@code interrupted} will be {@code true} and {@code result} will be
   *         empty.
   */
  public static <T> Result<T> awaitResult(final InterruptibleSupplier<T> supplier) {
    checkNotNull(supplier);

    try {
      return new Result<>(Optional.ofNullable(supplier.get()), false);
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return new Result<>(Optional.empty(), true);
    }
  }

  /**
   * An interruptible action that does not supply a result.
   */
  @FunctionalInterface
  public interface InterruptibleRunnable {
    /**
     * Invokes the action.
     *
     * @throws InterruptedException If the current thread is interrupted while waiting for the action to complete.
     */
    void run() throws InterruptedException;
  }

  /**
   * An interruptible supplier of results.
   *
   * @param <T> The type of the result.
   */
  @FunctionalInterface
  public interface InterruptibleSupplier<T> {
    /**
     * Gets the result.
     *
     * @return The result.
     *
     * @throws InterruptedException If the current thread is interrupted while waiting for the supplier to complete.
     */
    @Nullable
    T get() throws InterruptedException;
  }

  /**
   * The result of an interruptible operation that returns a result.
   *
   * @param <T> The result of the operation.
   */
  @Immutable
  public static final class Result<T> {
    /**
     * Indicates the operation was interrupted; {@code result} is meaningless.
     */
    public final boolean interrupted;

    /**
     * The result of the operation or empty if the operation did not supply a result.
     */
    public final Optional<T> result;

    private Result(final Optional<T> result, final boolean interrupted) {
      this.interrupted = interrupted;
      this.result = result;
    }
  }
}
