package no.uyqn.dmscreen.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.HashMap;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;

class DungeonMasterPromptTests extends McpEndToEndTest {
    @Test
    void isListed() {
        var prompts = client.listPrompts().prompts();
        assertThat(prompts)
                .filteredOn(prompt -> prompt.name().equals("dungeon_master"))
                .singleElement()
                .satisfies(prompt -> {
                    assertThat(prompt.arguments()).singleElement().satisfies(argument -> {
                        assertThat(argument.name()).isEqualTo("tone");
                        assertThat(argument.description()).isNotBlank();
                        assertThat(argument.required()).isFalse();
                    });
                });
    }

    private McpSchema.GetPromptResult dungeonMasterPrompt(String tone) {
        var arguments = new HashMap<String, Object>();
        if (tone != null) {
            arguments.put("tone", tone);
        }
        var promptRequest = McpSchema.GetPromptRequest.builder("dungeon_master")
                .arguments(arguments)
                .build();
        return client.getPrompt(promptRequest);
    }

    private String text(McpSchema.GetPromptResult promptResult) {
        return assertThat(promptResult.messages())
                .singleElement()
                .satisfies(message -> assertThat(message.role()).isEqualTo(McpSchema.Role.USER))
                .extracting(McpSchema.PromptMessage::content)
                .asInstanceOf(InstanceOfAssertFactories.type(McpSchema.TextContent.class))
                .extracting(McpSchema.TextContent::text)
                .actual();
    }

    @Test
    void rendersWithTone() {
        var prompt = dungeonMasterPrompt("grim");
        var text = text(prompt);
        assertThat(text).contains("request_roll", "roll_dice", "recap_campaign", "grim");
    }

    @Test
    void rendersWithoutTone() {
        var prompt = dungeonMasterPrompt(null);
        var text = text(prompt);
        assertThat(text)
                .contains("request_roll", "roll_dice", "recap_campaign")
                .doesNotContain("{", "Run the game in a");
    }

    @Test
    void rendersWithBlankTone() {
        var blankPrompt = dungeonMasterPrompt("");
        var blankText = text(blankPrompt);

        var nullPrompt = dungeonMasterPrompt(null);
        var nullText = text(nullPrompt);

        assertThat(blankText).isEqualTo(nullText);
    }

    @Test
    void serverInstructionsMentionTools() {
        var serverInstructions = client.getServerInstructions();
        assertThat(serverInstructions).isNotBlank().contains("Never assume", "call a tool");
    }
}
