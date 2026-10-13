package no.uyqn.dmscreen.mcp;

import static org.assertj.core.api.Assertions.assertThat;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientStreamableHttpTransport;
import io.modelcontextprotocol.spec.McpSchema;
import no.uyqn.dmscreen.TestcontainersConfiguration;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import tools.jackson.databind.ObjectMapper;

@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
abstract class McpEndToEndTest {
    protected McpSyncClient client;

    @Autowired
    protected ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    @BeforeEach
    void connect() {
        var transport = HttpClientStreamableHttpTransport.builder("http://localhost:" + port)
                .build();
        client = McpClient.sync(transport).build();
        client.initialize();
    }

    @AfterEach
    void disconnect() {
        client.closeGracefully();
    }

    protected <T> T structuredContentAs(McpSchema.CallToolResult result, Class<T> clazz) {
        return objectMapper.convertValue(result.structuredContent(), clazz);
    }

    protected String errorText(McpSchema.CallToolResult result) {
        assertThat(result.isError()).isTrue();
        return assertThat(result.content())
                .singleElement()
                .asInstanceOf(InstanceOfAssertFactories.type(McpSchema.TextContent.class))
                .extracting(McpSchema.TextContent::text)
                .asString()
                .actual();
    }
}
