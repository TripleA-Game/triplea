package games.strategy.triplea.attachments;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.ImmutableMap;

import games.strategy.engine.data.Attachable;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GameMap;
import games.strategy.engine.data.GameParseException;
import games.strategy.engine.data.MutableProperty;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.PlayerList;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.annotations.InternalDoNotExport;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.OriginalOwnerTracker;
import games.strategy.util.CollectionUtils;

/**
 * The Purpose of this class is to hold shared and simple methods used by RulesAttachment.
 */
public abstract class AbstractRulesAttachment extends AbstractConditionsAttachment {
  private static final long serialVersionUID = -6977650137928964759L;

  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment). Determines if we will be counting each for the
  // purposes of m_objectiveValue
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected boolean m_countEach = false;
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment). The multiple that will be applied to m_objectiveValue
  // if m_countEach is true
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected int m_eachMultiple = 1;
  @InternalDoNotExport
  // Do Not Export (do not include in IAttachment). Used with the next Territory conditions to
  // determine the number of territories needed to be valid (ex: m_alliedOwnershipTerritories)
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected int m_territoryCount = -1;
  // A list of players that can be used with
  // directOwnershipTerritories, directExclusionTerritories,
  // directPresenceTerritories, or any of the other territory lists
  // only used if the attachment begins with "objectiveAttachment"
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected List<PlayerID> m_players = new ArrayList<>();
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected int m_objectiveValue = 0;
  // only matters for objectiveValue, does not affect the condition
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected int m_uses = -1;
  // condition for what turn it is
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected Map<Integer, Integer> m_turns = null;
  // for on/off conditions
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected boolean m_switch = true;
  // allows custom GameProperties
  @SuppressWarnings("checkstyle:MemberName") // rename upon next incompatible release
  protected String m_gameProperty = null;

  protected AbstractRulesAttachment(final String name, final Attachable attachable, final GameData gameData) {
    super(name, attachable, gameData);
  }

  private void setPlayers(final String names) throws GameParseException {
    final PlayerList pl = getData().getPlayerList();
    for (final String p : splitOnColon(names)) {
      final PlayerID player = pl.getPlayerId(p);
      if (player == null) {
        throw new GameParseException("Could not find player. name:" + p + thisErrorMsg());
      }
      m_players.add(player);
    }
  }

  private void setPlayers(final List<PlayerID> value) {
    m_players = value;
  }

  protected List<PlayerID> getPlayers() {
    return m_players.isEmpty() ? new ArrayList<>(Collections.singletonList((PlayerID) getAttachedTo())) : m_players;
  }

  private void resetPlayers() {
    m_players = new ArrayList<>();
  }

  @Override
  protected void setChance(final String chance) throws GameParseException {
    throw new GameParseException(
        "chance not allowed for use with RulesAttachments, instead use it with Triggers or PoliticalActions"
            + thisErrorMsg());
  }

  private void setObjectiveValue(final String value) {
    m_objectiveValue = getInt(value);
  }

  private void setObjectiveValue(final int value) {
    m_objectiveValue = value;
  }

  public int getObjectiveValue() {
    return m_objectiveValue;
  }

  private void resetObjectiveValue() {
    m_objectiveValue = 0;
  }

  /**
   * Internal use only, is not set by xml or property utils.
   * Is used to determine the number of territories we need to satisfy a specific territory based condition check.
   * It is set multiple times during each check [isSatisfied], as there might be multiple types of territory checks
   * being done. So it is
   * just a temporary value.
   */
  @InternalDoNotExport
  protected void setTerritoryCount(final String value) {
    if (value.equals("each")) {
      m_territoryCount = 1;
      m_countEach = true;
    } else {
      m_territoryCount = getInt(value);
    }
  }

  public int getTerritoryCount() {
    return m_territoryCount;
  }

  /**
   * Used to determine if there is a multiple on this national objective (if the user specified 'each' in the count.
   * For example, you may want to have the player receive 3 PUs for controlling each territory, in a list of
   * territories.
   */
  public int getEachMultiple() {
    if (!getCountEach()) {
      return 1;
    }
    return m_eachMultiple;
  }

  protected boolean getCountEach() {
    return m_countEach;
  }

  private void setUses(final String s) {
    m_uses = getInt(s);
  }

  private void setUses(final int u) {
    m_uses = u;
  }

  /**
   * "uses" on RulesAttachments apply ONLY to giving money (PUs) to the player, they do NOT apply to the condition, and
   * therefore should not
   * be tested for in isSatisfied.
   */
  public int getUses() {
    return m_uses;
  }

  private void resetUses() {
    m_uses = -1;
  }

  private void setSwitch(final String value) {
    m_switch = getBool(value);
  }

  private void setSwitch(final Boolean value) {
    m_switch = value;
  }

  private boolean getSwitch() {
    return m_switch;
  }

  private void resetSwitch() {
    m_switch = true;
  }

  private void setGameProperty(final String value) {
    m_gameProperty = value;
  }

  private String getGameProperty() {
    return m_gameProperty;
  }

  boolean getGamePropertyState(final GameData data) {
    return m_gameProperty != null && data.getProperties().get(m_gameProperty, false);
  }

  private void resetGameProperty() {
    m_gameProperty = null;
  }

  private void setRounds(final String rounds) throws GameParseException {
    if (rounds == null) {
      m_turns = null;
      return;
    }
    m_turns = new HashMap<>();
    final String[] s = splitOnColon(rounds);
    if (s.length < 1) {
      throw new GameParseException("Empty turn list" + thisErrorMsg());
    }
    for (final String subString : s) {
      int start;
      int end;
      try {
        start = getInt(subString);
        end = start;
      } catch (final Exception e) {
        final String[] s2 = splitOnHyphen(subString);
        if (s2.length != 2) {
          throw new GameParseException("Invalid syntax for turn range, must be 'int-int'" + thisErrorMsg());
        }
        start = getInt(s2[0]);
        if (s2[1].equals("+")) {
          end = Integer.MAX_VALUE;
        } else {
          end = getInt(s2[1]);
        }
      }
      m_turns.put(start, end);
    }
  }

  private void setTurns(final String turns) throws GameParseException {
    setRounds(turns);
  }

  private void setTurns(final Map<Integer, Integer> value) {
    m_turns = value;
  }

  private Map<Integer, Integer> getTurns() {
    return m_turns;
  }

  private void resetTurns() {
    m_turns = null;
  }

  protected boolean checkTurns(final GameData data) {
    final int turn = data.getSequence().getRound();
    for (final int t : m_turns.keySet()) {
      if (turn >= t && turn <= m_turns.get(t)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Takes a string like "original", "originalNoWater", "enemy", "controlled", "controlledNoWater", "all", "map", and
   * turns it into an
   * actual list of territories.
   * Also sets territoryCount.
   */
  protected Set<Territory> getTerritoriesBasedOnStringName(final String name, final Collection<PlayerID> players,
      final GameData data) {
    final GameMap gameMap = data.getMap();
    switch (name) {
      case "original":
      case "enemy": { // get all originally owned territories
        final Set<Territory> originalTerrs = new HashSet<>();
        for (final PlayerID player : players) {
          originalTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
        }
        setTerritoryCount(String.valueOf(originalTerrs.size()));
        return originalTerrs;
      }
      case "originalNoWater": { // get all originally owned territories, but no water or impassables
        final Set<Territory> originalTerrs = new HashSet<>();
        for (final PlayerID player : players) {
          originalTerrs.addAll(CollectionUtils.getMatches(OriginalOwnerTracker.getOriginallyOwned(data, player),
              // TODO: does this account for occupiedTerrOf???
              Matches.territoryIsNotImpassableToLandUnits(player, data)));
        }
        setTerritoryCount(String.valueOf(originalTerrs.size()));
        return originalTerrs;
      }
      case "controlled":
        final Set<Territory> ownedTerrs = new HashSet<>();
        for (final PlayerID player : players) {
          ownedTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
        }
        setTerritoryCount(String.valueOf(ownedTerrs.size()));
        return ownedTerrs;
      case "controlledNoWater":
        final Set<Territory> ownedTerrsNoWater = new HashSet<>();
        for (final PlayerID player : players) {
          ownedTerrsNoWater.addAll(CollectionUtils.getMatches(gameMap.getTerritoriesOwnedBy(player),
              Matches.territoryIsNotImpassableToLandUnits(player, data)));
        }
        setTerritoryCount(String.valueOf(ownedTerrsNoWater.size()));
        return ownedTerrsNoWater;
      case "all": {
        final Set<Territory> allTerrs = new HashSet<>();
        for (final PlayerID player : players) {
          allTerrs.addAll(gameMap.getTerritoriesOwnedBy(player));
          allTerrs.addAll(OriginalOwnerTracker.getOriginallyOwned(data, player));
        }
        setTerritoryCount(String.valueOf(allTerrs.size()));
        return allTerrs;
      }
      case "map": {
        final Set<Territory> allTerrs = new HashSet<>(gameMap.getTerritories());
        setTerritoryCount(String.valueOf(allTerrs.size()));
        return allTerrs;
      }
      default: // The list just contained 1 territory
        final Territory t = data.getMap().getTerritory(name);
        if (t == null) {
          throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
        }
        final Set<Territory> terr = new HashSet<>();
        terr.add(t);
        setTerritoryCount(String.valueOf(1));
        return terr;
    }
  }

  /**
   * Takes the raw data from the xml, and turns it into an actual territory list.
   * Will also set territoryCount.
   */
  protected Set<Territory> getTerritoryListBasedOnInputFromXml(final String[] terrs, final Collection<PlayerID> players,
      final GameData data) {
    // If there's only 1, it might be a 'group' (original, controlled, controlledNoWater, all)
    if (terrs.length == 1) {
      return getTerritoriesBasedOnStringName(terrs[0], players, data);
    } else if (terrs.length == 2) {
      if (!terrs[1].equals("controlled") && !terrs[1].equals("controlledNoWater") && !terrs[1].equals("original")
          && !terrs[1].equals("originalNoWater") && !terrs[1].equals("all") && !terrs[1].equals("map")
          && !terrs[1].equals("enemy")) {
        // Get the list of territories
        return getListedTerritories(terrs, true, true);
      }

      final Set<Territory> territories = getTerritoriesBasedOnStringName(terrs[1], players, data);
      // set it a second time, since getTerritoriesBasedOnStringName also sets it (so do it
      setTerritoryCount(String.valueOf(terrs[0]));
      // after the method call).
      return territories;
    } else {
      // Get the list of territories
      return getListedTerritories(terrs, true, true);
    }
  }

  protected void validateNames(final String[] terrList) {
    if (terrList != null && terrList.length > 0) {
      getListedTerritories(terrList, true, true);
      // removed checks for length & group commands because it breaks the setTerritoryCount feature.
    }
  }

  /**
   * Validate that all listed territories actually exist. Will return an empty list of territories if sent a list that
   * is empty or contains
   * only a "" string.
   */
  public Set<Territory> getListedTerritories(final String[] list, final boolean testFirstItemForCount,
      final boolean mustSetTerritoryCount) {
    final Set<Territory> territories = new HashSet<>();
    // this list is null, empty, or contains "", so return a blank list of territories
    if (list == null || list.length == 0 || (list.length == 1 && (list[0] == null || list[0].length() == 0))) {
      return territories;
    }
    boolean haveSetCount = false;
    for (int i = 0; i < list.length; i++) {
      final String name = list[i];
      if (testFirstItemForCount && i == 0) {
        // See if the first entry contains the number of territories needed to meet the criteria
        try {
          // check if this is an integer, and if so set territory count
          getInt(name);
          if (mustSetTerritoryCount) {
            haveSetCount = true;
            setTerritoryCount(name);
          }
          continue;
        } catch (final Exception e) {
          // territory name is not an integer; fall through
        }
      }
      if (name.equals("each")) {
        m_countEach = true;
        if (mustSetTerritoryCount) {
          haveSetCount = true;
          setTerritoryCount(String.valueOf(1));
        }
        continue;
      }
      // Skip looking for the territory if the original list contains one of the 'group' commands
      if (name.equals("controlled") || name.equals("controlledNoWater") || name.equals("original")
          || name.equals("originalNoWater") || name.equals("all") || name.equals("map") || name.equals("enemy")) {
        break;
      }
      // Validate all territories exist
      final Territory territory = getData().getMap().getTerritory(name);
      if (territory == null) {
        throw new IllegalStateException("No territory called:" + name + thisErrorMsg());
      }
      territories.add(territory);
    }
    if (mustSetTerritoryCount && !haveSetCount) {
      // if we have not set it, then set it to be the size of this list
      setTerritoryCount(String.valueOf(territories.size()));
    }
    return territories;
  }

  @Override
  public Map<String, MutableProperty<?>> getPropertyMap() {
    return ImmutableMap.<String, MutableProperty<?>>builder()
        .putAll(super.getPropertyMap())
        .put("countEach", MutableProperty.ofReadOnly(this::getCountEach))
        .put("eachMultiple", MutableProperty.ofReadOnly(this::getEachMultiple))
        .put("players",
            MutableProperty.of(
                this::setPlayers,
                this::setPlayers,
                this::getPlayers,
                this::resetPlayers))
        .put("objectiveValue",
            MutableProperty.of(
                this::setObjectiveValue,
                this::setObjectiveValue,
                this::getObjectiveValue,
                this::resetObjectiveValue))
        .put("uses",
            MutableProperty.of(
                this::setUses,
                this::setUses,
                this::getUses,
                this::resetUses))
        .put("turns",
            MutableProperty.of(
                this::setTurns,
                this::setTurns,
                this::getTurns,
                this::resetTurns))
        .put("switch",
            MutableProperty.of(
                this::setSwitch,
                this::setSwitch,
                this::getSwitch,
                this::resetSwitch))
        .put("gameProperty",
            MutableProperty.ofString(
                this::setGameProperty,
                this::getGameProperty,
                this::resetGameProperty))
        .put("rounds", MutableProperty.ofWriteOnlyString(this::setRounds))
        .build();
  }
}
