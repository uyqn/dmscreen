package no.uyqn.dmscreen.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import java.util.HashMap;
import java.util.List;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpPrompt;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class DungeonMasterPrompt {
    private final Resource template;

    public DungeonMasterPrompt(@Value("classpath:/prompts/dungeon_master.st") Resource template) {
        this.template = template;
    }

    @McpPrompt(
            name = "dungeon_master",
            description =
                    "Run the campaign as its dungeon master, following the dmscreen rules for state, dice and DM-only"
                            + " secrets")
    public McpSchema.GetPromptResult dungeonMaster(
            @McpArg(name = "tone", description = "Mood of the narration, e.g. grim or light. Omit for a neutral tone")
                    String tone) {
        var variables = new HashMap<String, Object>();
        variables.put("tone", tone == null || tone.isBlank() ? null : tone);
        var dungeonMasterPrompt =
                PromptTemplate.builder().resource(template).variables(variables).build();
        var textContent =
                McpSchema.TextContent.builder(dungeonMasterPrompt.render()).build();
        var promptMessage = McpSchema.PromptMessage.builder(McpSchema.Role.USER, textContent)
                .build();
        return McpSchema.GetPromptResult.builder(List.of(promptMessage)).build();
    }
}
