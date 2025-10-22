package com.example.wordsearch;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.nio.file.Path;
import java.util.List;

@Command(name = "index", description = ".docx індексація у одній або кількох теках")
public class IndexCommand implements Runnable {

    @Option(names = {"-i", "--index-dir"}, required = true, description = "Шлях до каталогу індексу")
    private Path indexDir;

    @Option(names = {"-r", "--recreate"}, description = "Створити індекс з нуля (перезапис)")
    private boolean recreate;

    @Option(names = {"-s", "--sources"}, required = true, split = ",", description = "Список тек для сканування, розділений комами")
    private List<Path> sources;

    @Override
    public void run() {
        try {
            Indexer indexer = new Indexer();
            indexer.indexFolders(sources, indexDir, recreate);
            System.out.println("Індексацію завершено. Індекс: " + indexDir.toAbsolutePath());
        } catch (Exception e) {
            System.err.println("Помилка індексації: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
