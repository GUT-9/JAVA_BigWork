package org.example;

import java.io.IOException;
import java.nio.file.*;
import java.util.stream.Collectors;

public class FileTool {
    public static String read(String path) throws IOException {
        Path p = Paths.get(path);
        if (!Files.exists(p)) throw new NoSuchFileException(path);
        return Files.readString(p);
    }

    public static void write(String path, String content) throws IOException {
        Path p = Paths.get(path);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static String guessFileName(String language) {
        return switch (language.toLowerCase()) {
            case "java" -> "Main.java";
            case "python" -> "main.py";
            case "c" -> "main.c";
            case "cpp" -> "main.cpp";
            case "go" -> "main.go";
            case "rust" -> "main.rs";
            case "js" -> "main.js";
            case "ts" -> "main.ts";
            default -> "output.txt";
        };
    }
}