package org.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.example.model.ConversationMeta;
import org.example.model.Message;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ConversationStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Path DIR = Paths.get("history");

    /* 保存 or 更新 */
    public static void save(ConversationMeta meta, List<Message> msgs) throws IOException {
        Files.createDirectories(DIR);
        Path metaFile = DIR.resolve(meta.getId() + ".meta");
        Path msgFile = DIR.resolve(meta.getId() + ".json");
        MAPPER.writeValue(metaFile.toFile(), meta);
        MAPPER.writeValue(msgFile.toFile(), msgs);
    }

    /* 根据 id 加载消息列表 */
    public static List<Message> loadMsg(String id) throws IOException {
        Path msgFile = DIR.resolve(id + ".json");
        if (!Files.exists(msgFile)) return new ArrayList<>();
        return MAPPER.readValue(msgFile.toFile(), new TypeReference<>() {
        });
    }

    /* 加载全部元信息（按 lastMsgTime 倒序） */
    public static List<ConversationMeta> listMeta() throws IOException {
        if (!Files.exists(DIR)) return List.of();
        try (Stream<Path> stream = Files.list(DIR)) {
            return stream
                    .filter(p -> p.toString().endsWith(".meta"))
                    .map(p -> {
                        try {
                            return MAPPER.readValue(p.toFile(), ConversationMeta.class);
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    })
                    .sorted((a, b) -> Long.compare(b.getLastMsgTime(), a.getLastMsgTime()))
                    .collect(Collectors.toList());
        }
    }

    /* 删除对话 */
    public static void delete(String id) throws IOException {
        Files.deleteIfExists(DIR.resolve(id + ".meta"));
        Files.deleteIfExists(DIR.resolve(id + ".json"));
    }
}