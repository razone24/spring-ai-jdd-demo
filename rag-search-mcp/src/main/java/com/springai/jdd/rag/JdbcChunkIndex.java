package com.springai.jdd.rag;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class JdbcChunkIndex implements ChunkIndex {

    private final JdbcTemplate jdbcTemplate;
    private final String table;

    public JdbcChunkIndex(JdbcTemplate jdbcTemplate,
                          @Value("${spring.ai.vectorstore.pgvector.table-name}") String table) {
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
    }

    @Override
    public boolean exists(String chunkId) {
        Boolean found = jdbcTemplate.queryForObject(
                "select exists(select 1 from " + table + " where id = ?)", Boolean.class, chunkId);
        return Boolean.TRUE.equals(found);
    }

    @Override
    public int retainOnlyFiles(Set<String> fileHashes) {
        String[] hashes = fileHashes.toArray(String[]::new);
        return jdbcTemplate.update(
                "delete from " + table + " where split_part(id, '-', 1) <> all (?)", (Object) hashes);
    }
}
