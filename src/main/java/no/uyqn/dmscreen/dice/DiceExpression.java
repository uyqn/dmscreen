package no.uyqn.dmscreen.dice;

import java.util.List;

/**
 * Record representing dice expressions, e.g. d20, 2d6, 2d6+3, 1d8-1, 1d8+1d6+2.
 *
 * @param terms collection of {@link Term} e.g. 1d8+1d6 is a list of two terms 1d8, 1d6
 * @param modifier the modifier of the dice expression represented usually as the final number at the end
 */
public record DiceExpression(List<Term> terms, int modifier) {
    public DiceExpression {
        if (terms == null || terms.isEmpty()) {
            throw new InvalidDiceExpression("Dice expression must at least include 1 term");
        }
        terms = List.copyOf(terms);
    }

    public DiceExpression(List<Term> terms) {
        this(terms, 0);
    }

    /**
     * Parses a string to {@link DiceExpression}
     *
     * @param diceExpression string representation of the dice expression
     * @return {@link DiceExpression}
     */
    public static DiceExpression parse(String diceExpression) {
        throw new UnsupportedOperationException("Not implemented");
    }

    /**
     * A die expression term NdM
     *
     * @param count N
     * @param sides M
     */
    public record Term(int count, int sides) {
        public Term {
            if (count < 1 || count > 100) {
                throw new InvalidDiceExpression("counts " + count + " must be between 1 and 100");
            }
            if (sides < 2 || sides > 1_000) {
                throw new InvalidDiceExpression("sides " + sides + " must be between 2 and 1 000");
            }
        }

        /**
         * Parses a string to {@link Term}
         *
         * @param termExpression string presentation of a single term of {@link DiceExpression}
         * @return {@link Term}
         */
        public static Term parse(String termExpression) {
            throw new UnsupportedOperationException("Not implemeted");
        }
    }
}
