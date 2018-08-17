package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.Constants;
import games.strategy.triplea.MapSupport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.util.CollectionUtils;
import games.strategy.util.Tuple;

/**
 * A class of attachments that can be "activated" during a user action delegate.
 * For now they will just be conditions that can then fire triggers.
 */
@MapSupport
public class UserActionAttachment extends AbstractUserActionAttachment {
  private static final long serialVersionUID = 5268397563276055355L;

  private List<Tuple<String, String>> m_activateTrigger = new ArrayList<>();


  public UserActionAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  public static Collection<UserActionAttachment> getUserActionAttachments(final PlayerID player) {
    return player.getAttachments().values().stream()
        .filter(a -> a.getName().startsWith(Constants.USERACTION_ATTACHMENT_PREFIX))
        .filter(UserActionAttachment.class::isInstance)
        .map(UserActionAttachment.class::cast)
        .collect(Collectors.toList());
  }

  static UserActionAttachment get(final PlayerID player, final String nameOfAttachment) {
    return getAttachment(player, nameOfAttachment, UserActionAttachment.class);
  }

  private void setActivateTrigger(final String value) throws GameParseException {
    // triggerName:numberOfTimes:useUses:testUses:testConditions:testChance
    final String[] s = splitOnColon(value);
    if (s.length != 6) {
      throw new GameParseException(
          "activateTrigger must have 6 parts: triggerName:numberOfTimes:useUses:testUses:testConditions:testChance"
              + thisErrorMsg());
    }
    TriggerAttachment trigger = null;
    for (final PlayerID player : getData().getPlayerList().getPlayers()) {
      for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, null)) {
        if (ta.getName().equals(s[0])) {
          trigger = ta;
          break;
        }
      }
      if (trigger != null) {
        break;
      }
    }
    if (trigger == null) {
      throw new GameParseException("No TriggerAttachment named: " + s[0] + thisErrorMsg());
    }
    String options = value;
    options = options.replaceFirst((s[0] + ":"), "");
    final int numberOfTimes = getInt(s[1]);
    if (numberOfTimes < 0) {
      throw new GameParseException(
          "activateTrigger must be positive for the number of times to fire: " + s[1] + thisErrorMsg());
    }
    getBool(s[2]);
    getBool(s[3]);
    getBool(s[4]);
    getBool(s[5]);
    m_activateTrigger.add(Tuple.of(s[0], options));
  }

  private void setActivateTrigger(final List<Tuple<String, String>> value) {
    m_activateTrigger = value;
  }

  private List<Tuple<String, String>> getActivateTrigger() {
    return m_activateTrigger;
  }

  private void resetActivateTrigger() {
    m_activateTrigger = new ArrayList<>();
  }

  public static void fireTriggers(final UserActionAttachment actionAttachment,
      final HashMap<ICondition, Boolean> testedConditionsSoFar, final IDelegateBridge bridge) {
    final GameData data = bridge.getData();
    for (final Tuple<String, String> tuple : actionAttachment.getActivateTrigger()) {
      // numberOfTimes:useUses:testUses:testConditions:testChance
      TriggerAttachment toFire = null;
      for (final PlayerID player : data.getPlayerList().getPlayers()) {
        for (final TriggerAttachment ta : TriggerAttachment.getTriggers(player, null)) {
          if (ta.getName().equals(tuple.getFirst())) {
            toFire = ta;
            break;
          }
        }
        if (toFire != null) {
          break;
        }
      }
      final HashSet<TriggerAttachment> toFireSet = new HashSet<>();
      toFireSet.add(toFire);
      final String[] options = splitOnColon(tuple.getSecond());
      final int numberOfTimesToFire = getInt(options[0]);
      final boolean useUsesToFire = getBool(options[1]);
      final boolean testUsesToFire = getBool(options[2]);
      final boolean testConditionsToFire = getBool(options[3]);
      final boolean testChanceToFire = getBool(options[4]);
      if (testConditionsToFire) {
        if (!testedConditionsSoFar.containsKey(toFire)) {
          // this should directly add the new tests to testConditionsToFire...
          TriggerAttachment.collectTestsForAllTriggers(toFireSet, bridge,
              new HashSet<>(testedConditionsSoFar.keySet()), testedConditionsSoFar);
        }
        if (!AbstractTriggerAttachment.isSatisfiedMatch(testedConditionsSoFar).test(toFire)) {
          continue;
        }
      }
      for (int i = 0; i < numberOfTimesToFire; ++i) {
        bridge.getHistoryWriter().startEvent(MyFormatter.attachmentNameToText(actionAttachment.getName())
            + " activates a trigger called: " + MyFormatter.attachmentNameToText(toFire.getName()));
        TriggerAttachment.fireTriggers(toFireSet, testedConditionsSoFar, bridge, null, null, useUsesToFire,
            testUsesToFire, testChanceToFire, false);
      }
    }
  }

  public Set<PlayerID> getOtherPlayers() {
    final HashSet<PlayerID> otherPlayers = new HashSet<>();
    otherPlayers.add((PlayerID) this.getAttachedTo());
    otherPlayers.addAll(m_actionAccept);
    return otherPlayers;
  }

  /**
   * Returns the valid actions for this player.
   */
  public static Collection<UserActionAttachment> getValidActions(final PlayerID player,
      final HashMap<ICondition, Boolean> testedConditions) {
    return CollectionUtils.getMatches(getUserActionAttachments(player),
        Matches.abstractUserActionAttachmentCanBeAttempted(testedConditions));
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("activateTrigger",
            MutableProperty.of(
                this::setActivateTrigger,
                this::setActivateTrigger,
                this::getActivateTrigger,
                this::resetActivateTrigger))
        .build();
  }
}
