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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.DriverManager;

public class ConversationStore {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    // 修改：根据用户ID创建不同的存储目录
    private static Path getDir(Integer userId) {
        return Paths.get("history/user_" + userId);
    }

    /* 保存 or 更新 */
    public static void save(ConversationMeta meta, List<Message> msgs) throws IOException {
        Path userDir = getDir(meta.getUserId());
        Files.createDirectories(userDir);
        Path metaFile = userDir.resolve(meta.getId() + ".meta");
        Path msgFile = userDir.resolve(meta.getId() + ".json");
        MAPPER.writeValue(metaFile.toFile(), meta);
        MAPPER.writeValue(msgFile.toFile(), msgs);
    }

    /* 根据 id 加载消息列表 */
    public static List<Message> loadMsg(String id, Integer userId) throws IOException {
        Path msgFile = getDir(userId).resolve(id + ".json");
        if (!Files.exists(msgFile)) return new ArrayList<>();
        return MAPPER.readValue(msgFile.toFile(), new TypeReference<>() {
        });
    }

    /* 加载指定用户的全部元信息（按 lastMsgTime 倒序） */
    public static List<ConversationMeta> listMeta(Integer userId) throws IOException {
        Path userDir = getDir(userId);
        if (!Files.exists(userDir)) return List.of();
        try (Stream<Path> stream = Files.list(userDir)) {
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
    public static void delete(String id, Integer userId) throws IOException {
        Path userDir = getDir(userId);
        Files.deleteIfExists(userDir.resolve(id + ".meta"));
        Files.deleteIfExists(userDir.resolve(id + ".json"));
    }

    /* 保存对话元信息到数据库 */
    public static void saveMetaToDatabase(ConversationMeta meta) throws SQLException {
        String sql = """
            MERGE conversations AS target
            USING (SELECT ? AS id) AS source
            ON target.id = source.id
            WHEN MATCHED THEN
                UPDATE SET title = ?, last_msg_time = ?
            WHEN NOT MATCHED THEN
                INSERT (id, user_id, title, create_time, last_msg_time)
                VALUES (?, ?, ?, ?, ?);
            """;

        try (Connection conn = org.example.config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, meta.getId());
            pstmt.setString(2, meta.getTitle());
            pstmt.setLong(3, meta.getLastMsgTime());
            pstmt.setString(4, meta.getId());
            pstmt.setInt(5, meta.getUserId());
            pstmt.setString(6, meta.getTitle());
            pstmt.setLong(7, meta.getCreateTime());
            pstmt.setLong(8, meta.getLastMsgTime());

            pstmt.executeUpdate();
        }
    }

    /* 从数据库加载用户对话元信息 */
    public static List<ConversationMeta> loadMetaFromDatabase(Integer userId) throws SQLException {
        String sql = "SELECT id, title, create_time, last_msg_time FROM conversations WHERE user_id = ? ORDER BY last_msg_time DESC";
        List<ConversationMeta> metas = new ArrayList<>();

        try (Connection conn = org.example.config.DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setInt(1, userId);
            ResultSet rs = pstmt.executeQuery();

            while (rs.next()) {
                ConversationMeta meta = ConversationMeta.builder()
                        .id(rs.getString("id"))
                        .title(rs.getString("title"))
                        .createTime(rs.getLong("create_time"))
                        .lastMsgTime(rs.getLong("last_msg_time"))
                        .userId(userId)
                        .build();
                metas.add(meta);
            }
        }

        return metas;
    }
}