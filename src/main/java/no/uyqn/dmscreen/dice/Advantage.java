package no.uyqn.dmscreen.dice;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

public enum Advantage {
    NONE(IntStream::sum),
    ADVANTAGE((stream) -> stream.max().orElseThrow(() -> new InvalidDiceExpression("Invalid advantage roll"))),
    DISADVANTAGE(stream -> stream.min().orElseThrow(() -> new InvalidDiceExpression("Invalid disadvantage roll")));

    private final Function<IntStream, Integer> operator;

    Advantage(Function<IntStream, Integer> operator) {
        this.operator = operator;
    }

    public int getTotal(int... rolls) {
        return operator.apply(Arrays.stream(rolls));
    }
}
