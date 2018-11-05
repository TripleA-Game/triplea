package games.strategy.engine.framework.message;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;

import games.strategy.engine.framework.startup.ui.PlayerType;
import games.strategy.triplea.NetworkData;
import games.strategy.util.Version;

/**
 * data from the server indicating what players are available to be
 * taken, and what players are being played.
 * This object also contains versioning info which the client should
 * check to ensure that it is playing the same game as the server.
 * (updated by veqryn to be the object that, besides game options, determines the starting setup for game. ie: who is
 * playing what)
 */
@NetworkData
public class PlayerListing implements Serializable {
  private static final long serialVersionUID = -8913538086737733980L;

  /**
   * Maps String player name -> node Name
   * if node name is null then the player is available to play.
   */
  private final Map<String, String> playerToNodeListing;
  private final Map<String, Boolean> playersEnabledListing;
  private final Map<String, String> localPlayerTypes;
  private final Collection<String> playersAllowedToBeDisabled;
  private final Version gameVersion;
  private final String gameName;
  private final String gameRound;
  private final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder;

  /**
   * Creates a new instance of PlayerListing.
   */
  public PlayerListing(
      final Map<String, String> playerToNodeListing,
      final Map<String, Boolean> playersEnabledListing,
      final Map<String, PlayerType> localPlayerTypes,
      final Version gameVersion,
      final String gameName,
      final String gameRound,
      final Collection<String> playersAllowedToBeDisabled,
      final Map<String, Collection<String>> playerNamesAndAlliancesInTurnOrder) {

    // Note: Sets from guava immutables are not necessarily serializable (!)
    // We use copy constructors here to avoid this problem.

    this.playerToNodeListing = Optional.ofNullable(playerToNodeListing)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
    this.playersEnabledListing = Optional.ofNullable(playersEnabledListing)
        .map(HashMap::new)
        .orElseGet(HashMap::new);
    this.localPlayerTypes = localPlayerTypes.entrySet()
        .stream()
        // convert Map<String,PlayerType> -> Map<String,String>
        .collect(Collectors.toMap(Entry::getKey, e -> e.getValue().getLabel()));
    this.gameVersion = gameVersion;
    this.gameName = gameName;
    this.gameRound = gameRound;
    this.playersAllowedToBeDisabled = Optional.ofNullable(playersAllowedToBeDisabled)
        .map(HashSet::new)
        .orElseGet(HashSet::new);

    this.playerNamesAndAlliancesInTurnOrder =
        Optional.ofNullable(playerNamesAndAlliancesInTurnOrder)
            .orElse(Collections.emptyMap())
            .entrySet()
            .stream()
            .collect(Collectors.toMap(
                Entry::getKey,
                e -> Sets.newHashSet(e.getValue()),
                (u, v) -> {
                  throw new IllegalStateException(String.format("Duplicate key %s", u));
                },
                LinkedHashMap::new));

    // make sure none of the collection values are null.
    // playerToNodeListing is an exception, it can have null values meaning no user has chosen a given nation.
    Preconditions.checkArgument(this.playersEnabledListing.values().stream().noneMatch(Objects::isNull),
        this.playersEnabledListing.toString());
    Preconditions.checkArgument(this.localPlayerTypes.values().stream().noneMatch(Objects::isNull),
        this.localPlayerTypes.toString());
    Preconditions.checkArgument(this.playersAllowedToBeDisabled.stream().noneMatch(Objects::isNull),
        this.playersAllowedToBeDisabled.toString());
    Preconditions.checkArgument(this.playerNamesAndAlliancesInTurnOrder.values().stream().noneMatch(Objects::isNull),
        this.playerNamesAndAlliancesInTurnOrder.toString());
  }

  public Collection<String> getPlayersAllowedToBeDisabled() {
    return playersAllowedToBeDisabled;
  }

  public Map<String, String> getPlayerToNodeListing() {
    return playerToNodeListing;
  }

  public Map<String, Boolean> getPlayersEnabledListing() {
    return playersEnabledListing;
  }

  public Map<String, Collection<String>> getPlayerNamesAndAlliancesInTurnOrderLinkedHashMap() {
    return playerNamesAndAlliancesInTurnOrder;
  }

  public String getGameName() {
    return gameName;
  }

  public Version getGameVersion() {
    return gameVersion;
  }

  @Override
  public String toString() {
    return "PlayerListingMessage:" + playerToNodeListing;
  }

  public String getGameRound() {
    return gameRound;
  }

  public Map<String, PlayerType> getLocalPlayerTypeMap() {
    return localPlayerTypes.entrySet()
        .stream()
        .collect(Collectors.toMap(Entry::getKey, e -> PlayerType.fromLabel(e.getValue())));
  }
}
