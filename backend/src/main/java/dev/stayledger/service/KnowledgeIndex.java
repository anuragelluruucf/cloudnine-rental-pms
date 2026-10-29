package dev.stayledger.service;

import dev.langchain4j.data.document.Metadata;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.embedding.onnx.allminilml6v2.AllMiniLmL6V2EmbeddingModel;
import dev.langchain4j.store.embedding.EmbeddingSearchRequest;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.stayledger.domain.KnowledgeChunk;
import dev.stayledger.domain.Property;
import dev.stayledger.repo.KnowledgeChunkRepository;
import jakarta.persistence.EntityManager;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import static dev.langchain4j.store.embedding.filter.MetadataFilterBuilder.metadataKey;

@Service
public class KnowledgeIndex {

    /**
     * Words that show up in almost every guest question. They do not make a
     * listing note relevant.
     */
    static final Set<String> STOPWORDS = Set.of(
            "what", "when", "where", "which", "that", "this", "with", "from", "your",
            "have", "does", "will", "about", "please", "into", "they", "them", "than",
            "then", "just", "some", "want", "need", "tell", "know", "like", "also",
            "would", "could", "should", "there", "their", "been", "were"
    );

    /** A paraphrase with no shared words has to be this close before it is used. */
    static final double PARAPHRASE_SCORE = 0.72;

    private final KnowledgeChunkRepository chunks;
    private final EntityManager entityManager;
    private final EmbeddingModel embeddingModel;
    private final Object lock = new Object();
    private volatile InMemoryEmbeddingStore<TextSegment> store = new InMemoryEmbeddingStore<>();

    public KnowledgeIndex(KnowledgeChunkRepository chunks, EntityManager entityManager) {
        this.chunks = chunks;
        this.entityManager = entityManager;
        this.embeddingModel = new AllMiniLmL6V2EmbeddingModel();
    }

    @EventListener(ApplicationReadyEvent.class)
    @Transactional(readOnly = true)
    public void onReady() {
        rebuild();
    }

    @Transactional
    public void syncProperty(Property property) {
        UUID propertyId = property.getId();
        String description = property.getDescription();
        String houseRules = property.getHouseRules();
        String amenities = property.getAmenities();
        String checkInInfo = property.getCheckInInfo();
        String checkOutInfo = property.getCheckOutInfo();
        String wifi = wifiText(property);
        String faq = property.getFaqNotes();

        chunks.deleteByPropertyId(propertyId);
        Property managed = entityManager.getReference(Property.class, propertyId);
        int position = 0;
        position = addPieces(managed, position, "Description", description);
        position = addPieces(managed, position, "House rules", houseRules);
        position = addPieces(managed, position, "Amenities", amenities);
        position = addPieces(managed, position, "Check-in", checkInInfo);
        position = addPieces(managed, position, "Check-out", checkOutInfo);
        position = addPieces(managed, position, "Wifi", wifi);
        addPieces(managed, position, "FAQ", faq);
        chunks.flush();
        rebuild();
    }

    @Transactional(readOnly = true)
    public void rebuild() {
        List<KnowledgeChunk> all = chunks.findAllWithProperty();
        synchronized (lock) {
            InMemoryEmbeddingStore<TextSegment> next = new InMemoryEmbeddingStore<>();
            for (KnowledgeChunk chunk : all) {
                TextSegment segment = TextSegment.from(
                        chunk.getContent(),
                        Metadata.from("propertyId", chunk.getProperty().getId().toString())
                );
                Embedding embedding = embeddingModel.embed(segment).content();
                next.add(embedding, segment);
            }
            this.store = next;
        }
    }

    public List<String> search(UUID propertyId, String question, int limit) {
        synchronized (lock) {
            if (store.isEmpty() || limit <= 0) {
                return List.of();
            }
            Embedding query = embeddingModel.embed(question).content();
            var result = store.search(EmbeddingSearchRequest.builder()
                    .queryEmbedding(query)
                    .maxResults(12)
                    .minScore(0.0)
                    .filter(metadataKey("propertyId").isEqualTo(propertyId.toString()))
                    .build());
            List<String> contentWords = contentTokens(question);
            return result.matches().stream()
                    .filter(match -> relevant(contentWords, match.score(), match.embedded().text()))
                    .map(match -> new Scored(
                            match.embedded().text(),
                            match.score() + (0.45 * lexical(question, match.embedded().text()))
                    ))
                    .sorted(Comparator.comparingDouble(Scored::score).reversed())
                    .limit(Math.min(limit, 2))
                    .map(Scored::text)
                    .toList();
        }
    }

    /**
     * A note is relevant when it shares a content word with the question.
     * Embedding score alone is not enough: nearby listing notes (a door code,
     * checkout, a description) score above zero for almost any question.
     * A paraphrase with no shared word is kept only at a high score, and only
     * when the question itself has no content words to match.
     */
    static boolean relevant(List<String> contentWords, double embeddingScore, String text) {
        if (contentWords.isEmpty()) {
            return embeddingScore >= PARAPHRASE_SCORE;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        for (String token : contentWords) {
            if (haystack.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private int addPieces(Property property, int position, String label, String text) {
        if (text == null || text.isBlank()) {
            return position;
        }
        for (String piece : split(text)) {
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setProperty(property);
            chunk.setSource(label);
            chunk.setPosition(position++);
            chunk.setContent(clip(label + ": " + piece));
            chunks.save(chunk);
        }
        return position;
    }

    static List<String> split(String text) {
        String normalized = text.replace('\r', '\n').trim();
        String[] paragraphs = normalized.split("\\n\\s*\\n");
        List<String> pieces = new ArrayList<>();
        for (String paragraph : paragraphs) {
            String flat = paragraph.replace('\n', ' ').replaceAll("\\s+", " ").trim();
            if (flat.isEmpty()) {
                continue;
            }
            if (flat.length() <= 480) {
                pieces.add(flat);
                continue;
            }
            int start = 0;
            while (start < flat.length()) {
                int end = Math.min(flat.length(), start + 480);
                pieces.add(flat.substring(start, end).trim());
                start = end;
            }
        }
        return pieces;
    }

    static List<String> contentTokens(String question) {
        String[] tokens = question.toLowerCase(Locale.ROOT).split("[^a-z0-9]+");
        List<String> kept = new ArrayList<>();
        for (String token : tokens) {
            if (token.length() >= 4 && !STOPWORDS.contains(token)) {
                kept.add(token);
            }
        }
        return kept;
    }

    static double lexical(String question, String text) {
        List<String> tokens = contentTokens(question);
        if (tokens.isEmpty()) {
            return 0;
        }
        String haystack = text.toLowerCase(Locale.ROOT);
        int hits = 0;
        for (String token : tokens) {
            if (haystack.contains(token)) {
                hits++;
            }
        }
        return (double) hits / tokens.size();
    }

    private static String wifiText(Property property) {
        if ((property.getWifiNetwork() == null || property.getWifiNetwork().isBlank())
                && (property.getWifiPassword() == null || property.getWifiPassword().isBlank())) {
            return null;
        }
        return "The wifi network name is " + empty(property.getWifiNetwork())
                + " and the wifi password is " + empty(property.getWifiPassword()) + ".";
    }

    private static String empty(String value) {
        return value == null || value.isBlank() ? "not listed" : value.trim();
    }

    private static String clip(String value) {
        if (value.length() <= 2000) {
            return value;
        }
        return value.substring(0, 2000);
    }

    private record Scored(String text, double score) {
    }
}
