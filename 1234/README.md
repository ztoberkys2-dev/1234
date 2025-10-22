# Word Search (Java 17, Maven)

Index and search Microsoft Word `.docx` files using Apache Lucene and Apache POI.

## Requirements
- Java 17+
- Maven 3.9+

## Quick start (Windows PowerShell)

```powershell
# Run tests
mvn -q -e -DskipTests=false test

# Package a fat jar
mvn -q -DskipTests package

# Run CLI help
java -jar target/word-search-0.1.0-SNAPSHOT-jar-with-dependencies.jar --help
```

## CLI

Two subcommands: `index` and `search`.

```powershell
# Build index (recreate) from one or more folders
java -jar target/word-search-0.1.0-SNAPSHOT-jar-with-dependencies.jar index `
  --index-dir .\index `
  --recreate `
  --sources C:\Docs,C:\MoreDocs

# Search
java -jar target/word-search-0.1.0-SNAPSHOT-jar-with-dependencies.jar search `
  --index-dir .\index `
  --query "привіт світ" `
  --limit 10 `
  --filename "hello" `
  --modified-since-days 30
```

Notes:
- Only `.docx` files are indexed.
- Stored fields: path, filename_kw, title (if present), modified_store, size_store, preview.
- Indexed fields: content, title, filename; numeric points: modified, size.

## Development

- Main entry: `com.example.wordsearch.App`
- Indexer: `com.example.wordsearch.Indexer`
- Searcher: `com.example.wordsearch.Searcher`

VS Code:
- Run tests: use the provided task "Maven: test".
- Launch config runs the CLI with arguments.

## License

MIT

## Install Java 17 and Maven on Windows (one-time)

If `mvn` is not recognized in PowerShell, install Maven. You’ll also need JDK 17.

```powershell
# Option A: winget (recommended)
winget install --id EclipseAdoptium.Temurin.17.JDK -e
winget install --id Apache.Maven -e

# Option B: Chocolatey (if you use choco)
choco install temurin17 -y
choco install maven -y

# Verify
java -version
mvn -version
```

If Java 17 is installed but VS Code still compiles with a different Java, set your Java runtime in VS Code Settings (Java > Configuration: Runtime) or ensure `JAVA_HOME` points to the Java 17 install directory and PowerShell is restarted.
