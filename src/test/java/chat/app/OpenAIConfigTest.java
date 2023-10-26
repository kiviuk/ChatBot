package chat.app;

import chat.app.openai.OpenAIConfig;
import chat.app.userapi.BotResponse;
import lombok.SneakyThrows;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.web.reactive.function.client.WebClient;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.MockResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties =
        {
                "openai.api.key=testKey", "openai.api.baseurl=/testBaseURL", "openai.api.endpoint=/testEndpoint"
        }
)
public class OpenAIConfigTest {

    @Value("${openai.api.endpoint}")
    private String openaiApiEndpoint;

    @Autowired
    OpenAIConfig openAIConfig;

    private static MockWebServer mockWebServer;

    @BeforeAll
    static void setUp() throws Exception {
        mockWebServer = new MockWebServer();
        mockWebServer.start();
    }

    @AfterAll
    static void tearDown() throws Exception {
        mockWebServer.shutdown();
    }

    @SneakyThrows
    @Test
    public void testWebClientBean() {

        BotResponse apiBotResponse = new BotResponse("ok");

        WebClient openaiWebClient = openAIConfig.buildWebClient();

        assertNotNull(openaiWebClient);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(apiBotResponse);
        mockWebServer.enqueue(new MockResponse().setBody(jsonStr));

        String baseUrl = String.format("http://localhost:%s", mockWebServer.getPort());

        WebClient webClient = openaiWebClient.mutate().baseUrl(baseUrl).build();

        String response = webClient.post().uri(openaiApiEndpoint).retrieve().bodyToMono(String.class).block();
        assertEquals("{\"response\":\"ok\"}", response);
    }
}