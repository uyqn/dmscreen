package no.uyqn.dmscreen.dice;

import java.util.Arrays;
import no.uyqn.dmscreen.TestcontainersConfiguration;
import no.uyqn.dmscreen.dice.internal.RollRepository;
import org.assertj.core.api.Assertions;
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
        Assertions.assertThat(rollId).isNotNull();
        Assertions.assertThat(rollRepository.findById(rollId)).hasValueSatisfying(persistedRoll -> {
            Assertions.assertThat(persistedRoll.getId()).isEqualTo(roll.rollId());
            Assertions.assertThat(persistedRoll.getExpression())
                    .isEqualTo(roll.expression().toString());
            Assertions.assertThat(Arrays.stream(persistedRoll.getDice()).boxed().toList())
                    .isEqualTo(roll.dice());
            Assertions.assertThat(persistedRoll.getTotal()).isEqualTo(roll.total());
        });
    }
}
