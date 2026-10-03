package com.springai.jdd.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Hybrid retrieval: semantic (pgvector) and keyword (Postgres full-text) search over the same wiki
 * table, merged by reciprocal rank fusion.
 */
@Component
public class RagSearchTools {

    private static final int TOP_K = 4;
    private static final int CANDIDATES = 8;

    private final VectorStore vectorStore;
    private final KeywordSearch keywordSearch;
    private final ObjectMapper objectMapper;

    public RagSearchTools(VectorStore vectorStore, KeywordSearch keywordSearch, ObjectMapper objectMapper) {
        this.vectorStore = vectorStore;
        this.keywordSearch = keywordSearch;
        this.objectMapper = objectMapper;
    }

    @Tool(name = "searchKnowledgeBase",
          description = """
                  Search the internal JDD 2026 knowledge base (wiki) — by meaning and by exact words such as names. \
                  The wiki covers the venue and practical information (Wi-Fi, badges, food, parking, getting there, \
                  recordings, after-party, code of conduct, contacts) and the speakers' profiles and talk. \
                  Returns the most relevant passages with their source document and similarity score.""")
    public String searchKnowledgeBase(@ToolParam(description = "What to look for, phrased as a question or keywords")
                                      String query) {
        List<Passage> semantic = vectorStore.similaritySearch(SearchRequest.builder()
                                                                            .query(query)
                                                                            .topK(CANDIDATES)
                                                                            .build())
                                            .stream()
                                            .map(RagSearchTools::passageOf)
                                            .toList();
        List<Passage> keyword = keywordSearch.search(query, CANDIDATES);
        List<Map<String, Object>> results = HybridRanking.fuse(semantic, keyword, TOP_K)
                                                         .stream()
                                                         .map(RagSearchTools::resultOf)
                                                         .toList();
        return objectMapper.writeValueAsString(results);
    }

    private static Passage passageOf(Document document) {
        return new Passage(document.getId(),
                           document.getText(),
                           String.valueOf(document.getMetadata().getOrDefault("source", "unknown")),
                           document.getScore());
    }

    private static Map<String, Object> resultOf(Passage passage) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("text", passage.text());
        result.put("source", passage.source());
        result.put("score", passage.similarity() == null ? "keyword match" : passage.similarity());
        return result;
    }
}
