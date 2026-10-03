package com.springai.jdd.rag;

import java.util.Set;

/**
 * What the vector store already holds, by chunk id ({@code <fileHash>-<chunkIndex>}).
 * Lets the ingestor skip unchanged files and drop chunks of files that changed or disappeared.
 */
public interface ChunkIndex {

    boolean exists(String chunkId);

    int retainOnlyFiles(Set<String> fileHashes);
}
