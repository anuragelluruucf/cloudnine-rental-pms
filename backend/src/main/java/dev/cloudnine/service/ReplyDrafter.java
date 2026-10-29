package dev.cloudnine.service;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.cloudnine.config.CloudNineProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ReplyDrafter {

    private static final Logger log = LoggerFactory.getLogger(ReplyDrafter.class);

    private final CloudNineProperties properties;

    public ReplyDrafter(CloudNineProperties properties) {
        this.properties = properties;
    }

    public String draft(String propertyName, String guestName, String question, List<String> snippets) {
        String key = properties.getLlm().getApiKey();
        if (key != null && !key.isBlank()) {
            try {
                OpenAiChatModel model = OpenAiChatModel.builder()
                        .apiKey(key.trim())
                        .baseUrl(properties.getLlm().getBaseUrl())
                        .modelName(properties.getLlm().getModel())
                        .build();
                return model.chat(prompt(propertyName, guestName, question, snippets));
            } catch (RuntimeException ex) {
                log.warn("Chat model failed, using the offline draft: {}", ex.getMessage());
            }
        }
        return offline(propertyName, guestName, question, snippets);
    }

    public boolean usingRemoteModel() {
        String key = properties.getLlm().getApiKey();
        return key != null && !key.isBlank();
    }

    static String offline(String propertyName, String guestName, String question, List<String> snippets) {
        String notes = snippets.isEmpty()
                ? "- No listing notes matched this question."
                : snippets.stream().map(snippet -> "- " + snippet).reduce((a, b) -> a + "\n" + b).orElse("-");
        return """
                Hello %s,

                Thanks for your question about %s: "%s"

                Here's what the listing notes say:
                %s

                This is a draft for the host to review before it's sent.
                """.formatted(guestName.trim(), propertyName.trim(), question.trim(), notes);
    }

    private static String prompt(String propertyName, String guestName, String question, List<String> snippets) {
        String notes = snippets.isEmpty() ? "(no listing notes matched)" : String.join("\n", snippets);
        return """
                You draft replies for a vacation-rental host. Use only the listing notes below.
                If the notes do not answer the question, say you are not sure and that the host will confirm.
                Do not invent wifi passwords, door codes, or policies.
                Keep it under 120 words. Plain sentences.

                Listing: %s
                Guest name: %s
                Question: %s

                Listing notes:
                %s
                """.formatted(propertyName, guestName, question, notes);
    }
}
