package org.example.config;

import java.sql.*;

public class DatabaseConfig {
    private static final String URL = "jdbc:jtds:sqlserver://localhost:1433/ChatApp";
    private static final String USER = "sa";
    private static final String PASSWORD = "123"; // 替换为您的密码

    static {
        try {
            Class.forName("net.sourceforge.jtds.jdbc.Driver");
            System.out.println("✅ JTDS驱动加载成功");

            // 启动时初始化数据库
            initializeDatabase();
        } catch (ClassNotFoundException e) {
            throw new RuntimeException("加载JDBC驱动失败", e);
        } catch (SQLException e) {
            System.err.println("❌ 数据库初始化失败: " + e.getMessage());
        }
    }

    public static Connection getConnection() throws SQLException {
        try {
            System.out.println("尝试连接数据库...");
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ 数据库连接成功");
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ 数据库连接失败: " + e.getMessage());

            // 如果是数据库不存在的错误，尝试创建数据库
            if (e.getMessage().contains("database") && e.getMessage().contains("not found")) {
                System.out.println("尝试创建数据库...");
                if (createDatabase()) {
                    // 重新尝试连接
                    return DriverManager.getConnection(URL, USER, PASSWORD);
                }
            }

            throw e;
        }
    }

    // 初始化数据库和表
    private static void initializeDatabase() throws SQLException {
        // 确保数据库存在
        if (!databaseExists()) {
            System.out.println("数据库不存在，正在创建...");
            createDatabase();
        }

        // 确保表存在
        if (!tableExists("users")) {
            System.out.println("创建users表...");
            createUsersTable();
        }

        if (!tableExists("conversations")) {
            System.out.println("创建conversations表...");
            createConversationsTable();
        }

        System.out.println("✅ 数据库初始化完成");
    }

    // 检查数据库是否存在
    private static boolean databaseExists() {
        String checkDbSql = "SELECT COUNT(*) FROM master.dbo.sysdatabases WHERE name = 'ChatApp'";

        try (Connection conn = getMasterConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkDbSql)) {

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            System.err.println("检查数据库存在失败: " + e.getMessage());
            return false;
        }
    }

    // 创建数据库
    private static boolean createDatabase() {
        String createDbSql = "CREATE DATABASE ChatApp";

        try (Connection conn = getMasterConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createDbSql);
            System.out.println("✅ 数据库创建成功");

            // 创建表
            createUsersTable();
            createConversationsTable();

            return true;
        } catch (SQLException e) {
            System.err.println("❌ 数据库创建失败: " + e.getMessage());
            return false;
        }
    }

    // 创建用户表
    private static void createUsersTable() throws SQLException {
        String createTableSql =
                "CREATE TABLE users (" +
                        "    id INT IDENTITY(1,1) PRIMARY KEY, " +
                        "    username NVARCHAR(50) NOT NULL UNIQUE, " +
                        "    password NVARCHAR(100) NOT NULL, " +
                        "    created_at DATETIME DEFAULT GETDATE()" +
                        ")";

        executeUpdate(createTableSql);
        System.out.println("✅ users表创建成功");
    }

    // 创建对话表
    private static void createConversationsTable() throws SQLException {
        String createTableSql =
                "CREATE TABLE conversations (" +
                        "    id NVARCHAR(50) PRIMARY KEY, " +
                        "    user_id INT NOT NULL, " +
                        "    title NVARCHAR(200) NOT NULL, " +
                        "    create_time BIGINT NOT NULL, " +
                        "    last_msg_time BIGINT NOT NULL, " +
                        "    FOREIGN KEY (user_id) REFERENCES users(id)" +
                        ")";

        executeUpdate(createTableSql);
        System.out.println("✅ conversations表创建成功");
    }

    // 检查表是否存在
    private static boolean tableExists(String tableName) {
        String checkTableSql =
                "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = '" + tableName + "'";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(checkTableSql)) {

            return rs.next() && rs.getInt(1) > 0;
        } catch (SQLException e) {
            return false;
        }
    }

    // 执行更新操作
    private static void executeUpdate(String sql) throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.executeUpdate(sql);
        }
    }

    // 获取master数据库连接（用于创建数据库）
    private static Connection getMasterConnection() throws SQLException {
        String masterUrl = "jdbc:jtds:sqlserver://localhost:1433/master";
        return DriverManager.getConnection(masterUrl, USER, PASSWORD);
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("连接测试失败: " + e.getMessage());
            return false;
        }
    }
}