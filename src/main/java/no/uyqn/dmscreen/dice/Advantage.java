package no.uyqn.dmscreen.dice;

import java.util.function.IntBinaryOperator;

public enum Advantage {
    NONE(null),
    ADVANTAGE(Math::max),
    DISADVANTAGE(Math::min);

    private final IntBinaryOperator operator;

    Advantage(IntBinaryOperator operator) {
        this.operator = operator;
    }

    public int select(int a, int b) {
        if (this == NONE) {
            throw new IllegalStateException(
                    NONE + " has no die to select: only one d20 is rolled without advantage or disadvantage");
        }
        return operator.applyAsInt(a, b);
    }
}
