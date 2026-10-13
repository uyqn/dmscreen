package no.uyqn.dmscreen.mcp;

import no.uyqn.dmscreen.dice.Dice;
import no.uyqn.dmscreen.dice.DiceExpression;
import no.uyqn.dmscreen.dice.RollResult;
import org.springframework.ai.mcp.annotation.McpTool;
import org.springframework.ai.mcp.annotation.McpToolParam;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RollDiceTool {
    private final Dice dice;

    @Autowired
    public RollDiceTool(Dice dice) {
        this.dice = dice;
    }

    @McpTool(
            name = "roll_dice",
            description = "Rolls a dice for the DM, monster or NPC. For player rolls, use request_roll")
    public RollResult rollDice(
            @McpToolParam(
                            description =
                                    "dice expression e.g. 2d6+1 (roll 6 sided dice twice, add 1 to the result as a"
                                            + " modifier)")
                    String diceExpression) {
        var parsedDiceExpression = DiceExpression.parse(diceExpression);
        return dice.roll(parsedDiceExpression);
    }
}
