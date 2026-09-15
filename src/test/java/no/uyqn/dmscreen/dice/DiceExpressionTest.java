package no.uyqn.dmscreen.dice;

import java.util.List;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class DiceExpressionTest {
    @DisplayName("Throws when terms are null or empty")
    @Test
    void throwsWhenTermsAreNullOrEmpty() {
        Assertions.assertThatThrownBy(() -> new DiceExpression(null)).isInstanceOf(InvalidDiceExpression.class);
        Assertions.assertThatThrownBy(() -> new DiceExpression(List.of())).isInstanceOf(InvalidDiceExpression.class);
    }

    @DisplayName("Throws with invalid counts and/or sides")
    @ParameterizedTest
    @CsvSource({"0, 20", "-2, 20", "101, 20", "1, 1", "1, -20", "1, 1001"})
    void throwsWithInvalidCountsOrSides(int counts, int sides) {
        Assertions.assertThatThrownBy(() -> new DiceExpression.Term(counts, sides))
                .isInstanceOf(InvalidDiceExpression.class);
    }

    @DisplayName("DiceExpression.Term#parse parses correct count and sides")
    @ParameterizedTest
    @CsvSource({"d20, 1, 20", "2d6, 2, 6", "1d8, 1, 8", "1d6, 1, 6"})
    void parseTerm(String expression, int count, int sides) {
        var term = DiceExpression.Term.parse(expression);

        Assertions.assertThat(term.count()).isEqualTo(count);
        Assertions.assertThat(term.sides()).isEqualTo(sides);
    }

    @DisplayName("DiceExpression.Term#toString is of canonical form")
    @ParameterizedTest
    @CsvSource({"d20, 1d20", "2d6, 2d6", "1d8, 1d8", "1d6, 1d6"})
    void termCanonicalForm(String expression, String canonicalForm) {
        var term = DiceExpression.Term.parse(expression);
        Assertions.assertThat(term.toString()).isEqualTo(canonicalForm);
    }

    @DisplayName("DiceExpression#parse parses correctly size of the terms and modifiers")
    @ParameterizedTest
    @CsvSource({"d20, 1, 0", "2d6, 1, 0", "2d6+3, 1, 3", "1d8-1, 1, -1", "1d8+1d6+2, 2, 2"})
    void parseDiceExpression(String expression, int size, int modifier) {
        var diceExpression = DiceExpression.parse(expression);

        Assertions.assertThat(diceExpression.terms().size()).isEqualTo(size);
        Assertions.assertThat(diceExpression.modifier()).isEqualTo(modifier);
    }

    @DisplayName("DiceExpression#toString is of canonical form")
    @ParameterizedTest
    @CsvSource({"d20, 1d20", "2d6, 2d6", "2d6+3, 2d6+3", "1d8-1, 1d8-1", "1d8+1d6+2, 1d8+1d6+2"})
    void diceExpressionCanonicalForm(String expression, String canonicalForm) {
        var diceExpression = DiceExpression.parse(expression);
        Assertions.assertThat(diceExpression.toString()).isEqualTo(canonicalForm);
    }

    @DisplayName("DiceExpression#parse throws InvalidDiceException for invalid expressions")
    @ParameterizedTest
    @ValueSource(strings = {"d0", "0d6", "2d", "101d6", "1d1001", "abc"})
    void invalidDiceExpressions(String expression) {
        Assertions.assertThatThrownBy(() -> DiceExpression.parse(expression)).isInstanceOf(InvalidDiceExpression.class);
    }
}
