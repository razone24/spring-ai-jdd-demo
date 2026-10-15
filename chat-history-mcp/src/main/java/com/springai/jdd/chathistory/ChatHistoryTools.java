package com.springai.jdd.chathistory;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class ChatHistoryTools {

    private static final int TOP_K = 3;
    private static final String EMBEDDING_MODEL = "embedding_model";

    private final VectorStore vectorStore;
    private final ObjectMapper objectMapper;
    private final double similarityThreshold;
    private final String embeddingModel;

    public ChatHistoryTools(VectorStore vectorStore,
                            ObjectMapper objectMapper,
                            @Value("${chat-history.similarity-threshold}") double similarityThreshold,
                            @Value("${llm.embedding-model}") String embeddingModel) {
        this.vectorStore = vectorStore;
        this.objectMapper = objectMapper;
        this.similarityThreshold = similarityThreshold;
        this.embeddingModel = embeddingModel;
    }

    @Tool(name = "recordChatHistory", description = """
            Save a question and its final answer into the semantic cache so that identical or similar \
            questions asked later can be answered instantly without redoing the work. \
            Returns the ID of the stored record. Call this once, after you have a good answer.""")
    public String recordChatHistory(@ToolParam(description = "The user's original question") String question,
                                    @ToolParam(description = "The final answer given to the user") String answer) {
        String id = UUID.randomUUID().toString();
        Document doc = new Document(id, question, Map.of(
                "answer", answer,
                "timestamp", Instant.now().toString(),
                EMBEDDING_MODEL, embeddingModel));
        vectorStore.add(List.of(doc));
        return id;
    }

    @Tool(name = "searchChatHistory", description = """
            Look in the semantic cache for previously answered questions that mean the same thing as the \
            current one, so a cached answer can be reused instead of recomputed. \
            Returns matching question/answer pairs with their timestamp and similarity score (0..1, higher is closer).""")
    public String searchChatHistory(@ToolParam(description = "The question to look up") String question) {
        List<Document> results = vectorStore.similaritySearch(SearchRequest.builder()
                                                                           .query(question)
                                                                           .topK(TOP_K)
                                                                           .similarityThreshold(similarityThreshold)
                                                                           .filterExpression(new FilterExpressionBuilder()
                                                                                   .eq(EMBEDDING_MODEL, embeddingModel)
                                                                                   .build())
                                                                           .build());
        List<Map<String, Object>> mapped = results.stream()
                                                  .map(d -> Map.<String, Object>of(
                                                          "question", d.getText(),
                                                          "answer", d.getMetadata().getOrDefault("answer", ""),
                                                          "timestamp", d.getMetadata().getOrDefault("timestamp", ""),
                                                          "score", d.getScore() != null ? d.getScore() : 0.0))
                                                  .toList();
        return objectMapper.writeValueAsString(mapped);
    }
}
