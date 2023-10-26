package chat.app.userapi;

import chat.app.openai.*;
import chat.app.openai.Choice;
import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriComponentsBuilder;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@RestController
public class Endpoint {

    private static final Logger LOGGER = LoggerFactory.getLogger(Endpoint.class);
    private static final ResponseEntity<ChatResponse> USER_PROMPT_CANNOT_BE_NULL =
            ResponseEntity.badRequest().body(
                    new ChatResponse(
                            List.of(
                                    new Choice(1, new Message("","User request cannot be null"))
                            )
                    )
            );

    private final OpenAIConfig openAIConfig;
    private final RestTemplate restTemplate;

    @Value("${openai.api.baseurl}")
    private String openaiBaseUrl;

    @Value("${openai.api.endpoint}")
    private String openaiEndpoint;

    @Autowired
    public Endpoint(OpenAIConfig openAIConfig, @Qualifier("openaiRestTemplate") RestTemplate restTemplate) {
        this.openAIConfig = openAIConfig;
        this.restTemplate = restTemplate;
    }

    /**
     * Accepts a chat request and processes it.
     *
     * @param request The request object containing the chat details.
     * @return The response entity indicating the status and result of the processing.
     */
    @PostMapping("/ama")
    public ResponseEntity<ChatResponse> ama(@NonNull @RequestBody Request humanoidRequest){
        if (humanoidRequest.prompt() == null) {
            LOGGER.info("Received null user chat prompt.");
            return USER_PROMPT_CANNOT_BE_NULL;  // Returns HTTP 400
        }

        LOGGER.info("Received prompt: " + humanoidRequest.prompt());

        var openAIRequest = openAIConfig.buildRequest(humanoidRequest.prompt());

        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(openaiBaseUrl + "/" + openaiEndpoint)
                .queryParam("api-version", "2023-07-01-preview");

        LOGGER.debug("URI ========> {}", builder.toUriString());

        HttpEntity<ChatRequest> entity = new HttpEntity<>(openAIRequest);

        return restTemplate.postForEntity(builder.toUriString(), entity, ChatResponse.class);

    }

    @PostMapping(value = "/prompt", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> prompt(@NonNull @RequestBody Request prompt) {

        if (prompt.prompt() == null) {
            LOGGER.info("Received null user chat prompt.");
            return ResponseEntity.ok("User request cannot be null");  // Returns HTTP 400
        }

        OpenAIClient client = openAIConfig.buildOpenAIClient();

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
        chatCompletionsOptions.setTemperature(0.2);
        ChatCompletions chatCompletions =
                client.getChatCompletions("gpt35-deployment", chatCompletionsOptions);

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


    @PostMapping("/chat")
    public ResponseEntity<BotResponse> acceptRequest(@NonNull @RequestBody Request request) {

        if (request.prompt() == null) {
            LOGGER.info("Received null user chat request.");
            return ResponseEntity.ok(new BotResponse("No RESPONSE"));  // Returns HTTP 400
        }

        LOGGER.info("Received chat request: " + request);

        return this.callOpenAI(request).block();
                //.doOnNext(response -> LOGGER.info("Bot response ready to be sent to user: " + response));
    }

    /**
     * Calls the OpenAI API with the given user request.
     *
     * @param humanoidRequest The user request object containing the chat details.
     */
    @SneakyThrows
    private Mono<ResponseEntity<BotResponse>> callOpenAI(Request humanoidRequest) {
        var openAIRequest = openAIConfig.buildRequest(humanoidRequest.prompt());
        var body = BodyInserters.fromValue(openAIRequest);

        ObjectMapper objectMapper = new ObjectMapper();
        String jsonStr = objectMapper.writeValueAsString(openAIRequest);
        LOGGER.debug(jsonStr);

        return openAIConfig
                .buildWebClient()
                .post()
                .uri(openaiEndpoint)
                .body(body)
                .retrieve()
                .toEntity(BotResponse.class);
                //.doOnSubscribe(subscription -> LOGGER.info("Calling URI {} with payload: {}", openaiEndpoint, body))
                //.doOnError(err -> LOGGER.error("Error", err))
                //.onErrorResume(err -> Mono.just(ResponseEntity.ok(new BotResponse("Ouch@ " + err.getMessage()))));

    }

}
