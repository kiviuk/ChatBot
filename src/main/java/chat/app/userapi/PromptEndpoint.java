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

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

@RestController
public class PromptEndpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(PromptEndpoint.class);

    @Value("${openai.api.baseurl}")
    private String azureOpenaiApiBaseUrl;

    @Value("${openai.api.deployment}")
    private String gpt35DeploymentName;

    @Value("${openai.api.key}")
    private String azureOpenaiApiKey;

    @Value("${openai.api.temperature}")
    private double temperature;

    @Value("${openai.system-message2}")
    private String systemMessage;

    @Value("${openai.max-token-length}")
    private Integer maxTokenLength;

    private static final SecureRandom random = new SecureRandom();

    private static String generateRandomHash() {
        return new BigInteger(130, random).toString(32);
    }

    /**
     * Endpoint for POST /prompt requests. Accepts a user's prompt via JSON payload and generates
     * a response using the Azure's OpenAI GPT-3 model.
     *
     * @param prompt Non-null instance of {@link Request} representing the user's prompt.
     * @return {@link ResponseEntity<String>} with AI-generated responses as a single string.
     */
    @PostMapping(value = "/prompt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> prompt(@NonNull @RequestBody Request prompt) {

        if (prompt.prompt() == null || prompt.prompt().isEmpty()) {
            LOGGER.info("Received empty user prompt.");
            return ResponseEntity.ok("User prompt cannot be empty");
        }

        // Creating a list of messages including system message and user prompt.
        List<ChatMessage> gpt35ChatMessages = createGpt35ChatMessages(prompt);
        // With the created chat messages, ChatCompletionsOptions is initialized.
        ChatCompletionsOptions gpt35ChatCompletionsOptions = new ChatCompletionsOptions(gpt35ChatMessages);
        // Temperature is set for the chat completion options.
        gpt35ChatCompletionsOptions.setTemperature(temperature);
        // Create and initialize the OpenAIClient.
        OpenAIClient gpt35Client =
                createOpenAIClient(new AzureKeyCredential(getAzureOpenaiApiKey()), getAzureOpenaiApiBaseUrl());
        // Request chat completions from the OpenAI client.
        ChatCompletions gpt35ChatCompletions =
                gpt35Client.getChatCompletions(getAzureOpenaiDeployment(), gpt35ChatCompletionsOptions);
        // Extract the responses from ChatCompletions.
        List<String> gpt35ChatResponses = gpt35ChatResponses(gpt35ChatCompletions);

        // Convert response list to a single string and return.
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
        chatMessages.add(new ChatMessage(ChatRole.SYSTEM, systemMessage));
        String completePrompt = String.format("%s\n%s", generateRandomHash(), prompt.prompt());
        completePrompt = completePrompt.substring(0, Math.min(maxTokenLength, completePrompt.length()));
        chatMessages.add(new ChatMessage(ChatRole.USER, completePrompt));
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
        return System.getProperty(azureOpenaiApiKey);
    }
    public String getAzureOpenaiDeployment() {
        return System.getProperty(gpt35DeploymentName);
    }
    public String getAzureOpenaiApiBaseUrl() {
        return System.getProperty(azureOpenaiApiBaseUrl);
    }

}