package no.uyqn.dmscreen.dice;

import java.util.Arrays;
import java.util.random.RandomGenerator;
import java.util.stream.IntStream;
import no.uyqn.dmscreen.dice.internal.Roll;
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

    private int[] rollTerms(DiceExpression expression) {
        return expression.terms().stream()
                .flatMapToInt(term -> IntStream.generate(() -> randomGenerator.nextInt(1, term.sides() + 1))
                        .limit(term.count()))
                .toArray();
    }

    private int[] rollTwice(DiceExpression expression) {
        var terms = expression.terms();
        if (terms.size() != 1) {
            throw new InvalidDiceExpression(expression + " is not a legal advantage/disadvantage roll");
        }
        var term = terms.getFirst();
        var count = term.count();
        if (count != 1) {
            throw new InvalidDiceExpression(expression + " is not a valid count for advantage/disadvantage roll");
        }
        var sides = term.sides();
        return new int[] {randomGenerator.nextInt(1, sides + 1), randomGenerator.nextInt(1, sides + 1)};
    }

    public RollResult roll(DiceExpression expression, Advantage advantage, String reason) {
        var dice = advantage == Advantage.NONE ? rollTerms(expression) : rollTwice(expression);
        var total = advantage.getTotal(dice) + expression.modifier();

        var roll = new Roll(expression, dice, advantage, total, reason);
        rollRepository.save(roll);
        return new RollResult(
                roll.getId(), expression, Arrays.stream(dice).boxed().toList(), advantage, total);
    }
}
