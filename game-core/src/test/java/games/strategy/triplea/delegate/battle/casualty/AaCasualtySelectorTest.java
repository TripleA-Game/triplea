package games.strategy.triplea.delegate.battle.casualty;

import static games.strategy.triplea.Constants.UNIT_ATTACHMENT_NAME;
import static games.strategy.triplea.delegate.MockDelegateBridge.whenGetRandom;
import static games.strategy.triplea.delegate.MockDelegateBridge.withValues;
import static games.strategy.triplea.delegate.battle.steps.MockGameData.givenGameData;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.description;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import games.strategy.engine.data.GameData;
import games.strategy.engine.data.GamePlayer;
import games.strategy.engine.data.Territory;
import games.strategy.engine.data.Unit;
import games.strategy.engine.data.UnitType;
import games.strategy.engine.delegate.IDelegateBridge;
import games.strategy.triplea.attachments.UnitAttachment;
import games.strategy.triplea.delegate.DiceRoll;
import games.strategy.triplea.delegate.battle.BattleState;
import games.strategy.triplea.delegate.data.CasualtyDetails;
import games.strategy.triplea.delegate.power.calculator.CombatValue;
import games.strategy.triplea.delegate.power.calculator.CombatValueBuilder;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AaCasualtySelectorTest {

  @Mock GamePlayer hitPlayer;
  @Mock GamePlayer aaPlayer;
  private UnitType aaUnitType;
  private UnitType damageableAaUnitType;
  private UnitType planeUnitType;
  private UnitType planeMultiHpUnitType;
  private GameData gameData;

  private List<Unit> givenAaUnits(final int quantity) {
    return aaUnitType.create(quantity, aaPlayer, true);
  }

  private List<Unit> givenDamageableAaUnits(final int quantity) {
    return damageableAaUnitType.create(quantity, aaPlayer, true);
  }

  private List<Unit> givenPlaneUnits(final int quantity) {
    return planeUnitType.create(quantity, hitPlayer, true);
  }

  private List<Unit> givenMultiHpPlaneUnits(final int quantity) {
    return planeMultiHpUnitType.create(quantity, hitPlayer, true);
  }

  private DiceRoll givenDiceRoll(final boolean... shots) {
    final int[] diceRolls = new int[shots.length];
    int hits = 0;
    for (int i = 0; i < shots.length; i++) {
      diceRolls[i] = shots[i] ? 0 : 2;
      hits += (shots[i] ? 1 : 0);
    }

    return new DiceRoll(diceRolls, hits, 1, false);
  }

  private CombatValue givenAaCombatValue() {
    return CombatValueBuilder.aaCombatValue()
        .friendlyUnits(List.of())
        .enemyUnits(List.of())
        .side(BattleState.Side.DEFENSE)
        .supportAttachments(List.of())
        .build();
  }

  @Nested
  class RandomCasualties {
    private IDelegateBridge bridge;

    @BeforeEach
    void initializeGameData() {
      gameData =
          givenGameData()
              .withEditMode(false)
              .withChooseAaCasualties(false)
              .withLowLuck(false)
              .withLowLuckAaOnly(false)
              .withRollAaIndividually(false)
              .withRandomAaCasualties(true)
              .build();
      bridge = mock(IDelegateBridge.class);
      when(bridge.getData()).thenReturn(gameData);

      aaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment aaUnitAttachment =
          new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
      aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

      damageableAaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment damageableAaUnitAttachment =
          new UnitAttachment("damageableAaUnitAttachment", aaUnitType, gameData);
      damageableAaUnitAttachment.setDamageableAa(true);
      damageableAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, damageableAaUnitAttachment);

      planeUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeUnitAttachment =
          new UnitAttachment("planeUnitAttachment", aaUnitType, gameData);
      planeUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeUnitAttachment);

      planeMultiHpUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeMultiHpUnitAttachment =
          new UnitAttachment("planeMultiHpUnitAttachment", aaUnitType, gameData);
      planeMultiHpUnitAttachment.setHitPoints(2);
      planeMultiHpUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeMultiHpUnitAttachment);
    }

    @Test
    void noHitsReturnEmptyCasualties() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              List.of(mock(Unit.class)),
              givenAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("No hits so no kills or damaged", details.size(), is(0));
    }

    @Test
    void hitsEqualToPlanesKillsAll() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenPlaneUnits(1),
              givenAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
    }

    @Test
    void hitsMoreThanPlanesKillsAll() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenPlaneUnits(1),
              givenAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("The plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat("Planes only have 1 hit point so no damages", details.getDamaged(), is(empty()));
    }

    @Test
    void oneHitAgainstTwoPlanesOnlyKillsOne() {

      whenGetRandom(bridge).thenAnswer(withValues(0));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenPlaneUnits(2),
              givenAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One of the two planes are killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
      verify(bridge, description("2 planes with only 1 hit"))
          .getRandom(eq(2), eq(1), any(), any(), anyString());
    }

    @Test
    void identicalDieRollsShouldStillKillPlanesEqualToHits() {

      whenGetRandom(bridge).thenAnswer(withValues(9, 9, 9, 9, 9));

      final List<Unit> planes = givenPlaneUnits(10);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              givenAaUnits(3),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "5 planes are killed even though the dice were all 9s", details.getKilled(), hasSize(5));
      assertThat(details.getDamaged(), is(empty()));
      verify(bridge, description("10 planes with only 5 hits"))
          .getRandom(eq(10), eq(5), any(), any(), anyString());
    }

    @Test
    void hitsEqualToPlanesMultiHpDamagesAndKillsAll() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenMultiHpPlaneUnits(1),
              givenDamageableAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("Plane was killed", details.getKilled(), hasSize(1));
      assertThat("Plane was also damaged", details.getDamaged(), hasSize(1));
    }

    @Test
    void hitsGreaterThanPlanesMultiHpDamagesAndKillsAll() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenMultiHpPlaneUnits(1),
              givenDamageableAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("Plane was killed", details.getKilled(), hasSize(1));
      assertThat("Plane was also damaged", details.getDamaged(), hasSize(1));
    }

    @Test
    void oneHitAgainstMultiHpPlaneOnlyDamagesIt() {

      whenGetRandom(bridge).thenAnswer(withValues(0));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenMultiHpPlaneUnits(1),
              givenDamageableAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("Plane is not killed", details.getKilled(), is(empty()));
      assertThat("Plane is damaged", details.getDamaged(), hasSize(1));
    }

    @Test
    void threeHitsAgainstTwoMultiHpPlanesKillsOneAndDamagesTheOther() {

      whenGetRandom(bridge).thenAnswer(withValues(0, 1, 2));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenMultiHpPlaneUnits(2),
              givenDamageableAaUnits(1),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("1 Plane is killed", details.getKilled(), hasSize(1));
      assertThat(
          "The killed plane is also damaged and the other plane takes 1 damage",
          details.getDamaged(),
          hasSize(2));
    }

    @Test
    void identicalDieRollsShouldStillKillAndDamagePlanesEqualToHits() {

      whenGetRandom(bridge).thenAnswer(withValues(6, 6, 6, 6, 6));

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenMultiHpPlaneUnits(7),
              givenDamageableAaUnits(3),
              mock(CombatValue.class),
              mock(CombatValue.class),
              "text",
              givenDiceRoll(true, true, true, true, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("5 planes should be killed or damaged", details.size(), is(5));
    }
  }

  @Nested
  class IndividualRollCasualties {
    private IDelegateBridge bridge;

    @BeforeEach
    void initializeGameData() {
      gameData =
          givenGameData()
              .withDiceSides(6)
              .withEditMode(false)
              .withChooseAaCasualties(false)
              .withLowLuck(false)
              .withLowLuckAaOnly(false)
              .withRollAaIndividually(true)
              .build();
      bridge = mock(IDelegateBridge.class);
      when(bridge.getData()).thenReturn(gameData);

      aaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment aaUnitAttachment =
          new UnitAttachment("aaUnitAttachment", aaUnitType, gameData);
      aaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, aaUnitAttachment);

      damageableAaUnitType = new UnitType("aaUnitType", gameData);
      final UnitAttachment damageableAaUnitAttachment =
          new UnitAttachment("damageableAaUnitAttachment", aaUnitType, gameData);
      damageableAaUnitAttachment.setDamageableAa(true);
      damageableAaUnitType.addAttachment(UNIT_ATTACHMENT_NAME, damageableAaUnitAttachment);

      planeUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeUnitAttachment =
          new UnitAttachment("planeUnitAttachment", aaUnitType, gameData);
      planeUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeUnitAttachment);

      planeMultiHpUnitType = new UnitType("planeUnitType", gameData);
      final UnitAttachment planeMultiHpUnitAttachment =
          new UnitAttachment("planeMultiHpUnitAttachment", aaUnitType, gameData);
      planeMultiHpUnitAttachment.setHitPoints(2);
      planeMultiHpUnitType.addAttachment(UNIT_ATTACHMENT_NAME, planeMultiHpUnitAttachment);
    }

    @Test
    void hitsEqualToPlanesKillsAll() {

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              givenPlaneUnits(1),
              givenAaUnits(1),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRoll(true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat("One plane was hit and killed", details.getKilled(), hasSize(1));
      assertThat(details.getDamaged(), is(empty()));
    }

    @Test
    void hitsLessThanPlanesKillsAccordingToTheRolledDice() {

      final List<Unit> planes = givenPlaneUnits(5);

      final CasualtyDetails details =
          AaCasualtySelector.getAaCasualties(
              planes,
              givenAaUnits(1),
              mock(CombatValue.class),
              givenAaCombatValue(),
              "text",
              givenDiceRoll(true, false, true, false, true),
              bridge,
              hitPlayer,
              UUID.randomUUID(),
              mock(Territory.class));

      assertThat(
          "1st, 3rd, and 5th plane were killed",
          details.getKilled(),
          is(List.of(planes.get(0), planes.get(2), planes.get(4))));
    }
  }
}
