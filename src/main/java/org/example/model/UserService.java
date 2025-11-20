package org.example.model;

import org.example.config.DatabaseConfig;

import java.sql.*;
import java.util.Optional;

public class UserService {

    public boolean register(String username, String password) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            int result = pstmt.executeUpdate();
            return result > 0;

        } catch (SQLException e) {
            System.err.println("❌ 注册失败: " + e.getMessage());
            return false;
        }
    }

    public Optional<User> login(String username, String password) {
        String sql = "SELECT id, username, password, created_at FROM users WHERE username = ? AND password = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            pstmt.setString(2, password);

            ResultSet rs = pstmt.executeQuery();
            if (rs.next()) {
                User user = User.builder()
                        .id(rs.getInt("id"))
                        .username(rs.getString("username"))
                        .password(rs.getString("password"))
                        .createdAt(rs.getString("created_at"))
                        .build();
                System.out.println("✅ 用户登录成功: " + username);
                return Optional.of(user);
            } else {
                System.out.println("❌ 用户名或密码错误: " + username);
            }

        } catch (SQLException e) {
            System.err.println("❌ 登录失败: " + e.getMessage());
        }

        return Optional.empty();
    }

    public boolean userExists(String username) {
        String sql = "SELECT id FROM users WHERE username = ?";

        try (Connection conn = DatabaseConfig.getConnection();
             PreparedStatement pstmt = conn.prepareStatement(sql)) {

            pstmt.setString(1, username);
            ResultSet rs = pstmt.executeQuery();
            return rs.next();

        } catch (SQLException e) {
            System.err.println("❌ 检查用户失败: " + e.getMessage());
            return false;
        }
    }
}