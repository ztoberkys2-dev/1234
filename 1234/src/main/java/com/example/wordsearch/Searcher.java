package com.example.wordsearch;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.uhighlight.UnifiedHighlighter;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Searcher implements AutoCloseable {
    private DirectoryReader reader;
    private IndexSearcher searcher;
    private final Analyzer analyzer = new StandardAnalyzer();

    public Searcher(Path indexDir) throws IOException {
        this.reader = DirectoryReader.open(FSDirectory.open(indexDir));
        this.searcher = new IndexSearcher(reader);
    }

    public List<SearchResult> search(String queryString, int limit, String filenameContains, Integer modifiedSinceDays) throws Exception {
        BooleanQuery.Builder builder = new BooleanQuery.Builder();

        String[] fields = new String[]{"content", "title", "filename"};
        MultiFieldQueryParser parser = new MultiFieldQueryParser(fields, analyzer);
        Query mainQuery = parser.parse(queryString);
        builder.add(mainQuery, BooleanClause.Occur.MUST);

        if (filenameContains != null && !filenameContains.isBlank()) {
            String pattern = "*" + filenameContains.toLowerCase(Locale.ROOT) + "*";
            Query fname = new WildcardQuery(new Term("filename_lc_kw", pattern));
            builder.add(fname, BooleanClause.Occur.FILTER);
        }
        if (modifiedSinceDays != null && modifiedSinceDays > 0) {
            long threshold = Instant.now().minus(modifiedSinceDays, ChronoUnit.DAYS).toEpochMilli();
            Query timeq = LongPoint.newRangeQuery("modified", threshold, Long.MAX_VALUE);
            builder.add(timeq, BooleanClause.Occur.FILTER);
        }

        Query finalQuery = builder.build();
        TopDocs topDocs = searcher.search(finalQuery, Math.max(1, limit));

        UnifiedHighlighter highlighter = new UnifiedHighlighter(searcher, analyzer);
        String[] highlights = highlighter.highlight("content", finalQuery, topDocs, 1);

        List<SearchResult> results = new ArrayList<>();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc sd = topDocs.scoreDocs[i];
            Document d = searcher.doc(sd.doc);
            String path = d.get("path");
            String title = d.get("title");
            String preview = d.get("preview");
            if (highlights != null && i < highlights.length && highlights[i] != null && !highlights[i].isBlank()) {
                preview = highlights[i].replaceAll("\\n", " ");
            }
            results.add(new SearchResult(path, title, preview, sd.score));
        }
        return results;
    }

    public static boolean indexExists(Path indexDir) {
        try {
            return DirectoryReader.indexExists(FSDirectory.open(indexDir));
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void close() throws IOException {
        if (reader != null) reader.close();
    }

    public static class SearchResult {
        private final String path;
        private final String title;
        private final String preview;
        private final float score;

        public SearchResult(String path, String title, String preview, float score) {
            this.path = path; this.title = title; this.preview = preview; this.score = score;
        }
        public String getPath() { return path; }
        public String getTitle() { return title; }
        public String getPreview() { return preview; }
        public float getScore() { return score; }
    }
}
