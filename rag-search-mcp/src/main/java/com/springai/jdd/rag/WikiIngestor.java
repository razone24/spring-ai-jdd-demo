package com.springai.jdd.rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Embeds the wiki at startup, one chunk per Markdown section. Chunk ids are derived from the file
 * content, so an unchanged file is never re-embedded and an edited file replaces its old chunks
 * instead of living next to them.
 */
@Component
public class WikiIngestor implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(WikiIngestor.class);
    private static final String SOURCE = "source";
    // Part of every chunk id: bump it when the chunking changes so existing chunks are rebuilt.
    private static final String CHUNKING_VERSION = "markdown-sections-v1";

    private final VectorStore vectorStore;
    private final ResourcePatternResolver resourceResolver;
    private final ChunkIndex chunkIndex;

    public WikiIngestor(VectorStore vectorStore, ResourcePatternResolver resourceResolver, ChunkIndex chunkIndex) {
        this.vectorStore = vectorStore;
        this.resourceResolver = resourceResolver;
        this.chunkIndex = chunkIndex;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        ingest();
    }

    void ingest() throws IOException {
        Set<String> currentFiles = new HashSet<>();
        for (Resource resource : resourceResolver.getResources("classpath:wiki/**/*.md")) {
            String fileHash = hashBytes(resource.getContentAsByteArray());
            currentFiles.add(fileHash);
            if (chunkIndex.exists(fileHash + "-0")) {
                log.info("Skipping already-ingested wiki file: {}", resource.getFilename());
                continue;
            }
            List<String> sections = MarkdownSections.split(resource.getContentAsString(StandardCharsets.UTF_8));
            List<Document> stamped = new ArrayList<>();
            for (int i = 0; i < sections.size(); i++) {
                stamped.add(new Document(fileHash + "-" + i, sections.get(i), Map.of(SOURCE, resource.getFilename())));
            }
            log.info("Ingesting {} chunks for {}", stamped.size(), resource.getFilename());
            vectorStore.add(stamped);
        }
        int removed = chunkIndex.retainOnlyFiles(currentFiles);
        if (removed > 0) {
            log.info("Removed {} stale chunks of changed or deleted wiki files", removed);
        }
    }

    private String hashBytes(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(CHUNKING_VERSION.getBytes(StandardCharsets.UTF_8));
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash).substring(0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
