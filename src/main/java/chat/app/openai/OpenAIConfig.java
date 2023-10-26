package chat.app.openai;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.OpenAIClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.List;
import java.util.Set;

@Configuration
public class OpenAIConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(OpenAIConfig.class);
    private static final String OPENAI_API_KEY = "api-key";

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @Value("${openai.api.baseurl}")
    private String openaiApiBaseUrl;

    @Value("${openai.api.endpoint}")
    private String openaiApiEndpoint;

    public OpenAIClient buildOpenAIClient() {
        return new OpenAIClientBuilder()
                .credential(new AzureKeyCredential(openaiApiKey))
                .endpoint(openaiApiBaseUrl)
                .buildClient();
    }

    public WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create();

        LOGGER.debug("Using url: '{}' key: '{}'", openaiApiBaseUrl, openaiApiKey);

        return WebClient.builder()
                .baseUrl(openaiApiBaseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(OPENAI_API_KEY, openaiApiKey)
                .filter(logRequest())
                .build();
    }

    public ChatRequest buildRequest(String prompt) {
        Message systemMessage =
                new Message("system", "You are an AI assistant that helps people find information.");
        Message userMessage =
                new Message("user", prompt);

        Set<String> stopSequence = null;

        return new ChatRequest(
                List.of(systemMessage, userMessage),
                800,
                0.7,
                0,
                0,
                0.95,
                stopSequence);
    }

    private ExchangeFilterFunction logRequest() {
        return (clientRequest, next) -> {
            LOGGER.debug("Request: '{}' URL: '{}' Headers: '{}'",
                    clientRequest.method(),
                    clientRequest.url(),
                    clientRequest.headers());
            return next.exchange(clientRequest);
        };
    }

    @Bean
    @Qualifier("openaiRestTemplate")
    public RestTemplate openaiRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.getInterceptors().add(((request, body, execution) -> {
            request.getHeaders().add("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.3");
            request.getHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            request.getHeaders().add(HttpHeaders.AUTHORIZATION, "Bearer " + openaiApiKey);
            request.getHeaders().add(OPENAI_API_KEY, openaiApiKey);
            return execution.execute(request, body);
        }));

        return restTemplate;
    }

}
