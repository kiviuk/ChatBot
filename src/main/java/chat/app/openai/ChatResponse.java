package chat.app.openai;

import java.util.List;

public record ChatResponse(List<Choice> choices) {
}
