package games.strategy.triplea.delegate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.CompositeChange;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.PlayerID;
import games.strategy.engine.data.Route;
import games.strategy.engine.data.RouteScripted;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.net.GUID;
import games.strategy.sound.SoundPath;
import games.strategy.triplea.Properties;
import games.strategy.triplea.TripleAUnit;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.dataObjects.BattleRecord;
import games.strategy.triplea.delegate.dataObjects.CasualtyDetails;
import games.strategy.triplea.formatter.MyFormatter;
import games.strategy.triplea.ui.display.ITripleADisplay;
import games.strategy.triplea.util.TuvUtils;
import games.strategy.util.CollectionUtils;
import games.strategy.util.IntegerMap;
import games.strategy.util.Interruptibles;
import games.strategy.util.PredicateBuilder;
import lombok.extern.java.Log;

/**
 * Battle class used for air battles and interception before a standard battle.
 */
@Log
public class AirBattle extends AbstractBattle {
  private static final long serialVersionUID = 4686241714027216395L;
  protected static final String AIR_BATTLE = "Air Battle";
  protected static final String INTERCEPTORS_LAUNCH = "Defender Launches Interceptors";
  protected static final String ATTACKERS_FIRE = "Attackers Fire";
  protected static final String DEFENDERS_FIRE = "Defenders Fire";
  protected static final String ATTACKERS_WITHDRAW = "Attackers Withdraw?";
  protected static final String DEFENDERS_WITHDRAW = "Defenders Withdraw?";
  protected final ExecutionStack m_stack = new ExecutionStack();
  protected List<String> m_steps;
  protected final Collection<Unit> m_defendingWaitingToDie = new ArrayList<>();
  protected final Collection<Unit> m_attackingWaitingToDie = new ArrayList<>();
  protected boolean m_intercept = false;
  // -1 would mean forever until one side is eliminated. (default is 1 round)
  protected final int m_maxRounds;

  AirBattle(final Territory battleSite, final boolean bombingRaid, final GameData data, final PlayerID attacker,
      final BattleTracker battleTracker) {
    super(battleSite, attacker, battleTracker, bombingRaid, (bombingRaid ? BattleType.AIR_RAID : BattleType.AIR_BATTLE),
        data);
    isAmphibious = false;
    m_maxRounds = Properties.getAirBattleRounds(data);
    updateDefendingUnits();
  }

  public void updateDefendingUnits() {
    // fill in defenders
    if (isBombingRun) {
      defendingUnits =
          battleSite.getUnits().getMatches(defendingBombingRaidInterceptors(battleSite, attacker, gameData));
    } else {
      defendingUnits = battleSite.getUnits().getMatches(defendingGroundSeaBattleInterceptors(attacker, gameData));
    }
  }

  @Override
  public Change addAttackChange(final Route route, final Collection<Unit> units,
      final Map<Unit, Set<Unit>> targets) {
    attackingUnits.addAll(units);
    return ChangeFactory.EMPTY_CHANGE;
  }

  @Override
  public void removeAttack(final Route route, final Collection<Unit> units) {
    attackingUnits.removeAll(units);
  }

  @Override
  public void fight(final IDelegateBridge bridge) {
    // remove units that may already be dead due to a previous event (like they died from a strategic bombing raid,
    // rocket attack, etc)
    removeUnitsThatNoLongerExist();
    // we were interrupted
    if (m_stack.isExecuting()) {
      showBattle(bridge);
      m_stack.execute(bridge);
      return;
    }
    updateDefendingUnits();
    bridge.getHistoryWriter().startEvent("Air Battle in " + battleSite, battleSite);
    BattleCalculator.sortPreBattle(attackingUnits);
    BattleCalculator.sortPreBattle(defendingUnits);
    m_steps = determineStepStrings(true);
    showBattle(bridge);
    pushFightLoopOnStack(true);
    m_stack.execute(bridge);
  }

