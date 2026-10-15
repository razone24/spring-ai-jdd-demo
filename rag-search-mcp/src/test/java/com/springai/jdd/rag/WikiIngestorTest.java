package com.springai.jdd.rag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WikiIngestorTest {

    @Mock
    VectorStore vectorStore;

    @Mock
    ChunkIndex chunkIndex;

    @Test
    void producesChunksWithDeterministicIds() throws Exception {
        when(chunkIndex.exists(any())).thenReturn(false);

        List<String> firstIds = ingestAndCollectIds();
        reset(vectorStore);
        List<String> secondIds = ingestAndCollectIds();

        assertThat(firstIds).isNotEmpty().doesNotHaveDuplicates().isEqualTo(secondIds);
        assertThat(firstIds.getFirst()).matches("[a-f0-9]{16}-\\d+");
    }

    @Test
    void skipsFilesWhoseFirstChunkAlreadyExists() throws Exception {
        when(chunkIndex.exists(any())).thenReturn(true);

        newIngestor().ingest();

        verify(vectorStore, never()).add(any());
    }

    @Test
    void prunesChunksOfFilesThatAreNoLongerInTheWiki() throws Exception {
        when(chunkIndex.exists(any())).thenReturn(true);

        newIngestor().ingest();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Set<String>> kept = ArgumentCaptor.forClass(Set.class);
        verify(chunkIndex).retainOnlyFiles(kept.capture());
        assertThat(kept.getValue()).hasSize(3).allMatch(hash -> hash.matches("[a-f0-9]{16}"));
    }

    private List<String> ingestAndCollectIds() throws Exception {
        newIngestor().ingest();
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore, atLeastOnce()).add(captor.capture());
        return captor.getAllValues().stream().flatMap(List::stream).map(Document::getId).toList();
    }

    private WikiIngestor newIngestor() {
        lenient().when(chunkIndex.retainOnlyFiles(anySet())).thenReturn(0);
        return new WikiIngestor(vectorStore, new PathMatchingResourcePatternResolver(), chunkIndex);
    }
}
