package no.uyqn.dmscreen.dice;

import java.util.random.RandomGenerator;
import no.uyqn.dmscreen.dice.internal.RollRepository;
import org.springframework.stereotype.Service;

@Service
public class Dice {
    private final RollRepository rollRepository;
    private final RandomGenerator randomGenerator;

    public Dice(RollRepository rollRepository, RandomGenerator randomGenerator) {
        this.rollRepository = rollRepository;
        this.randomGenerator = randomGenerator;
    }

    public RollResult roll(DiceExpression expression) {
        return roll(expression, Advantage.NONE);
    }

    public RollResult roll(DiceExpression expression, String reason) {
        return roll(expression, Advantage.NONE, reason);
    }

    public RollResult roll(DiceExpression expression, Advantage advantage) {
        return roll(expression, advantage, null);
    }

    public RollResult roll(DiceExpression expression, Advantage advantage, String reason) {
        throw new UnsupportedOperationException("Not implemented");
    }
}
