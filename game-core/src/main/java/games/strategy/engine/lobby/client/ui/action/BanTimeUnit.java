package games.strategy.engine.lobby.client.ui.action;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import lombok.AllArgsConstructor;

/** The possible time units and corresponding mappings. */
@AllArgsConstructor
@SuppressWarnings("ImmutableEnumChecker")
enum BanTimeUnit {
  MINUTES("Minutes", minutes -> (long) minutes),

  HOURS("Hours", TimeUnit.HOURS::toMinutes),

  DAYS("Days", TimeUnit.DAYS::toMinutes),

  WEEKS("Weeks", durationUnit -> TimeUnit.DAYS.toMinutes(durationUnit * 7)),

  MONTHS("Weeks", durationUnit -> TimeUnit.DAYS.toMinutes(durationUnit * 30)),

  FOREVER("Forever", value -> (long) BanDurationDialog.MAX_DURATION);

  private final String displayName;

  private final Function<Integer, Long> toMinuteFunction;

  @Override
  public String toString() {
    return displayName;
  }

  long toMinutes(final int value) {
    return toMinuteFunction.apply(value);
  }
}
