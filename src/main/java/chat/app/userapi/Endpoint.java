package chat.app.userapi;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.ai.openai.models.*;
import com.azure.core.credential.AzureKeyCredential;
import lombok.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;

// https://ondrej-kvasnovsky.medium.com/how-to-stream-azure-openai-from-spring-api-to-web-client-74eb61db59fc
// https://learn.microsoft.com/en-us/java/api/overview/azure/ai-openai-readme?view=azure-java-preview
@RestController
public class Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint.class);
    private static final String DEPLOYMENT_OR_MODEL_NAME = "gpt35-deployment";
    private static final double TEMPERATURE = 0.2;

    @Value("${openai.api.baseurl}")
    private String openaiApiBaseUrl;

    @PostMapping(value = "/prompt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> prompt(@NonNull @RequestBody Request prompt) {

        if (prompt.prompt() == null) {
            LOGGER.info("Received null user chat prompt.");
            return ResponseEntity.ok("User request cannot be null");  // Returns HTTP 400
        }

        OpenAIClient client =  new OpenAIClientBuilder()
                    .credential(new AzureKeyCredential(getOpenaiApiKey()))
                    .endpoint(openaiApiBaseUrl)
                    .buildClient();


        var chatMessages = new ArrayList<ChatMessage>();
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM, """
                You are an effective Senior Software Engineer helping a junior software engineer in analysing and
                understanding complex technical problems.
                User gives you some message, and you need to generate a precise response.
                
                Instructions:
                - Be concise
                - Be pragmatic and go to the point
                - Be technical
                - Refrain from self referrals, such "I suggest..."
                """));
        chatMessages.add(new ChatMessage(ChatRole.USER, prompt.prompt()));

        ChatCompletionsOptions chatCompletionsOptions = new ChatCompletionsOptions(chatMessages);
        chatCompletionsOptions.setTemperature(TEMPERATURE);
        ChatCompletions chatCompletions =
                client.getChatCompletions(DEPLOYMENT_OR_MODEL_NAME, chatCompletionsOptions);

        List<String> responses = new ArrayList<>();

        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatMessage message = choice.getMessage();
            LOGGER.info("Index: {}, Chat Role: {}.\n", choice.getIndex(), message.getRole());
            LOGGER.info("Message:");
            LOGGER.info(message.getContent());
            responses.add(message.getContent());
        }

        return ResponseEntity.ok(String.join("\n", responses));
    }

    public String getOpenaiApiKey() {
        return System.getProperty("OPENAI_TOKEN");
    }

}
