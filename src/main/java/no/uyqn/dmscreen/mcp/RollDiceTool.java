package no.uyqn.dmscreen.mcp;

import java.util.List;
import java.util.UUID;
import no.uyqn.dmscreen.dice.Advantage;
import no.uyqn.dmscreen.dice.Dice;
import no.uyqn.dmscreen.dice.DiceExpression;
import no.uyqn.dmscreen.dice.RollResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class RollDiceTool {
    private final Dice dice;

    public RollDiceTool(Dice dice) {
        this.dice = dice;
    }

    @McpTool(
            name = "roll_dice",
            description = "Roll dice for the DM, monster or NPC. For player rolls, use request_roll",
            generateOutputSchema = true)
    RollDiceResult rollDice(
            @McpToolParam(
                            description =
                                    "Dice expression e.g. 2d6+1 (roll 6 sided dice twice, add 1 to the result as a"
                                            + " modifier)")
                    String expression,
            @McpToolParam(
                            description =
                                    "ADVANTAGE or DISADVANTAGE on a single die such as 1d20 or 1d6. Omit for a normal"
                                            + " roll.",
                            required = false)
                    Advantage advantage,
            @McpToolParam(description = "Reason for rolling the dice") String reason) {
        var diceExpression = DiceExpression.parse(expression);
        var result = dice.roll(diceExpression, advantage == null ? Advantage.NONE : advantage, reason);
        return RollDiceResult.from(result);
    }

    record RollDiceResult(UUID rollId, String expression, List<Integer> dice, Advantage advantage, int total) {
        static RollDiceResult from(RollResult rollResult) {
            return new RollDiceResult(
                    rollResult.rollId(),
                    rollResult.expression().toString(),
                    rollResult.dice(),
                    rollResult.advantage(),
                    rollResult.total());
        }
    }
}
