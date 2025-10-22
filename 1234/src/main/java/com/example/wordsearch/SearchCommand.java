package com.example.wordsearch;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;

@Command(name = "search", description = "Пошук в індексі")
public class SearchCommand implements Runnable {

    @Option(names = {"-i", "--index-dir"}, required = true, description = "Шлях до каталогу індексу")
    private Path indexDir;

    @Option(names = {"-q", "--query"}, required = true, description = "Рядок запиту")
    private String query;

    @Option(names = {"-l", "--limit"}, description = "Макс. кількість результатів (типово 10)")
    private int limit = 10;

    @Option(names = {"-f", "--filename"}, description = "Необов’язковий фільтр: назва файлу містить")
    private String filenameContains;

    @Option(names = {"-m", "--modified-since-days"}, description = "Необов’язковий фільтр: змінені за останні N днів")
    private Integer modifiedSinceDays;

    @Override
    public void run() {
        try {
            if (!java.nio.file.Files.exists(indexDir) || !Searcher.indexExists(indexDir)) {
                System.err.println("Індекс не знайдено у: " + indexDir.toAbsolutePath());
                System.err.println("Спочатку виконайте команду index, щоб створити індекс.");
                System.exit(2);
            }
            Searcher searcher = new Searcher(indexDir);
            var results = searcher.search(query, limit, filenameContains, modifiedSinceDays);
            if (results.isEmpty()) {
                System.out.println("Немає результатів.");
            } else {
                for (var r : results) {
                    System.out.printf("- %s (score=%.3f)%n", r.getPath(), r.getScore());
                    if (r.getTitle() != null && !r.getTitle().isBlank()) {
                        System.out.println("  назва: " + r.getTitle());
                    }
                    if (r.getPreview() != null && !r.getPreview().isBlank()) {
                        System.out.println("  фрагмент: " + r.getPreview());
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Помилка пошуку: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
