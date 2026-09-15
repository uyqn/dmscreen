package no.uyqn.dmscreen.dice;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import org.jspecify.annotations.NonNull;

/**
 * Record representing dice expressions, e.g. d20, 2d6, 2d6+3, 1d8-1, 1d8+1d6+2.
 *
 * @param terms collection of {@link Term} e.g. 1d8+1d6 is a list of two terms 1d8, 1d6
 * @param modifier the modifier of the dice expression represented usually as the final number at the end
 */
public record DiceExpression(List<Term> terms, int modifier) {
    private static final Pattern PATTERN = Pattern.compile("\\d*d\\d+(\\+\\d*d\\d+)*([+-]\\d+)?");
    private static final Pattern MODIFIER = Pattern.compile("-?\\d+");

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
        var matcher = PATTERN.matcher(diceExpression);
        if (!matcher.matches()) {
            throw new InvalidDiceExpression(diceExpression + " is not a valid dice expression");
        }
        var tokens = diceExpression.replace("-", "+-").split("\\+");
        var modifiers = Arrays.stream(tokens)
                .filter(t -> MODIFIER.matcher(t).matches())
                .mapToInt(Integer::parseInt)
                .sum();
        var terms = Arrays.stream(tokens)
                .filter(t -> Term.PATTERN.matcher(t).matches())
                .map(Term::parse)
                .toList();
        return new DiceExpression(terms, modifiers);
    }

    @NonNull
    @Override
    public String toString() {
        var termsIterator = terms.iterator();
        var stringBuilder = new StringBuilder(termsIterator.next().toString());
        termsIterator.forEachRemaining(term -> stringBuilder.append("+").append(term));
        if (modifier == 0) {
            return stringBuilder.toString();
        }
        if (modifier > 0) {
            return stringBuilder.append("+").append(modifier).toString();
        }
        return stringBuilder.append(modifier).toString();
    }

    /**
     * A die expression term NdM
     *
     * @param count N
     * @param sides M
     */
    public record Term(int count, int sides) {
        private static final Pattern PATTERN = Pattern.compile("(?<count>\\d*)d(?<sides>\\d+)");

        public Term {
            if (count < 1 || count > 100) {
                throw new InvalidDiceExpression("count " + count + " must be between 1 and 100");
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
            var matcher = PATTERN.matcher(termExpression);
            if (!matcher.matches()) {
                throw new InvalidDiceExpression(termExpression + " is not a valid dice term");
            }
            var count = matcher.group("count");
            var sides = Integer.parseInt(matcher.group("sides"));
            return new Term(count.isEmpty() ? 1 : Integer.parseInt(count), sides);
        }

        @NonNull
        @Override
        public String toString() {
            return count + "d" + sides;
        }
    }
}
