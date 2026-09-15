package no.uyqn.dmscreen.dice;

import java.util.Random;
import java.util.random.RandomGenerator;
import java.util.stream.Stream;
import no.uyqn.dmscreen.dice.internal.Roll;
import no.uyqn.dmscreen.dice.internal.RollRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class DiceTest {
    private RollRepository mockRepository() {
        var repository = Mockito.mock(RollRepository.class);
        Mockito.when(repository.save(Mockito.any(Roll.class))).thenAnswer((invocation) -> invocation.getArgument(0));
        return repository;
    }

    private Dice createSeededDice() {
        return new Dice(mockRepository(), new Random(67));
    }

    @DisplayName("Seeded roll is deterministic")
    @ParameterizedTest
    @CsvSource({
        "d20, NONE",
        "2d6, NONE",
        "2d6+3, NONE",
        "1d8-1, NONE",
        "1d8+1d6+2, NONE",
        "d20, ADVANTAGE",
        "d20, DISADVANTAGE"
    })
    void seededRollIsDeterministic(String expression, Advantage advantage) {
        var diceExpression = DiceExpression.parse(expression);
        var roll1 = createSeededDice().roll(diceExpression, advantage);
        var roll2 = createSeededDice().roll(diceExpression, advantage);

        Assertions.assertThat(roll1).isEqualTo(roll2);
    }

    @DisplayName("Every result within bounds and every face appears")
    @ParameterizedTest
    @CsvSource({"d6, 10000", "d20, 10000"})
    void everyResultWithinBoundsAndEveryFaceAppears(String expression, int count) {
        var diceExpression = DiceExpression.parse(expression);
        var dice = createSeededDice();

        var rolls = Stream.generate(() -> dice.roll(diceExpression).total())
                .limit(count)
                .toList();
        var sides = diceExpression.terms().getFirst().sides();

        Assertions.assertThat(rolls).allMatch(roll -> roll >= 1 && roll <= sides);
        for (int i = 0; i < sides; i++) {
            Assertions.assertThat(rolls).contains(i + 1);
        }
    }

    private Dice mockedDice() {
        var random = Mockito.mock(RandomGenerator.class);
        Mockito.when(random.nextInt(1, 21)).thenReturn(7, 15);
        return new Dice(mockRepository(), random);
    }

    private RollResult assertAdvantage(Advantage advantage) {
        var dice = mockedDice();
        var diceExpression = DiceExpression.parse("d20");
        var roll = dice.roll(diceExpression, advantage);
        Assertions.assertThat(roll.dice().size()).isEqualTo(2);
        return roll;
    }

    @DisplayName("Roll with advantage keeps the higher number")
    @Test
    void advantageKeepsHigher() {
        var roll = assertAdvantage(Advantage.ADVANTAGE);
        Assertions.assertThat(roll.total()).isEqualTo(15);
    }

    @DisplayName("Roll with disadvantage keeps the lower number")
    @Test
    void disadvantageKeepsLower() {
        var roll = assertAdvantage(Advantage.DISADVANTAGE);
        Assertions.assertThat(roll.total()).isEqualTo(7);
    }
}
