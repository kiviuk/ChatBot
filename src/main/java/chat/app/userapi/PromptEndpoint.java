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

@RestController
public class PromptEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptEndpoint.class);

    @Value("${openai.api.baseurl}")
    private String azureOpenaiApiBaseUrl;

    @Value("${openai.api.deployment}")
    private String gpt35DeploymentName;

    @Value("${openai.api.temperature}")
    private double temperature;

    @PostMapping(value = "/prompt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> prompt(@NonNull @RequestBody Request prompt) {

        if (prompt.prompt() == null) {
            LOGGER.info("Received null user prompt.");
            return ResponseEntity.ok("User prompt cannot be null");
        }

        List<ChatMessage> gpt35ChatMessages = createGpt35ChatMessages(prompt);
        ChatCompletionsOptions gpt35ChatCompletionsOptions = new ChatCompletionsOptions(gpt35ChatMessages);
        gpt35ChatCompletionsOptions.setTemperature(temperature);

        OpenAIClient gpt35Client =
                createOpenAIClient(new AzureKeyCredential(getAzureOpenaiApiKey()), azureOpenaiApiBaseUrl);

        ChatCompletions gpt35ChatCompletions =
                gpt35Client.getChatCompletions(gpt35DeploymentName, gpt35ChatCompletionsOptions);

        List<String> gpt35ChatResponses = gpt35ChatResponses(gpt35ChatCompletions);

        return ResponseEntity.ok(String.join(System.lineSeparator(), gpt35ChatResponses));
    }

    private OpenAIClient createOpenAIClient(AzureKeyCredential keyCredential, String endpoint) {
        return new OpenAIClientBuilder()
                .credential(keyCredential)
                .endpoint(endpoint)
                .buildClient();
    }

    private List<ChatMessage> createGpt35ChatMessages(Request prompt) {
        List<ChatMessage> chatMessages = new ArrayList<>();
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
        return chatMessages;
    }

    private List<String> gpt35ChatResponses(ChatCompletions chatCompletions) {
        List<String> responses = new ArrayList<>();

        for (ChatChoice choice : chatCompletions.getChoices()) {
            ChatMessage message = choice.getMessage();
            LOGGER.info("Index: {}, Chat Role: {}.\n", choice.getIndex(), message.getRole());
            LOGGER.info("Message:");
            LOGGER.info(message.getContent());
            responses.add(message.getContent());
        }

        return responses;
    }

    public String getAzureOpenaiApiKey() {
        return System.getProperty("OPENAI_API_KEY");
    }

}