package games.strategy.triplea.delegate;

import games.strategy.engine.data.Change;
import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.TechnologyFrontier;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.data.changefactory.ChangeFactory;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.TechAbilityAttachment;
import games.strategy.triplea.attachments.TechAttachment;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.function.Function;
import lombok.AllArgsConstructor;
import org.triplea.java.collections.IntegerMap;

/** A collection of methods for tracking which players have which technology advances. */
@AllArgsConstructor
public class TechTracker {
  private final GameData data;

  public int getAirDefenseBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getAirDefenseBonus, type, player);
  }

  public int getAirAttackBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getAirAttackBonus, type, player);
  }

  public int getMovementBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getMovementBonus, type, player);
  }

  public int getAttackBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getAttackBonus, type, player);
  }

  public int getAttackRollsBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getAttackRollsBonus, type, player);
  }

  public int getDefenseBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getDefenseBonus, type, player);
  }

  public int getDefenseRollsBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getDefenseRollsBonus, type, player);
  }

  public int getRadarBonus(GamePlayer player, UnitType type) {
    return getSumOfBonuses(TechAbilityAttachment::getRadarBonus, type, player);
  }

  public boolean canBlitz(GamePlayer player, UnitType type) {
    return getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BLITZ, type, player);
  }

  public boolean canBombard(GamePlayer player, UnitType type) {
    return getUnitAbilitiesGained(TechAbilityAttachment.ABILITY_CAN_BOMBARD, type, player);
  }

  private int getSumOfBonuses(
      final Function<TechAbilityAttachment, IntegerMap<UnitType>> mapper,
      final UnitType type,
      final GamePlayer player) {
    return TechAbilityAttachment.sumIntegerMap(mapper, type, getCurrentTechAdvances(player));
  }

  private boolean getUnitAbilitiesGained(
      final String filterForAbility, final UnitType unitType, final GamePlayer player) {
    return getCurrentTechAdvances(player).stream()
        .map(TechAbilityAttachment::get)
        .filter(Objects::nonNull)
        .map(TechAbilityAttachment::getUnitAbilitiesGained)
        .map(m -> m.get(unitType))
        .filter(Objects::nonNull)
        .flatMap(Collection::stream)
        .anyMatch(filterForAbility::equals);
  }

  private Collection<TechAdvance> getCurrentTechAdvances(GamePlayer player) {
    return getCurrentTechAdvances(player, data.getTechnologyFrontier());
  }

  /**
   * Returns what tech advances this player already has successfully researched (including ones that
   * may not be in their tech frontier).
   */
  public static Collection<TechAdvance> getCurrentTechAdvances(
      final GamePlayer gamePlayer, final TechnologyFrontier technologyFrontier) {
    final Collection<TechAdvance> techAdvances = new ArrayList<>();
    final TechAttachment attachment = TechAttachment.get(gamePlayer);
    // search all techs
    for (final TechAdvance ta : TechAdvance.getTechAdvances(technologyFrontier)) {
      if (ta.hasTech(attachment)) {
        techAdvances.add(ta);
      }
    }
    return techAdvances;
  }

  /**
   * Returns what tech categories are no longer available for this player, because all techs in them
   * have been successfully researched already.
   */
  public static Collection<TechnologyFrontier> getFullyResearchedPlayerTechCategories(
      final GamePlayer gamePlayer) {
    final Collection<TechnologyFrontier> technologyFrontiers = new ArrayList<>();
    final TechAttachment attachment = TechAttachment.get(gamePlayer);
    for (final TechnologyFrontier tf : TechAdvance.getPlayerTechCategories(gamePlayer)) {
      boolean has = true;
      for (final TechAdvance t : tf.getTechs()) {
        has = t.hasTech(attachment);
        if (!has) {
          break;
        }
      }
      if (has) {
        technologyFrontiers.add(tf);
      }
    }
    return technologyFrontiers;
  }

  /** Grants or adds a tech advance to a given player. */
  public static void addAdvance(
      final GamePlayer player, final IDelegateBridge bridge, final TechAdvance advance) {
    final Change attachmentChange;
    if (advance instanceof GenericTechAdvance
        && ((GenericTechAdvance) advance).getAdvance() == null) {
      attachmentChange =
          ChangeFactory.genericTechChange(TechAttachment.get(player), true, advance.getProperty());
    } else {
      attachmentChange =
          ChangeFactory.attachmentPropertyChange(
              TechAttachment.get(player), "true", advance.getProperty());
    }
    bridge.addChange(attachmentChange);
    advance.perform(player, bridge);
  }

  static void removeAdvance(
      final GamePlayer player, final IDelegateBridge bridge, final TechAdvance advance) {
    final Change attachmentChange;
    if (advance instanceof GenericTechAdvance) {
      if (((GenericTechAdvance) advance).getAdvance() == null) {
        attachmentChange =
            ChangeFactory.genericTechChange(
                TechAttachment.get(player), false, advance.getProperty());
      } else {
        attachmentChange =
            ChangeFactory.attachmentPropertyChange(
                TechAttachment.get(player), "false", advance.getProperty());
      }
    } else {
      attachmentChange =
          ChangeFactory.attachmentPropertyChange(
              TechAttachment.get(player), "false", advance.getProperty());
    }
    bridge.addChange(attachmentChange);
  }

  public static int getTechCost(final GamePlayer gamePlayer) {
    final TechAttachment ta = TechAttachment.get(gamePlayer);
    return ta.getTechCost();
  }

  public static boolean hasLongRangeAir(final GamePlayer player) {
    return TechAttachment.get(player).getLongRangeAir();
  }

  public static boolean hasHeavyBomber(final GamePlayer player) {
    return TechAttachment.get(player).getHeavyBomber();
  }

  public static boolean hasSuperSubs(final GamePlayer player) {
    return TechAttachment.get(player).getSuperSub();
  }

  public static boolean hasJetFighter(final GamePlayer player) {
    return TechAttachment.get(player).getJetPower();
  }

  public static boolean hasRocket(final GamePlayer player) {
    return TechAttachment.get(player).getRocket();
  }

  public static boolean hasIndustrialTechnology(final GamePlayer player) {
    return TechAttachment.get(player).getIndustrialTechnology();
  }

  public static boolean hasImprovedArtillerySupport(final GamePlayer player) {
    return TechAttachment.get(player).getImprovedArtillerySupport();
  }

  public static boolean hasParatroopers(final GamePlayer player) {
    return TechAttachment.get(player).getParatroopers();
  }

  public static boolean hasIncreasedFactoryProduction(final GamePlayer player) {
    return TechAttachment.get(player).getIncreasedFactoryProduction();
  }

  public static boolean hasAaRadar(final GamePlayer player) {
    return TechAttachment.get(player).getAaRadar();
  }
}
