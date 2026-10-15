package com.springai.jdd.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Postgres full-text search over the same table the vector store writes. Embeddings are good at
 * meaning and bad at exact tokens — a person's name, a product, an id — which is where this helps.
 * Any of the query's words may match (stop words dropped, words stemmed); more matches rank higher.
 */
@Component
public class KeywordSearch {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;
    private final String table;

    public KeywordSearch(JdbcTemplate jdbcTemplate,
                         ObjectMapper objectMapper,
                         @Value("${spring.ai.vectorstore.pgvector.table-name}") String table) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = objectMapper;
        this.table = table;
    }

    public List<Passage> search(String query, int limit) {
        String sql = """
                with terms as (select replace(plainto_tsquery('english', ?)::text, '&', '|')::tsquery as query)
                select id, content, metadata::text as metadata,
                       ts_rank(to_tsvector('english', content), terms.query) as rank
                from %s, terms
                where to_tsvector('english', content) @@ terms.query
                order by rank desc
                limit ?""".formatted(table);
        return jdbcTemplate.query(sql, (row, index) -> new Passage(row.getString("id"),
                                                                    row.getString("content"),
                                                                    sourceOf(row.getString("metadata")),
                                                                    null),
                                  query, limit);
    }

    private String sourceOf(String metadata) {
        JsonNode node = objectMapper.readTree(metadata == null ? "{}" : metadata);
        return node.path("source").asString("unknown");
    }
}
