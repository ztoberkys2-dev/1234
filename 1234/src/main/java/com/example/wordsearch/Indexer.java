package com.example.wordsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xwpf.usermodel.XWPFDocument;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;

public class Indexer {

    public void indexFolders(List<Path> sources, Path indexDir, boolean recreate) throws IOException {
        if (sources == null || sources.isEmpty()) {
            throw new IllegalArgumentException("No sources provided");
        }
        Files.createDirectories(indexDir);

        Analyzer analyzer = new StandardAnalyzer();
        Directory dir = FSDirectory.open(indexDir);
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        if (recreate) {
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        } else {
            iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        }

        try (IndexWriter writer = new IndexWriter(dir, iwc)) {
            for (Path src : sources) {
                if (src == null) continue;
                Path abs = src.toAbsolutePath();
                if (!Files.exists(abs) || !Files.isDirectory(abs)) {
                    System.err.println("Skipping non-directory: " + abs);
                    continue;
                }
                indexOneFolder(writer, abs);
            }
            writer.commit();
        }
    }

    private void indexOneFolder(IndexWriter writer, Path folder) throws IOException {
        try (var stream = Files.walk(folder)) {
            stream.filter(p -> Files.isRegularFile(p) && p.toString().toLowerCase().endsWith(".docx"))
                    .forEach(p -> {
                        try {
                            indexDoc(writer, p);
                        } catch (Exception e) {
                            System.err.println("Failed to index " + p + ": " + e.getMessage());
                        }
                    });
        }
    }

    private void indexDoc(IndexWriter writer, Path file) throws Exception {
        BasicFileAttributes attrs = Files.readAttributes(file, BasicFileAttributes.class);
        long modified = attrs.lastModifiedTime().toMillis();
        long size = attrs.size();

        ExtractedDoc content = extractDocx(file);

        Document doc = new Document();
        String filename = file.getFileName().toString();
        doc.add(new StringField("path", file.toAbsolutePath().toString(), Field.Store.YES));
        doc.add(new StringField("filename_kw", filename, Field.Store.YES));
        doc.add(new TextField("filename", filename, Field.Store.NO));
        // нижче — нормалізоване поле для нечутливого до регістру фільтра
        doc.add(new StringField("filename_lc_kw", filename.toLowerCase(java.util.Locale.ROOT), Field.Store.NO));

        if (content.title != null && !content.title.isBlank()) {
            doc.add(new TextField("title", content.title, Field.Store.YES));
        }
        doc.add(new LongPoint("modified", modified));
        doc.add(new StoredField("modified_store", modified));
        doc.add(new LongPoint("size", size));
        doc.add(new StoredField("size_store", size));

        if (content.text != null && !content.text.isBlank()) {
            doc.add(new TextField("content", content.text, Field.Store.NO));
            String preview = content.text.substring(0, Math.min(1000, content.text.length()));
            doc.add(new StoredField("preview", preview));
        }

        writer.updateDocument(new Term("path", file.toAbsolutePath().toString()), doc);
    }

    private ExtractedDoc extractDocx(Path file) throws Exception {
        try (InputStream is = Files.newInputStream(file); XWPFDocument doc = new XWPFDocument(OPCPackage.open(is))) {
            StringBuilder sb = new StringBuilder();
            doc.getParagraphs().forEach(p -> {
                String t = p.getText();
                if (t != null && !t.isBlank()) sb.append(t).append('\n');
            });
            doc.getTables().forEach(table -> table.getRows().forEach(row -> row.getTableCells().forEach(cell -> {
                String t = cell.getText();
                if (t != null && !t.isBlank()) sb.append(t).append('\n');
            })));

            String title = null;
            try {
                var cp = doc.getProperties().getCoreProperties();
                if (cp != null && cp.getTitle() != null) {
                    title = cp.getTitle();
                }
            } catch (Exception ignore) {}

            return new ExtractedDoc(title, sb.toString());
        }
    }

    private record ExtractedDoc(String title, String text) {}
}