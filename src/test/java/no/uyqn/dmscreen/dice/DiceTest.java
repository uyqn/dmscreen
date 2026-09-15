package no.uyqn.dmscreen.dice;

import java.util.Random;
import no.uyqn.dmscreen.dice.internal.Roll;
import no.uyqn.dmscreen.dice.internal.RollRepository;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mockito;

class DiceTest {
    private Dice createSeededDice() {
        var repository = Mockito.mock(RollRepository.class);
        Mockito.when(repository.save(Mockito.any(Roll.class))).thenAnswer((invocation) -> invocation.getArgument(0));
        return new Dice(repository, new Random(67));
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
}
