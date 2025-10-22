package com.example.wordsearch;

import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(
    name = "wordsearch",
    mixinStandardHelpOptions = true,
    version = "wordsearch 0.1.0",
    description = "Індексувати та шукати файли .docx",
    subcommands = {
        IndexCommand.class,
        SearchCommand.class
    }
)
public class App implements Runnable {
    public static void main(String[] args) {
        int exitCode = new CommandLine(new App()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Скористайтеся підкомандою: index | search. Спробуйте --help.");
    }
}
