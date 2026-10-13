package no.uyqn.dmscreen.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.HashMap;
import no.uyqn.dmscreen.dice.Advantage;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class RollDiceToolTests extends McpEndToEndTest {
    @Test
    void isListedWithSchema() {
        var tools = client.listTools().tools();
        assertThat(tools)
                .filteredOn(tool -> tool.name().equals("roll_dice"))
                .singleElement()
                .satisfies(tool -> {
                    assertThat(tool.inputSchema())
                            .extractingByKey("properties")
                            .asInstanceOf(InstanceOfAssertFactories.MAP)
                            .containsOnlyKeys("expression", "advantage", "reason")
                            .allSatisfy((_, schema) -> assertThat(schema)
                                    .asInstanceOf(InstanceOfAssertFactories.MAP)
                                    .extractingByKey("description")
                                    .asString()
                                    .isNotBlank());
                    assertThat(tool.inputSchema())
                            .extractingByKey("required")
                            .asInstanceOf(InstanceOfAssertFactories.LIST)
                            .containsExactlyInAnyOrder("expression", "reason");
                    assertThat(tool.outputSchema()).isNotNull();
                });
    }

    private McpSchema.CallToolResult rollDice(String expression, Advantage advantage) {
        var arguments = new HashMap<String, Object>();
        arguments.put("expression", expression);
        if (advantage != null) {
            arguments.put("advantage", advantage);
        }
        arguments.put("reason", "test");
        return client.callTool(McpSchema.CallToolRequest.builder("roll_dice")
                .arguments(arguments)
                .build());
    }

    private McpSchema.CallToolResult rollDice(String expression) {
        return rollDice(expression, null);
    }

    @Test
    void rollsAndReturnsStructuredResult() {
        var callToolResult = rollDice("2d6+1");
        var result = structuredContentAs(callToolResult, RollDiceTool.RollDiceResult.class);
        assertThat(result.expression()).isEqualTo("2d6+1");
        assertThat(result.advantage()).isEqualTo(Advantage.NONE);
        assertThat(result.dice()).hasSize(2).allSatisfy(die -> assertThat(die).isBetween(1, 6));
        assertThat(result.total())
                .isEqualTo(result.dice().stream().mapToInt(Integer::intValue).sum() + 1);
    }

    @Test
    void reportsInvalidExpressionAsToolError() {
        var callToolResult = rollDice("1000000d6");
        assertThat(errorText(callToolResult)).contains("between 1 and 100");
    }

    @Test
    void rollsWithAdvantage() {
        var callToolAdvantageResult = rollDice("d20", Advantage.ADVANTAGE);
        var advantageResult = structuredContentAs(callToolAdvantageResult, RollDiceTool.RollDiceResult.class);
        assertThat(advantageResult.dice()).hasSize(2);
        assertThat(advantageResult.advantage()).isEqualTo(Advantage.ADVANTAGE);
        assertThat(advantageResult.total())
                .isEqualTo(advantageResult.dice().stream()
                        .mapToInt(Integer::intValue)
                        .max()
                        .orElseThrow());
    }

    @Test
    void rollsWithDisadvantage() {
        var callToolDisadvantageResult = rollDice("d20", Advantage.DISADVANTAGE);
        var disadvantageResult = structuredContentAs(callToolDisadvantageResult, RollDiceTool.RollDiceResult.class);
        assertThat(disadvantageResult.dice()).hasSize(2);
        assertThat(disadvantageResult.advantage()).isEqualTo(Advantage.DISADVANTAGE);
        assertThat(disadvantageResult.total())
                .isEqualTo(disadvantageResult.dice().stream()
                        .mapToInt(Integer::intValue)
                        .min()
                        .orElseThrow());
    }

    @Test
    void rejectsAdvantageOnMultipleDice() {
        var callToolResult = rollDice("2d6", Advantage.ADVANTAGE);
        assertThat(errorText(callToolResult)).contains("advantage");
    }
}
