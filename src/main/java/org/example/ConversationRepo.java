package org.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.Message;

import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class ConversationRepo {
    private static final String FILE = "conversation.jsonl";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static void append(Message m) throws IOException {
        String line = MAPPER.writeValueAsString(m) + System.lineSeparator();
        Files.write(Paths.get(FILE), line.getBytes(),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    public static List<Message> load() throws IOException {
        if (!Files.exists(Paths.get(FILE))) return new ArrayList<>();
        List<Message> list = new ArrayList<>();
        for (String l : Files.readAllLines(Paths.get(FILE))) {
            if (l.trim().isEmpty()) continue;
            list.add(MAPPER.readValue(l, Message.class));
        }
        return list;
    }

    public static void clear() throws IOException {
        Files.deleteIfExists(Paths.get(FILE));
    }
}