package no.uyqn.dmscreen.dice;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import no.uyqn.dmscreen.TestcontainersConfiguration;
import no.uyqn.dmscreen.dice.internal.RollRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.modulith.test.ApplicationModuleTest;

@Import(TestcontainersConfiguration.class)
@ApplicationModuleTest
class DiceModuleTests {
    @Autowired
    private RollRepository rollRepository;

    @Autowired
    private Dice dice;

    @DisplayName("Roll is persisted")
    @Test
    void rollIsPersisted() {
        var diceExpression = DiceExpression.parse("d20");
        var roll = dice.roll(diceExpression);
        var rollId = roll.rollId();
        assertThat(rollId).isNotNull();
        assertThat(rollRepository.findById(rollId)).hasValueSatisfying(persistedRoll -> {
            assertThat(persistedRoll.getId()).isEqualTo(roll.rollId());
            assertThat(persistedRoll.getExpression())
                    .isEqualTo(roll.expression().toString());
            assertThat(Arrays.stream(persistedRoll.getDice()).boxed().toList()).isEqualTo(roll.dice());
            assertThat(persistedRoll.getTotal()).isEqualTo(roll.total());
        });
    }
}
