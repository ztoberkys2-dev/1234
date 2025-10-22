package com.example.wordsearch;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;

import static org.junit.jupiter.api.Assertions.*;

public class IndexerSearcherTest {

    @Test
    void indexAndSearchDocx() throws Exception {
        Path tempRoot = Files.createTempDirectory("wordsearch-test-");
        Path docs = tempRoot.resolve("docs");
        Path index = tempRoot.resolve("index");
        Files.createDirectories(docs);
        Files.createDirectories(index);

        // Create a DOCX file
        Path doc1 = docs.resolve("hello-world.docx");
        createDocx(doc1, "Привіт світ! Це тестовий документ для пошуку.");

        Path doc2 = docs.resolve("notes.docx");
        createDocx(doc2, "Word search demo: Lucene і POI працюють разом.");

        Indexer idx = new Indexer();
        idx.indexFolders(List.of(docs), index, true);

        try (Searcher s = new Searcher(index)) {
            var results = s.search("пошуку", 5, null, null);
            assertFalse(results.isEmpty(), "Expected at least one result for query");
            boolean foundDoc1 = results.stream().anyMatch(r -> r.getPath().endsWith("hello-world.docx"));
            assertTrue(foundDoc1, "Should find hello-world.docx");
        }
    }

    private static void createDocx(Path path, String text) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            p.createRun().setText(text);
            try (var os = Files.newOutputStream(path)) {
                doc.write(os);
            }
        }
    }
}
