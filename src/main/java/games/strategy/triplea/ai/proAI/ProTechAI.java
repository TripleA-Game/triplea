package games.strategy.triplea.ai.proAI;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Resource;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.triplea.Constants;
import games.strategy.triplea.Properties;
import games.strategy.triplea.attachments.TerritoryAttachment;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.Matches;
import games.strategy.triplea.delegate.MoveValidator;
import games.strategy.triplea.delegate.TechAdvance;
import games.strategy.triplea.delegate.TransportTracker;
import games.strategy.triplea.delegate.remote.ITechDelegate;
import games.strategy.util.IntegerMap;
import games.strategy.util.Match;

/**
 * Pro tech AI.
 */
final class ProTechAI {

  static void tech(final ITechDelegate techDelegate, final GameData data, final PlayerID player) {
    if (!Properties.getWW2V3TechModel(data)) {
      return;
    }
    final Territory myCapitol = TerritoryAttachment.getFirstOwnedCapitalOrFirstUnownedCapital(player, data);
    final float enemyStrength = getStrengthOfPotentialAttackers(myCapitol, data, player);
    float myStrength = (myCapitol == null || myCapitol.getUnits() == null) ? 0.0F
        : strength(myCapitol.getUnits().getUnits(), false, false, false);
    final List<Territory> areaStrength = getNeighboringLandTerritories(data, player, myCapitol);
    for (final Territory areaTerr : areaStrength) {
      myStrength += strength(areaTerr.getUnits().getUnits(), false, false, false) * 0.75F;
    }
    final boolean capDanger = myStrength < (enemyStrength * 1.25F + 3.0F);
    final Resource pus = data.getResourceList().getResource(Constants.PUS);
    final int pusRemaining = player.getResources().getQuantity(pus);
    final Resource techtokens = data.getResourceList().getResource(Constants.TECH_TOKENS);
    final int techTokensQuantity = player.getResources().getQuantity(techtokens);
    int tokensToBuy = 0;
    if (!capDanger && techTokensQuantity < 3 && pusRemaining > Math.random() * 160) {
      tokensToBuy = 1;
    }
    if (techTokensQuantity > 0 || tokensToBuy > 0) {
      final List<TechnologyFrontier> cats = TechAdvance.getPlayerTechCategories(player);
      // retaining 65% chance of choosing land advances using basic ww2v3 model.
      if (data.getTechnologyFrontier().isEmpty()) {
        if (Math.random() > 0.35) {
          techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(1), tokensToBuy, null);
        } else {
          techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(0), tokensToBuy, null);
        }
      } else {
        final int rand = (int) (Math.random() * cats.size());
        techDelegate.rollTech(techTokensQuantity + tokensToBuy, cats.get(rand), tokensToBuy, null);
      }
    }
  }

  /**
   * Returns the strength of all attackers to a territory.
   * Differentiates between sea and land attack
   * Determines all transports within range of territory
   * Determines all air units within range of territory (using 2 for fighters and 3 for bombers)
   * Does not check for extended range fighters or bombers
   */
  private static float getStrengthOfPotentialAttackers(final Territory location, final GameData data,
      final PlayerID player) {
    final boolean transportsFirst = false;
    final boolean ignoreOnlyPlanes = true;

    PlayerID enemyPlayer = null;
    final List<PlayerID> enemyPlayers = getEnemyPlayers(data, player);
    final HashMap<PlayerID, Float> enemyPlayerAttackMap = new HashMap<>();
    final Iterator<PlayerID> playerIter = enemyPlayers.iterator();
    if (location == null) {
      return -1000.0F;
    }
    boolean nonTransportsInAttack = false;
    final boolean onWater = location.isWater();
    if (!onWater) {
      nonTransportsInAttack = true;
    }
    final Set<Territory> waterTerr = data.getMap().getNeighbors(location, Matches.territoryIsWater());
    while (playerIter.hasNext()) {
      float seaStrength = 0.0F;
      float firstStrength = 0.0F;
      float secondStrength = 0.0F;
      float blitzStrength = 0.0F;
      float strength;
      enemyPlayer = playerIter.next();
      final Match<Unit> enemyPlane = Match.allOf(
          Matches.unitIsAir(),
          Matches.unitIsOwnedBy(enemyPlayer),
          Matches.unitCanMove());
      final Match<Unit> enemyTransport = Match.allOf(Matches.unitIsOwnedBy(enemyPlayer),
          Matches.unitIsSea(), Matches.unitIsTransport(), Matches.unitCanMove());
      final Match<Unit> enemyShip = Match.allOf(
          Matches.unitIsOwnedBy(enemyPlayer),
          Matches.unitIsSea(),
          Matches.unitCanMove());
      final Match<Unit> enemyTransportable = Match.allOf(Matches.unitIsOwnedBy(enemyPlayer),
          Matches.unitCanBeTransported(), Matches.unitIsNotAa(), Matches.unitCanMove());
      final Match<Unit> transport = Match.allOf(Matches.unitIsSea(), Matches.unitIsTransport(), Matches.unitCanMove());
      final List<Territory> enemyFighterTerritories = findUnitTerr(data, enemyPlane);
      int maxFighterDistance = 0;
      // should change this to read production frontier and tech
      // reality is 99% of time units considered will have full move.
      // and likely player will have at least 1 max move plane.
      for (final Territory enemyFighterTerritory : enemyFighterTerritories) {
        final List<Unit> enemyFighterUnits = enemyFighterTerritory.getUnits().getMatches(enemyPlane);
        maxFighterDistance = Math.max(maxFighterDistance, MoveValidator.getMaxMovement(enemyFighterUnits));
      }
      // must be able to land...we will miss fighters who have a Carrier that can reach same sea zone...C'est la vie
      maxFighterDistance--;
      if (maxFighterDistance < 0) {
        maxFighterDistance = 0;
      }
      final List<Territory> enemyTransportTerritories = findUnitTerr(data, transport);
      int maxTransportDistance = 0;
      for (final Territory enemyTransportTerritory : enemyTransportTerritories) {
        final List<Unit> enemyTransportUnits = enemyTransportTerritory.getUnits().getMatches(transport);
        maxTransportDistance = Math.max(maxTransportDistance, MoveValidator.getMaxMovement(enemyTransportUnits));
      }
      final List<Unit> alreadyLoaded = new ArrayList<>();
      final List<Route> blitzTerrRoutes = new ArrayList<>();
      final List<Territory> checked = new ArrayList<>();
      final List<Unit> enemyWaterUnits = new ArrayList<>();
      for (final Territory t : data.getMap().getNeighbors(location,
          onWater ? Matches.territoryIsWater() : Matches.territoryIsLand())) {
        final List<Unit> enemies = t.getUnits().getMatches(Matches.unitIsOwnedBy(enemyPlayer));
        enemyWaterUnits.addAll(enemies);
        firstStrength += strength(enemies, true, onWater, transportsFirst);
        checked.add(t);
      }
      if (Matches.territoryIsLand().match(location)) {
        blitzStrength = determineEnemyBlitzStrength(location, blitzTerrRoutes, data, enemyPlayer);
      } else { // get ships attack strength
        // old assumed fleets won't split up, new lets them. no biggie.
        // assumes max ship movement is 3.
        // note, both old and new implementations
        // allow units to be calculated that are in
        // territories we have already assaulted
        // this can be easily changed
        final HashSet<Integer> ignore = new HashSet<>();
        ignore.add(1);
        final List<Route> r = new ArrayList<>();
        final List<Unit> ships = findAttackers(location, 3, ignore, enemyPlayer, data, enemyShip,
            Matches.territoryIsBlockedSea(enemyPlayer, data), r, true);
        secondStrength = strength(ships, true, true, transportsFirst);
        enemyWaterUnits.addAll(ships);
      }
      final List<Unit> attackPlanes =
          findPlaneAttackersThatCanLand(location, maxFighterDistance, enemyPlayer, data, checked);
      final float airStrength = allAirStrength(attackPlanes);
      if (Matches.territoryHasWaterNeighbor(data).match(location) && Matches.territoryIsLand().match(location)) {
        for (final Territory t4 : data.getMap().getNeighbors(location, maxTransportDistance)) {
          if (!t4.isWater()) {
            continue;
          }
          boolean transportsCounted = false;
          final Iterator<Territory> iterTerr = waterTerr.iterator();
          while (!transportsCounted && iterTerr.hasNext()) {
            final Territory waterCheck = iterTerr.next();
            if (enemyPlayer == null) {
              continue;
            }
            final List<Unit> transports = t4.getUnits().getMatches(enemyTransport);
            if (transports.isEmpty()) {
              continue;
            }
            if (!t4.equals(waterCheck)) {
              final Route seaRoute = getMaxSeaRoute(data, t4, waterCheck, enemyPlayer, maxTransportDistance);
              if (seaRoute == null || seaRoute.getEnd() == null || seaRoute.getEnd() != waterCheck) {
                continue;
              }
            }
            final List<Unit> loadedUnits = new ArrayList<>();
            int availInf = 0;
            int availOther = 0;
            for (final Unit candidateTransport : transports) {
              final Collection<Unit> thisTransUnits = TransportTracker.transporting(candidateTransport);
              if (thisTransUnits == null) {
                availInf += 2;
                availOther += 1;
                continue;
              } else {
                int inf = 2;
                int other = 1;
                for (final Unit checkUnit : thisTransUnits) {
                  if (Matches.unitIsInfantry().match(checkUnit)) {
                    inf--;
                  }
                  if (Matches.unitIsNotInfantry().match(checkUnit)) {
                    inf--;
                    other--;
                  }
                  loadedUnits.add(checkUnit);
                }
                availInf += inf;
                availOther += other;
              }
            }
            final Set<Territory> transNeighbors =
                data.getMap().getNeighbors(t4, Matches.isTerritoryAllied(enemyPlayer, data));
            for (final Territory transNeighbor : transNeighbors) {
              final List<Unit> transUnits = transNeighbor.getUnits().getMatches(enemyTransportable);
              transUnits.removeAll(alreadyLoaded);
              final List<Unit> availTransUnits = sortTransportUnits(transUnits);
              for (final Unit transUnit : availTransUnits) {
                if (availInf > 0 && Matches.unitIsInfantry().match(transUnit)) {
                  availInf--;
                  loadedUnits.add(transUnit);
                  alreadyLoaded.add(transUnit);
                }
                if (availInf > 0 && availOther > 0 && Matches.unitIsNotInfantry().match(transUnit)) {
                  availInf--;
                  availOther--;
                  loadedUnits.add(transUnit);
                  alreadyLoaded.add(transUnit);
                }
              }
            }
            seaStrength += strength(loadedUnits, true, false, transportsFirst);
            transportsCounted = true;
          }
        }
      }
      strength = seaStrength + blitzStrength + firstStrength + secondStrength;
      if (!ignoreOnlyPlanes || strength > 0.0F) {
        strength += airStrength;
      }
      if (onWater) {
        final Iterator<Unit> enemyWaterUnitsIter = enemyWaterUnits.iterator();
        while (enemyWaterUnitsIter.hasNext() && !nonTransportsInAttack) {
          if (Matches.unitIsNotTransport().match(enemyWaterUnitsIter.next())) {
            nonTransportsInAttack = true;
          }
        }
      }
      if (!nonTransportsInAttack) {
        strength = 0.0F;
      }
      enemyPlayerAttackMap.put(enemyPlayer, strength);
    }
    float maxStrength = 0.0F;
    for (final PlayerID enemyPlayerCandidate : enemyPlayers) {
      if (enemyPlayerAttackMap.get(enemyPlayerCandidate) > maxStrength) {
        enemyPlayer = enemyPlayerCandidate;
        maxStrength = enemyPlayerAttackMap.get(enemyPlayerCandidate);
      }
    }
    for (final PlayerID enemyPlayerCandidate : enemyPlayers) {
      if (enemyPlayer != enemyPlayerCandidate) {
        // give 40% of other players...this is will affect a lot of decisions by AI
        maxStrength += enemyPlayerAttackMap.get(enemyPlayerCandidate) * 0.40F;
      }
    }
    return maxStrength;
  }

  /**
   * Get a quick and dirty estimate of the strength of some units in a battle.
   *
   * @param units - the units to measure
   * @param attacking - are the units on attack or defense
   * @param sea - calculate the strength of the units in a sea or land battle?
   */
  private static float strength(final Collection<Unit> units, final boolean attacking, final boolean sea,
      final boolean transportsFirst) {
    float strength = 0.0F;
    if (units.isEmpty()) {
      return strength;
    }
    if (attacking && Match.noneMatch(units, Matches.unitHasAttackValueOfAtLeast(1))) {
      return strength;
    } else if (!attacking && Match.noneMatch(units, Matches.unitHasDefendValueOfAtLeast(1))) {
      return strength;
    }
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      if (unitAttachment.getIsInfrastructure()) {
        continue;
      } else if (unitAttachment.getIsSea() == sea) {
        final int unitAttack = unitAttachment.getAttack(u.getOwner());
        // BB = 6.0; AC=2.0/4.0; SUB=3.0; DS=4.0; TR=0.50/2.0; F=4.0/5.0; B=5.0/2.0;
        // played with this value a good bit
        strength += 1.00F;
        if (attacking) {
          strength += unitAttack * unitAttachment.getHitPoints();
        } else {
          strength += unitAttachment.getDefense(u.getOwner()) * unitAttachment.getHitPoints();
        }
        if (attacking) {
          if (unitAttack == 0) {
            strength -= 0.50F;
          }
        }
        if (unitAttack == 0 && unitAttachment.getTransportCapacity() > 0 && !transportsFirst) {
          // only allow transport to have 0.35 on defense; none on attack
          strength -= 0.50F;
        }
      } else if (unitAttachment.getIsAir() == sea) {
        strength += 1.00F;
        if (attacking) {
          strength += unitAttachment.getAttack(u.getOwner()) * unitAttachment.getAttackRolls(u.getOwner());
        } else {
          strength += unitAttachment.getDefense(u.getOwner());
        }
      }
    }
    if (attacking && !sea) {
      final int art = Matches.countMatches(units, Matches.unitIsArtillery());
      final int artSupport = Matches.countMatches(units, Matches.unitIsArtillerySupportable());
      strength += Math.min(art, artSupport);
    }
    return strength;
  }

  /**
   * Returns a list of all enemy players.
   */
  private static List<PlayerID> getEnemyPlayers(final GameData data, final PlayerID player) {
    final List<PlayerID> enemyPlayers = new ArrayList<>();
    for (final PlayerID players : data.getPlayerList().getPlayers()) {
      if (!data.getRelationshipTracker().isAllied(player, players)) {
        enemyPlayers.add(players);
      }
    }
    return enemyPlayers;
  }

  /**
   * Determine the enemy potential for blitzing a territory - all enemies are combined.
   *
   * @param blitzHere
   *        - Territory expecting to be blitzed
   * @return actual strength of enemy units (armor)
   */
  private static float determineEnemyBlitzStrength(final Territory blitzHere, final List<Route> blitzTerrRoutes,
      final GameData data, final PlayerID enemyPlayer) {
    final HashSet<Integer> ignore = new HashSet<>();
    ignore.add(1);
    final Match<Unit> blitzUnit =
        Match.allOf(Matches.unitIsOwnedBy(enemyPlayer), Matches.unitCanBlitz(), Matches.unitCanMove());
    final Match<Territory> validBlitzRoute = Match.allOf(
        Matches.territoryHasNoEnemyUnits(enemyPlayer, data),
        Matches.territoryIsNotImpassableToLandUnits(enemyPlayer, data));
    final List<Route> routes = new ArrayList<>();
    final List<Unit> blitzUnits =
        findAttackers(blitzHere, 2, ignore, enemyPlayer, data, blitzUnit, validBlitzRoute, routes, false);
    for (final Route r : routes) {
      if (r.numberOfSteps() == 2) {
        blitzTerrRoutes.add(r);
      }
    }
    return strength(blitzUnits, true, false, true);
  }

  private static List<Unit> findAttackers(final Territory start, final int maxDistance,
      final HashSet<Integer> ignoreDistance, final PlayerID player, final GameData data,
      final Match<Unit> unitCondition, final Match<Territory> routeCondition,
      final List<Route> routes, final boolean sea) {

    final IntegerMap<Territory> distance = new IntegerMap<>();
    final Map<Territory, Territory> visited = new HashMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    q.add(start);
    Territory current;
    distance.put(start, 0);
    visited.put(start, null);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current)) {
        if (!distance.keySet().contains(neighbor)) {
          if (!neighbor.getUnits().anyMatch(unitCondition)) {
            if (!routeCondition.match(neighbor)) {
              continue;
            }
          }
          if (sea) {
            final Route r = new Route();
            r.setStart(neighbor);
            r.add(current);
            if (MoveValidator.validateCanal(r, null, player, data) != null) {
              continue;
            }
          }
          distance.put(neighbor, distance.getInt(current) + 1);
          visited.put(neighbor, current);
          q.add(neighbor);
          final int dist = distance.getInt(neighbor);
          if (ignoreDistance.contains(dist)) {
            continue;
          }
          for (final Unit u : neighbor.getUnits()) {
            if (unitCondition.match(u) && Matches.unitHasEnoughMovementForRoutes(routes).match(u)) {
              units.add(u);
            }
          }
        }
      }
    }
    // pain in the ass, should just redesign stop blitz attack
    for (final Territory t : visited.keySet()) {
      final Route r = new Route();
      Territory t2 = t;
      r.setStart(t);
      while (t2 != null) {
        t2 = visited.get(t2);
        if (t2 != null) {
          r.add(t2);
        }
      }
      routes.add(r);
    }
    return units;
  }

  /**
   * does not count planes already in the starting territory.
   */
  private static List<Unit> findPlaneAttackersThatCanLand(final Territory start, final int maxDistance,
      final PlayerID player, final GameData data, final List<Territory> checked) {

    if (checked.isEmpty()) {
      return new ArrayList<>();
    }
    final IntegerMap<Territory> distance = new IntegerMap<>();
    final IntegerMap<Unit> unitDistance = new IntegerMap<>();
    final List<Unit> units = new ArrayList<>();
    final Queue<Territory> q = new LinkedList<>();
    Territory lz = null;
    Territory ac = null;
    final Match<Unit> enemyPlane = Match.allOf(
        Matches.unitIsAir(),
        Matches.unitIsOwnedBy(player),
        Matches.unitCanMove());
    final Match<Unit> enemyCarrier =
        Match.allOf(Matches.unitIsCarrier(), Matches.unitIsOwnedBy(player), Matches.unitCanMove());
    q.add(start);
    Territory current;
    distance.put(start, 0);
    while (!q.isEmpty()) {
      current = q.remove();
      if (distance.getInt(current) == maxDistance) {
        break;
      }
      for (final Territory neighbor : data.getMap().getNeighbors(current, territoryIsNotImpassableToAirUnits())) {
        if (!distance.keySet().contains(neighbor)) {
          q.add(neighbor);
          distance.put(neighbor, distance.getInt(current) + 1);
          if (lz == null && Matches.isTerritoryAllied(player, data).match(neighbor) && !neighbor.isWater()) {
            lz = neighbor;
          }
          if (checked.contains(neighbor)) {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
            }
          } else {
            for (final Unit u : neighbor.getUnits()) {
              if (ac == null && enemyCarrier.match(u)) {
                ac = neighbor;
              }
              if (enemyPlane.match(u)) {
                unitDistance.put(u, distance.getInt(neighbor));
              }
            }
          }
        }
      }
    }
    for (final Unit u : unitDistance.keySet()) {
      if (lz != null && Matches.unitHasEnoughMovementForRoute(checked).match(u)) {
        units.add(u);
      } else if (ac != null && Matches.unitCanLandOnCarrier().match(u)
          && Matches.unitHasEnoughMovementForRoute(checked).match(u)) {
        units.add(u);
      }
    }
    return units;
  }

  /**
   * Determine the strength of a collection of airUnits
   * Caller should guarantee units are all air.
   */
  private static float allAirStrength(final Collection<Unit> units) {
    float airstrength = 0.0F;
    for (final Unit u : units) {
      final UnitAttachment unitAttachment = UnitAttachment.get(u.getType());
      airstrength += 1.00F;
      airstrength += unitAttachment.getAttack(u.getOwner());
    }
    return airstrength;
  }

  private static Route getMaxSeaRoute(final GameData data, final Territory start, final Territory destination,
      final PlayerID player, final int maxDistance) {
    // note this does not care if subs are submerged or not
    // should it? does submerging affect movement of enemies?
    if (start == null || destination == null || !start.isWater() || !destination.isWater()) {
      return null;
    }
    final Match<Unit> sub = Match.allOf(Matches.unitIsSub().invert());
    final Match<Unit> transport = Match.allOf(Matches.unitIsTransport().invert(), Matches.unitIsLand().invert());
    final Match.CompositeBuilder<Unit> unitCondBuilder = Match.newCompositeBuilder(
        Matches.unitIsInfrastructure().invert(),
        Matches.alliedUnit(player, data).invert());
    if (Properties.getIgnoreTransportInMovement(data)) {
      unitCondBuilder.add(transport);
    }
    if (Properties.getIgnoreSubInMovement(data)) {
      unitCondBuilder.add(sub);
    }
    final Match<Territory> routeCond = Match.allOf(
        Matches.territoryHasUnitsThatMatch(unitCondBuilder.all()).invert(),
        Matches.territoryIsWater());
    final Match<Territory> routeCondition;
    routeCondition = Match.anyOf(Matches.territoryIs(destination), routeCond);
    Route r = data.getMap().getRoute(start, destination, routeCondition);
    if (r == null || r.getEnd() == null) {
      return null;
    }
    // cheating because can't do stepwise calculation with canals
    // shouldn't be a huge problem
    // if we fail due to canal, then don't go near any enemy canals
    if (MoveValidator.validateCanal(r, null, player, data) != null) {
      r = data.getMap().getRoute(start, destination,
          Match.allOf(routeCondition, Matches.territoryHasNonAllowedCanal(player, null, data).invert()));
    }
    if (r == null || r.getEnd() == null) {
      return null;
    }
    final int routeDistance = r.numberOfSteps();
    Route route2 = new Route();
    if (routeDistance <= maxDistance) {
      route2 = r;
    } else {
      route2.setStart(start);
      for (int i = 1; i <= maxDistance; i++) {
        route2.add(r.getAllTerritories().get(i));
      }
    }
    return route2;
  }

  /**
   * All Allied Territories which neighbor a territory
   * This duplicates getNeighbors(check, Matches.isTerritoryAllied(player, data))
   */
  private static List<Territory> getNeighboringLandTerritories(final GameData data, final PlayerID player,
      final Territory check) {
    final List<Territory> territories = new ArrayList<>();
    final List<Territory> checkList = getExactNeighbors(check, data);
    for (final Territory t : checkList) {
      if (Matches.isTerritoryAllied(player, data).match(t)
          && Matches.territoryIsNotImpassableToLandUnits(player, data).match(t)) {
        territories.add(t);
      }
    }
    return territories;
  }

  /**
   * Gets the neighbors which are one territory away.
   */
  private static List<Territory> getExactNeighbors(final Territory territory, final GameData data) {
    // old functionality retained, i.e. no route condition is imposed.
    // feel free to change, if you are confortable all calls to this function conform.
    final Match.CompositeBuilder<Territory> endCondBuilder = Match.newCompositeBuilder(
        Matches.territoryIsImpassable().invert());
    if (Properties.getNeutralsImpassable(data)) {
      endCondBuilder.add(Matches.territoryIsNeutralButNotWater().invert());
    }
    final int distance = 1;
    return findFrontier(territory, endCondBuilder.all(), Matches.always(), distance, data);
  }

  /**
   * Finds list of territories at exactly distance from the start.
   *
   * @param endCondition
   *        condition that all end points must satisfy
   * @param routeCondition
   *        condition that all traversed internal territories must satisy
   */
  private static List<Territory> findFrontier(
      final Territory start,
      final Match<Territory> endCondition,
      final Match<Territory> routeCondition,
      final int distance,
      final GameData data) {
    final Match<Territory> canGo = Match.anyOf(endCondition, routeCondition);
    final IntegerMap<Territory> visited = new IntegerMap<>();
    final Queue<Territory> q = new LinkedList<>();
    final List<Territory> frontier = new ArrayList<>();
    q.addAll(data.getMap().getNeighbors(start, canGo));
    Territory current;
    visited.put(start, 0);
    for (final Territory t : q) {
      visited.put(t, 1);
      if (1 == distance && endCondition.match(t)) {
        frontier.add(t);
      }
    }
    while (!q.isEmpty()) {
      current = q.remove();
      if (visited.getInt(current) == distance) {
        break;
      } else {
        for (final Territory neighbor : data.getMap().getNeighbors(current, canGo)) {
          if (!visited.keySet().contains(neighbor)) {
            q.add(neighbor);
            final int dist = visited.getInt(current) + 1;
            visited.put(neighbor, dist);
            if (dist == distance && endCondition.match(neighbor)) {
              frontier.add(neighbor);
            }
          }
        }
      }
    }
    return frontier;
  }

  /**
   * Return Territories containing any unit depending on unitCondition
   * Differs from findCertainShips because it doesn't require the units be owned.
   */
  private static List<Territory> findUnitTerr(final GameData data, final Match<Unit> unitCondition) {
    // Return territories containing a certain unit or set of Units
    final Match<Unit> limitShips = Match.allOf(unitCondition);
    final List<Territory> shipTerr = new ArrayList<>();
    final Collection<Territory> neighbors = data.getMap().getTerritories();
    for (final Territory t2 : neighbors) {
      if (t2.getUnits().anyMatch(limitShips)) {
        shipTerr.add(t2);
      }
    }
    return shipTerr;
  }

  /**
   * Interleave infantry and artillery/armor for loading on transports.
   */
  private static List<Unit> sortTransportUnits(final List<Unit> transUnits) {
    final List<Unit> sorted = new ArrayList<>();
    final List<Unit> infantry = new ArrayList<>();
    final List<Unit> artillery = new ArrayList<>();
    final List<Unit> armor = new ArrayList<>();
    final List<Unit> others = new ArrayList<>();
    for (final Unit x : transUnits) {
      if (Matches.unitIsArtillerySupportable().match(x)) {
        infantry.add(x);
      } else if (Matches.unitIsArtillery().match(x)) {
        artillery.add(x);
      } else if (Matches.unitCanBlitz().match(x)) {
        armor.add(x);
      } else {
        others.add(x);
      }
    }
    int artilleryCount = artillery.size();
    int armorCount = armor.size();
    int othersCount = others.size();
    for (final Unit anInfantry : infantry) {
      sorted.add(anInfantry);
      // this should be based on combined attack and defense powers, not on attachments like blitz
      if (armorCount > 0) {
        sorted.add(armor.get(armorCount - 1));
        armorCount--;
      } else if (artilleryCount > 0) {
        sorted.add(artillery.get(artilleryCount - 1));
        artilleryCount--;
      } else if (othersCount > 0) {
        sorted.add(others.get(othersCount - 1));
        othersCount--;
      }
    }
    if (artilleryCount > 0) {
      for (int j2 = 0; j2 < artilleryCount; j2++) {
        sorted.add(artillery.get(j2));
      }
    }
    if (othersCount > 0) {
      for (int j4 = 0; j4 < othersCount; j4++) {
        sorted.add(others.get(j4));
      }
    }
    if (armorCount > 0) {
      for (int j3 = 0; j3 < armorCount; j3++) {
        sorted.add(armor.get(j3));
      }
    }
    return sorted;
  }

  private static Match<Territory> territoryIsNotImpassableToAirUnits() {
    return territoryIsImpassableToAirUnits().invert();
  }

  /**
   * Assumes that water is passable to air units always.
   */
  private static Match<Territory> territoryIsImpassableToAirUnits() {
    return Match.allOf(Matches.territoryIsLand(), Matches.territoryIsImpassable());
  }
}