  private void pushFightLoopOnStack(final boolean firstRun) {
    if (isOver) {
      return;
    }
    final List<IExecutable> steps = getBattleExecutables(firstRun);
    // add in the reverse order we create them
    Collections.reverse(steps);
    for (final IExecutable step : steps) {
      m_stack.push(step);
    }
  }

  private boolean shouldFightAirBattle() {
    return !defendingUnits.isEmpty()
        && (isBombingRun
            ? attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())
            : !attackingUnits.isEmpty());
  }

  public boolean shouldEndBattleDueToMaxRounds() {
    return m_maxRounds > 0 && m_maxRounds <= round;
  }

  protected boolean canAttackerRetreat() {
    return !shouldEndBattleDueToMaxRounds() && shouldFightAirBattle()
        && Properties.getAirBattleAttackersCanRetreat(gameData);
  }

  protected boolean canDefenderRetreat() {
    return !shouldEndBattleDueToMaxRounds() && shouldFightAirBattle()
        && Properties.getAirBattleDefendersCanRetreat(gameData);
  }

  List<IExecutable> getBattleExecutables(final boolean firstRun) {
    final List<IExecutable> steps = new ArrayList<>();
    if (shouldFightAirBattle()) {
      if (firstRun) {
        steps.add(new InterceptorsLaunch());
      }
      steps.add(new AttackersFire());
      steps.add(new DefendersFire());
      steps.add(new IExecutable() { // just calculates lost TUV and kills off any suicide units
        private static final long serialVersionUID = -5575569705493214941L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          if (!m_intercept) {
            return;
          }
          final IntegerMap<UnitType> defenderCosts = TuvUtils.getCostsForTuv(defender, gameData);
          final IntegerMap<UnitType> attackerCosts = TuvUtils.getCostsForTuv(attacker, gameData);
          attackingUnits.removeAll(m_attackingWaitingToDie);
          remove(m_attackingWaitingToDie, bridge, battleSite);
          defendingUnits.removeAll(m_defendingWaitingToDie);
          remove(m_defendingWaitingToDie, bridge, battleSite);
          int tuvLostAttacker = TuvUtils.getTuv(m_attackingWaitingToDie, attacker, attackerCosts, gameData);
          attackerLostTuv += tuvLostAttacker;
          int tuvLostDefender = TuvUtils.getTuv(m_defendingWaitingToDie, defender, defenderCosts, gameData);
          defenderLostTuv += tuvLostDefender;
          m_attackingWaitingToDie.clear();
          m_defendingWaitingToDie.clear();
          // kill any suicide attackers (veqryn)
          final Predicate<Unit> attackerSuicide = PredicateBuilder
              .of(Matches.unitIsSuicide())
              .andIf(isBombingRun, Matches.unitIsNotStrategicBomber())
              .build();
          if (attackingUnits.stream().anyMatch(attackerSuicide)) {
            final List<Unit> suicideUnits = CollectionUtils.getMatches(attackingUnits, Matches.unitIsSuicide());
            attackingUnits.removeAll(suicideUnits);
            remove(suicideUnits, bridge, battleSite);
            tuvLostAttacker = TuvUtils.getTuv(suicideUnits, attacker, attackerCosts, gameData);
            attackerLostTuv += tuvLostAttacker;
          }
          if (defendingUnits.stream().anyMatch(Matches.unitIsSuicide())) {
            final List<Unit> suicideUnits = CollectionUtils.getMatches(defendingUnits, Matches.unitIsSuicide());
            defendingUnits.removeAll(suicideUnits);
            remove(suicideUnits, bridge, battleSite);
            tuvLostDefender = TuvUtils.getTuv(suicideUnits, defender, defenderCosts, gameData);
            defenderLostTuv += tuvLostDefender;
          }
        }
      });
    }
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 3148193405425861565L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
          return;
        }
        makeBattle(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = 3148193405425861565L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (shouldFightAirBattle() && !shouldEndBattleDueToMaxRounds()) {
          return;
        }
        end(bridge);
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -5408702756335356985L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!isOver && canAttackerRetreat()) {
          attackerRetreat(bridge);
        }
      }
    });
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -7819137222487595113L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!isOver && canDefenderRetreat()) {
          defenderRetreat(bridge);
        }
      }
    });
    final IExecutable loop = new IExecutable() {
      private static final long serialVersionUID = -5408702756335356985L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        pushFightLoopOnStack(false);
      }
    };
    steps.add(new IExecutable() {
      private static final long serialVersionUID = -4136481765101946944L;

      @Override
      public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
        if (!isOver) {
          m_steps = determineStepStrings(false);
          final ITripleADisplay display = getDisplay(bridge);
          display.listBattleSteps(battleId, m_steps);
          round++;
          // continue fighting
          // the recursive step
          // this should always be the base of the stack
          // when we execute the loop, it will populate the stack with the battle steps
          if (!m_stack.isEmpty()) {
            throw new IllegalStateException("Stack not empty:" + m_stack);
          }
          m_stack.push(loop);
        }
      }
    });
    return steps;
  }

  private List<String> determineStepStrings(final boolean showFirstRun) {
    final List<String> steps = new ArrayList<>();
    if (showFirstRun) {
      steps.add(AIR_BATTLE);
      steps.add(INTERCEPTORS_LAUNCH);
    }
    steps.add(ATTACKERS_FIRE);
    steps.add(DEFENDERS_FIRE);
    if (canAttackerRetreat()) {
      steps.add(ATTACKERS_WITHDRAW);
    }
    if (canDefenderRetreat()) {
      steps.add(DEFENDERS_WITHDRAW);
    }
    // steps.add(BOMBERS_TO_TARGETS);
    return steps;
  }

  private static void recordUnitsWereInAirBattle(final Collection<Unit> units, final IDelegateBridge bridge) {
    final CompositeChange wasInAirBattleChange = new CompositeChange();
    for (final Unit u : units) {
      wasInAirBattleChange.add(ChangeFactory.unitPropertyChange(u, true, TripleAUnit.WAS_IN_AIR_BATTLE));
    }
    if (!wasInAirBattleChange.isEmpty()) {
      bridge.addChange(wasInAirBattleChange);
    }
  }

  private void makeBattle(final IDelegateBridge bridge) {
    // record who was in this battle first, so that they do not take part in any ground battles
    if (isBombingRun) {
      recordUnitsWereInAirBattle(attackingUnits, bridge);
      recordUnitsWereInAirBattle(defendingUnits, bridge);
    }
    // so as of right now, Air Battles are created before both normal battles and strategic bombing raids
    // once completed, the air battle will create a strategic bombing raid, if that is the purpose of those aircraft
    // however, if the purpose is a normal battle, it will have already been created by the battle tracker / combat move
    // so we do not have to create normal battles, only bombing raids
    // setup new battle here
    if (isBombingRun) {
      final Collection<Unit> bombers = CollectionUtils.getMatches(attackingUnits, Matches.unitIsStrategicBomber());
      if (!bombers.isEmpty()) {
        Map<Unit, Set<Unit>> targets = null;
        final Collection<Unit> enemyTargetsTotal = battleSite.getUnits().getMatches(
            Matches.enemyUnit(bridge.getPlayerId(), gameData)
                .and(Matches.unitCanBeDamaged())
                .and(Matches.unitIsBeingTransported().negate()));
        for (final Unit unit : bombers) {
          final Collection<Unit> enemyTargets =
              CollectionUtils.getMatches(enemyTargetsTotal, Matches.unitIsLegalBombingTargetBy(unit));
          if (!enemyTargets.isEmpty()) {
            Unit target = null;
            if (enemyTargets.size() > 1
                && Properties.getDamageFromBombingDoneToUnitsInsteadOfTerritories(gameData)) {
              while (target == null) {
                target =
                    getRemote(bridge).whatShouldBomberBomb(battleSite, enemyTargets, Collections.singletonList(unit));
              }
            } else if (!enemyTargets.isEmpty()) {
              target = enemyTargets.iterator().next();
            }
            if (target != null) {
              targets = new HashMap<>();
              targets.put(target, new HashSet<>(Collections.singleton(unit)));
            }
            battleTracker.addBattle(new RouteScripted(battleSite), Collections.singleton(unit), true, attacker,
                bridge, null, null, targets, true);
          }
        }
        final IBattle battle = battleTracker.getPendingBattle(battleSite, true, null);
        final IBattle dependent = battleTracker.getPendingBattle(battleSite, false, BattleType.NORMAL);
        if (dependent != null) {
          battleTracker.addDependency(dependent, battle);
        }
        final IBattle dependentAirBattle = battleTracker.getPendingBattle(battleSite, false, BattleType.AIR_BATTLE);
        if (dependentAirBattle != null) {
          battleTracker.addDependency(dependentAirBattle, battle);
        }
      }
    }
  }

  private void end(final IDelegateBridge bridge) {
    // record it
    final String text;
    if (!attackingUnits.isEmpty()) {
      if (isBombingRun) {
        if (attackingUnits.stream().anyMatch(Matches.unitIsStrategicBomber())) {
          whoWon = WhoWon.ATTACKER;
          if (defendingUnits.isEmpty()) {
            battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          } else {
            battleResultDescription = BattleRecord.BattleResultDescription.WON_WITH_ENEMY_LEFT;
          }
          text = "Air Battle is over, the remaining bombers go on to their targets";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
        } else {
          whoWon = WhoWon.DRAW;
          battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, the bombers have all died";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, attacker);
        }
      } else {
        if (defendingUnits.isEmpty()) {
          whoWon = WhoWon.ATTACKER;
          battleResultDescription = BattleRecord.BattleResultDescription.WON_WITHOUT_CONQUERING;
          text = "Air Battle is over, the defenders have all died";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR_SUCCESSFUL, attacker);
        } else {
          whoWon = WhoWon.DRAW;
          battleResultDescription = BattleRecord.BattleResultDescription.STALEMATE;
          text = "Air Battle is over, neither side is eliminated";
          bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_STALEMATE, attacker);
        }
      }
    } else {
      whoWon = WhoWon.DEFENDER;
      battleResultDescription = BattleRecord.BattleResultDescription.LOST;
      text = "Air Battle is over, the attackers have all died";
      bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_FAILURE, attacker);
    }
    bridge.getHistoryWriter().addChildToEvent(text);
    battleTracker.getBattleRecords().addResultToBattle(attacker, battleId, defender, attackerLostTuv,
        defenderLostTuv, battleResultDescription, new BattleResults(this, gameData));
    getDisplay(bridge).battleEnd(battleId, "Air Battle over");
    isOver = true;
    battleTracker.removeBattle(AirBattle.this, bridge.getData());
  }

  void finishBattleAndRemoveFromTrackerHeadless(final IDelegateBridge bridge) {
    makeBattle(bridge);
    whoWon = WhoWon.ATTACKER;
    battleResultDescription = BattleRecord.BattleResultDescription.NO_BATTLE;
    battleTracker.getBattleRecords().removeBattle(attacker, battleId);
    isOver = true;
    battleTracker.removeBattle(AirBattle.this, bridge.getData());
  }

  private void attackerRetreat(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should
    // move during non combat to their landing site, or be scrapped if they can't find one.
    // retreat planes
    if (!attackingUnits.isEmpty()) {
      queryRetreat(false, bridge, Collections.singleton(battleSite));
    }
  }

  private void defenderRetreat(final IDelegateBridge bridge) {
    // planes retreat to the same square the battle is in, and then should
    // move during non combat to their landing site, or be scrapped if they can't find one.
    // retreat planes
    if (!defendingUnits.isEmpty()) {
      queryRetreat(true, bridge, Collections.singleton(battleSite));
    }
  }

  private void queryRetreat(final boolean defender, final IDelegateBridge bridge,
      final Collection<Territory> availableTerritories) {
    if (availableTerritories.isEmpty()) {
      return;
    }
    final Collection<Unit> units =
        defender ? new ArrayList<>(defendingUnits) : new ArrayList<>(attackingUnits);
    if (units.isEmpty()) {
      return;
    }
    final PlayerID retreatingPlayer = defender ? this.defender : attacker;
    final String text = retreatingPlayer.getName() + " retreat?";
    final String step = defender ? DEFENDERS_WITHDRAW : ATTACKERS_WITHDRAW;
    getDisplay(bridge).gotoBattleStep(battleId, step);
    final Territory retreatTo =
        getRemote(retreatingPlayer, bridge).retreatQuery(battleId, false, battleSite, availableTerritories, text);
    if (retreatTo != null && !availableTerritories.contains(retreatTo)) {
      log.log(Level.SEVERE, "Invalid retreat selection :" + retreatTo + " not in "
          + MyFormatter.defaultNamedToTextList(availableTerritories));
      return;
    }
    if (retreatTo != null) {
      if (!headless) {
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_RETREAT_AIR, attacker);
      }
      retreat(units, defender, bridge);
      final String messageShort = retreatingPlayer.getName() + " retreats";
      final String messageLong = retreatingPlayer.getName() + " retreats all units to " + retreatTo.getName();
      getDisplay(bridge).notifyRetreat(messageShort, messageLong, step, retreatingPlayer);
    }
  }

  private void retreat(final Collection<Unit> retreating, final boolean defender, final IDelegateBridge bridge) {
    if (!defender) {
      // we must remove any of these units from the land battle that follows (this comes before we remove them from this
      // battle, because
      // after we remove from this battle we are no longer blocking any battles)
      final Collection<IBattle> dependentBattles = battleTracker.getBlocked(AirBattle.this);
      removeFromDependents(retreating, bridge, dependentBattles, true);
    }
    final String transcriptText = MyFormatter.unitsToText(retreating) + (defender ? " grounded" : " retreated");
    final Collection<Unit> units = defender ? defendingUnits : attackingUnits;
    units.removeAll(retreating);
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(retreating));
    recordUnitsWereInAirBattle(retreating, bridge);
  }

  private void showBattle(final IDelegateBridge bridge) {
    final String title = "Air Battle in " + battleSite.getName();
    getDisplay(bridge).showBattle(battleId, battleSite, title, attackingUnits, defendingUnits, null, null, null,
        Collections.emptyMap(), attacker, defender, isAmphibious(), getBattleType(),
        Collections.emptySet());
    getDisplay(bridge).listBattleSteps(battleId, m_steps);
  }

  /**
   * Finds the maximum number of units that can intercept from a given territory including checking
   * on any air base requirements.
   */
  public static int getMaxInterceptionCount(final Territory t, final Collection<Unit> possible) {
    if (possible.stream().noneMatch(Matches.unitRequiresAirBaseToIntercept())) {
      return Integer.MAX_VALUE;
    }
    int result = 0;
    for (final Unit base : t.getUnits().getMatches(Matches.unitIsAirBase().and(Matches.unitIsNotDisabled()))) {
      final int baseMax = UnitAttachment.get(base.getType()).getMaxInterceptCount();
      if (baseMax == -1) {
        return Integer.MAX_VALUE;
      }
      result += baseMax;
    }
    return result;
  }

  class InterceptorsLaunch implements IExecutable {
    private static final long serialVersionUID = 4300406315014471768L;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      getInterceptors(bridge);
      if (!defendingUnits.isEmpty()) {
        m_intercept = true;
        // play a sound
        bridge.getSoundChannelBroadcaster().playSoundForAll(SoundPath.CLIP_BATTLE_AIR, attacker);
      }
    }

    private void getInterceptors(final IDelegateBridge bridge) {
      final boolean groundedPlanesRetreated;
      final Collection<Unit> interceptors;
      if (isBombingRun) {
        // if bombing run, ask who will intercept
        interceptors = getRemote(defender, bridge).selectUnitsQuery(battleSite,
            new ArrayList<>(defendingUnits), "Select Air to Intercept");
        groundedPlanesRetreated = false;
      } else {
        // if normal battle, we may choose to withdraw some air units (keep them grounded for both Air battle and the
        // subsequent normal
        // battle) instead of launching
        if (Properties.getAirBattleDefendersCanRetreat(gameData)) {
          interceptors = getRemote(defender, bridge).selectUnitsQuery(battleSite,
              new ArrayList<>(defendingUnits), "Select Air to Intercept");
          groundedPlanesRetreated = true;
        } else {
          // if not allowed to withdraw, we must commit all air
          interceptors = new ArrayList<>(defendingUnits);
          groundedPlanesRetreated = false;
        }
      }
      if (interceptors != null && (!defendingUnits.containsAll(interceptors)
          || interceptors.size() > getMaxInterceptionCount(battleSite, defendingUnits))) {
        throw new IllegalStateException("Interceptors choose from outside of available units");
      }
      final Collection<Unit> beingRemoved = new ArrayList<>(defendingUnits);
      defendingUnits.clear();
      if (interceptors != null) {
        beingRemoved.removeAll(interceptors);
        defendingUnits.addAll(interceptors);
      }
      getDisplay(bridge).changedUnitsNotification(battleId, defender, beingRemoved, null, null);
      if (groundedPlanesRetreated) {
        // this removes them from the subsequent normal battle. (do not use this for bombing battles)
        retreat(beingRemoved, true, bridge);
      }
      if (!attackingUnits.isEmpty()) {
        bridge.getHistoryWriter().addChildToEvent(attacker.getName() + " attacks with " + attackingUnits.size()
            + " units heading to " + battleSite.getName(), new ArrayList<>(attackingUnits));
      }
      if (!defendingUnits.isEmpty()) {
        bridge.getHistoryWriter().addChildToEvent(defender.getName() + " launches " + defendingUnits.size()
            + " interceptors out of " + battleSite.getName(), new ArrayList<>(defendingUnits));
      }
    }
  }

  class AttackersFire implements IExecutable {
    private static final long serialVersionUID = -5289634214875797408L;
    DiceRoll m_dice;
    CasualtyDetails m_details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!m_intercept) {
        return;
      }
      final IExecutable roll = new IExecutable() {
        private static final long serialVersionUID = 6579019987019614374L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_dice = DiceRoll.airBattle(attackingUnits, false, attacker, bridge, "Attackers Fire, ");
        }
      };
      final IExecutable calculateCasualties = new IExecutable() {
        private static final long serialVersionUID = 4556409970663527142L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_details = BattleCalculator.selectCasualties(ATTACKERS_FIRE, defender, defendingUnits, defendingUnits,
              attacker, attackingUnits, false, new ArrayList<>(), battleSite, null, bridge, ATTACKERS_FIRE,
              m_dice, true, battleId, false, m_dice.getHits(), true);
          m_defendingWaitingToDie.addAll(m_details.getKilled());
          markDamaged(m_details.getDamaged(), bridge);
        }
      };
      final IExecutable notifyCasualties = new IExecutable() {
        private static final long serialVersionUID = 4224354422817922451L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          notifyCasualties(battleId, bridge, ATTACKERS_FIRE, m_dice, defender, attacker, m_details);
        }
      };
      // push in reverse order of execution
      stack.push(notifyCasualties);
      stack.push(calculateCasualties);
      stack.push(roll);
    }
  }

  class DefendersFire implements IExecutable {
    private static final long serialVersionUID = -7277182945495744003L;
    DiceRoll m_dice;
    CasualtyDetails m_details;

    @Override
    public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
      if (!m_intercept) {
        return;
      }
      final IExecutable roll = new IExecutable() {
        private static final long serialVersionUID = 5953506121350176595L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_dice = DiceRoll.airBattle(defendingUnits, true, defender, bridge, "Defenders Fire, ");
        }
      };
      final IExecutable calculateCasualties = new IExecutable() {
        private static final long serialVersionUID = 6658309931909306564L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          m_details = BattleCalculator.selectCasualties(DEFENDERS_FIRE, attacker, attackingUnits, attackingUnits,
              defender, defendingUnits, false, new ArrayList<>(), battleSite, null, bridge, DEFENDERS_FIRE,
              m_dice, false, battleId, false, m_dice.getHits(), true);
          m_attackingWaitingToDie.addAll(m_details.getKilled());
          markDamaged(m_details.getDamaged(), bridge);
        }
      };
      final IExecutable notifyCasualties = new IExecutable() {
        private static final long serialVersionUID = 4461950841000674515L;

        @Override
        public void execute(final ExecutionStack stack, final IDelegateBridge bridge) {
          notifyCasualties(battleId, bridge, DEFENDERS_FIRE, m_dice, attacker, defender, m_details);
        }
      };
      // push in reverse order of execution
      stack.push(notifyCasualties);
      stack.push(calculateCasualties);
      stack.push(roll);
    }
  }

  private static Predicate<Unit> unitHasAirDefenseGreaterThanZero() {
    return u -> UnitAttachment.get(u.getType()).getAirDefense(u.getOwner()) > 0;
  }

  private static Predicate<Unit> unitHasAirAttackGreaterThanZero() {
    return u -> UnitAttachment.get(u.getType()).getAirAttack(u.getOwner()) > 0;
  }

  static Predicate<Unit> attackingGroundSeaBattleEscorts() {
    return Matches.unitCanAirBattle();
  }

  public static Predicate<Unit> defendingGroundSeaBattleInterceptors(final PlayerID attacker, final GameData data) {
    return PredicateBuilder.of(
        Matches.unitCanAirBattle())
        .and(Matches.unitIsEnemyOf(data, attacker))
        .and(Matches.unitWasInAirBattle().negate())
        .andIf(!Properties.getCanScrambleIntoAirBattles(data), Matches.unitWasScrambled().negate())
        .build();
  }

  /**
   * Returns a unit predicate that determines if it can potentially intercept including checking any
   * air base requirements.
   */
  public static Predicate<Unit> defendingBombingRaidInterceptors(final Territory territory, final PlayerID attacker,
      final GameData data) {
    final Predicate<Unit> canIntercept = PredicateBuilder.of(
        Matches.unitCanIntercept())
        .and(Matches.unitIsEnemyOf(data, attacker))
        .and(Matches.unitWasInAirBattle().negate())
        .andIf(!Properties.getCanScrambleIntoAirBattles(data), Matches.unitWasScrambled().negate())
        .build();
    final Predicate<Unit> airbasesCanIntercept = Matches.unitIsEnemyOf(data, attacker)
        .and(Matches.unitIsAirBase())
        .and(Matches.unitIsNotDisabled())
        .and(Matches.unitIsBeingTransported().negate());
    return u -> canIntercept.test(u)
        && (!Matches.unitRequiresAirBaseToIntercept().test(u)
            || Matches.territoryHasUnitsThatMatch(airbasesCanIntercept).test(territory));
  }

  static boolean territoryCouldPossiblyHaveAirBattleDefenders(final Territory territory, final PlayerID attacker,
      final GameData data, final boolean bombing) {
    final boolean canScrambleToAirBattle = Properties.getCanScrambleIntoAirBattles(data);
    final Predicate<Unit> defendingAirMatch = bombing ? defendingBombingRaidInterceptors(territory, attacker, data)
        : defendingGroundSeaBattleInterceptors(attacker, data);
    int maxScrambleDistance = 0;
    if (canScrambleToAirBattle) {
      for (final UnitType unitType : data.getUnitTypeList()) {
        final UnitAttachment ua = UnitAttachment.get(unitType);
        if (ua.getCanScramble() && maxScrambleDistance < ua.getMaxScrambleDistance()) {
          maxScrambleDistance = ua.getMaxScrambleDistance();
        }
      }
    } else {
      return territory.getUnits().anyMatch(defendingAirMatch);
    }
    // should we check if the territory also has an air base?
    return territory.getUnits().anyMatch(defendingAirMatch)
        || data.getMap().getNeighbors(territory, maxScrambleDistance).stream()
            .anyMatch(Matches.territoryHasUnitsThatMatch(defendingAirMatch));
  }

  static int getAirBattleRolls(final Collection<Unit> units, final boolean defending) {
    int rolls = 0;
    for (final Unit u : units) {
      rolls += getAirBattleRolls(u, defending);
    }
    return rolls;
  }

  static int getAirBattleRolls(final Unit unit, final boolean defending) {
    if (defending) {
      if (!unitHasAirDefenseGreaterThanZero().test(unit)) {
        return 0;
      }
    } else {
      if (!unitHasAirAttackGreaterThanZero().test(unit)) {
        return 0;
      }
    }
    return Math.max(0, (defending ? UnitAttachment.get(unit.getType()).getDefenseRolls(unit.getOwner())
        : UnitAttachment.get(unit.getType()).getAttackRolls(unit.getOwner())));
  }

  private void remove(final Collection<Unit> killed, final IDelegateBridge bridge, final Territory battleSite) {
    if (killed.size() == 0) {
      return;
    }
    final Collection<Unit> dependent = getDependentUnits(killed);
    killed.addAll(dependent);
    final Change killedChange = ChangeFactory.removeUnits(battleSite, killed);
    // m_killed.addAll(killed);
    final String transcriptText = MyFormatter.unitsToText(killed) + " lost in " + battleSite.getName();
    bridge.getHistoryWriter().addChildToEvent(transcriptText, new ArrayList<>(killed));
    bridge.addChange(killedChange);
    final Collection<IBattle> dependentBattles = battleTracker.getBlocked(AirBattle.this);
    removeFromDependents(killed, bridge, dependentBattles, false);
  }

  private static void notifyCasualties(final GUID battleId, final IDelegateBridge bridge, final String stepName,
      final DiceRoll dice, final PlayerID hitPlayer, final PlayerID firingPlayer, final CasualtyDetails details) {
    getDisplay(bridge).casualtyNotification(battleId, stepName, dice, hitPlayer, details.getKilled(),
        details.getDamaged(), Collections.emptyMap());
    // execute in a seperate thread to allow either player to click continue first.
    final Thread t = new Thread(() -> {
      try {
        getRemote(firingPlayer, bridge).confirmEnemyCasualties(battleId, "Press space to continue", hitPlayer);
      } catch (final Exception e) {
        log.log(Level.SEVERE, "Error during casualty notification", e);
      }
    }, "Click to continue waiter");
    t.start();
    getRemote(hitPlayer, bridge).confirmOwnCasualties(battleId, "Press space to continue");
    bridge.leaveDelegateExecution();
    Interruptibles.join(t);
    bridge.enterDelegateExecution();
  }

  private static void removeFromDependents(final Collection<Unit> units, final IDelegateBridge bridge,
      final Collection<IBattle> dependents, final boolean withdrawn) {
    for (final IBattle dependent : dependents) {
      dependent.unitsLostInPrecedingBattle(units, bridge, withdrawn);
    }
  }

  @Override
  public boolean isEmpty() {
    return attackingUnits.isEmpty();
  }

  @Override
  public void unitsLostInPrecedingBattle(final Collection<Unit> units,
      final IDelegateBridge bridge, final boolean withdrawn) {}
}
