package chat.app.openai;


import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Set;

public record ChatRequest(List<Message> messages,
                          @JsonProperty("max_tokens")int maxTokens,
                          double temperature,
                          @JsonProperty("frequency_penalty") double frequencyPenalty,
                          @JsonProperty("presence_penalty") double presencePenalty,
                          @JsonProperty("top_p") double topP,
                          @JsonProperty("stop") Set<String> stopSequence) {
}

