package org.example.config;

import org.example.util.ConsoleUtil;

import java.awt.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;
import java.util.Base64;
import java.nio.charset.StandardCharsets;

/**
 * API密钥配置管理器
 * 优先级：环境变量 > 配置文件 > 手动输入
 */
public class ConfigManager {

    private static final String CONFIG_DIR = System.getProperty("user.home") + "/.deepseek-console";
    private static final String CONFIG_FILE = CONFIG_DIR + "/config.properties";
    private static final String KEY_ENV_VAR = "DEEPSEEK_API_KEY";
    private static final String KEY_CONFIG_NAME = "api.key";

    private static final String SIMPLE_ENCRYPT_KEY = "DeepSeekConsole2024!";

    // 配置项常量
    public static final String KEY_API_KEY = "api.key";
    public static final String KEY_MODEL = "api.model";
    public static final String KEY_TEMPERATURE = "api.temperature";
    public static final String KEY_TIMEOUT = "api.timeout";
    public static final String KEY_AUTO_SAVE = "app.auto_save";

    private static Properties config;

    static {
        loadConfig();
    }

    /**
     * 获取API密钥（主方法）
     */
    public static String getApiKey() {
        // 1. 检查环境变量
        String envKey = System.getenv(KEY_ENV_VAR);
        if (envKey != null && !envKey.trim().isEmpty()) {
            return envKey.trim();
        }

        // 2. 检查配置文件
        String configKey = config.getProperty(KEY_API_KEY);
        if (configKey != null && !configKey.trim().isEmpty()) {
            return decrypt(configKey.trim());
        }

        // 3. 提示用户输入
        return promptForApiKey();
    }

    /**
     * 提示用户输入API密钥
     */
    private static String promptForApiKey() {
        ConsoleUtil.printLine("\n⚠️ 未找到API密钥配置");
        ConsoleUtil.printLine("请按以下方式之一配置：");
        ConsoleUtil.printLine("1. 设置环境变量: DEEPSEEK_API_KEY");
        ConsoleUtil.printLine("2. 在配置文件中设置");
        ConsoleUtil.printLine("3. 本次临时输入\n");

        String key = ConsoleUtil.readLine("请输入DeepSeek API密钥 (输入q跳过): ").trim();

        if ("q".equalsIgnoreCase(key) || key.isEmpty()) {
            ConsoleUtil.printLine("❌ 缺少API密钥，部分功能将无法使用");
            return null;
        }

        // 询问是否保存
        String saveChoice = ConsoleUtil.readLine("是否保存到配置文件？(y/n): ").trim().toLowerCase();
        if ("y".equals(saveChoice) || "yes".equals(saveChoice)) {
            saveApiKey(key);
            ConsoleUtil.printLine("✅ API密钥已保存到配置文件");
        } else {
            ConsoleUtil.printLine("⚠️ 密钥未保存，仅本次会话有效");
        }

        return key;
    }

    /**
     * 保存API密钥到配置文件
     */
    public static void saveApiKey(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return;
        }

        config.setProperty(KEY_API_KEY, encrypt(apiKey.trim()));
        saveConfig();
    }

    /**
     * 删除API密钥
     */
    public static void removeApiKey() {
        config.remove(KEY_API_KEY);
        saveConfig();
        ConsoleUtil.printLine("✅ API密钥已从配置文件中移除");
    }

    /**
     * 获取配置值
     */
    public static String getConfig(String key, String defaultValue) {
        String value = config.getProperty(key);
        return value != null ? value : defaultValue;
    }

    /**
     * 设置配置值
     */
    public static void setConfig(String key, String value) {
        config.setProperty(key, value);
        saveConfig();
    }

    /**
     * 打开配置界面
     */
    public static void openConfigMenu() {
        boolean running = true;

        while (running) {
            ConsoleUtil.printLine("\n" + "=".repeat(40));
            ConsoleUtil.printLine("          配置管理");
            ConsoleUtil.printLine("=".repeat(40));

            String currentKey = config.getProperty(KEY_API_KEY);
            String keyStatus = currentKey != null ? "✅ 已配置" : "❌ 未配置";

            ConsoleUtil.printLine("1. 查看当前配置");
            ConsoleUtil.printLine("2. 设置API密钥");
            ConsoleUtil.printLine("3. 删除API密钥");
            ConsoleUtil.printLine("4. 设置默认模型");
            ConsoleUtil.printLine("5. 设置高级参数");
            ConsoleUtil.printLine("6. 打开配置文件目录");
            ConsoleUtil.printLine("7. 返回主菜单");

            String choice = ConsoleUtil.readLine("请选择: ").trim();

            switch (choice) {
                case "1":
                    showCurrentConfig();
                    break;
                case "2":
                    setApiKeyFromInput();
                    break;
                case "3":
                    removeApiKey();
                    break;
                case "4":
                    setDefaultModel();
                    break;
                case "5":
                    setAdvancedParams();
                    break;
                case "6":
                    openConfigDirectory();
                    break;
                case "7":
                    running = false;
                    break;
                default:
                    ConsoleUtil.printLine("❌ 无效选项");
            }

            if (!"7".equals(choice)) {
                ConsoleUtil.readLine("\n按回车键继续...");
            }
        }
    }

    /**
     * 显示当前配置
     */
    private static void showCurrentConfig() {
        ConsoleUtil.printLine("\n📋 当前配置：");
        ConsoleUtil.printLine("-".repeat(30));

        // API密钥状态（不显示实际值）
        String key = config.getProperty(KEY_API_KEY);
        if (key != null && !key.isEmpty()) {
            ConsoleUtil.printLine("API密钥: ✅ 已配置");
        } else {
            ConsoleUtil.printLine("API密钥: ❌ 未配置");
        }

        // 显示其他配置
        for (String keyName : config.stringPropertyNames()) {
            if (!KEY_API_KEY.equals(keyName)) {
                String value = config.getProperty(keyName);
                ConsoleUtil.printLine(keyName + ": " + value);
            }
        }

        // 配置文件路径
        ConsoleUtil.printLine("-".repeat(30));
        ConsoleUtil.printLine("配置文件: " + CONFIG_FILE);

        // 环境变量状态
        String envKey = System.getenv(KEY_ENV_VAR);
        ConsoleUtil.printLine("环境变量: " + (envKey != null ? "✅ 已设置" : "❌ 未设置"));
    }

    /**
     * 从用户输入设置API密钥
     */
    private static void setApiKeyFromInput() {
        ConsoleUtil.printLine("\n🔑 设置API密钥");
        ConsoleUtil.printLine("您可以在 https://platform.deepseek.com/ 获取API密钥");

        String key = ConsoleUtil.readLine("请输入API密钥: ").trim();

        if (key.isEmpty()) {
            ConsoleUtil.printLine("❌ 密钥不能为空");
            return;
        }

        // 简单验证格式（以sk-开头）
        if (!key.startsWith("sk-")) {
            ConsoleUtil.printLine("⚠️ 警告：密钥格式可能不正确（应以sk-开头）");
            String confirm = ConsoleUtil.readLine("是否继续保存？(y/n): ").trim().toLowerCase();
            if (!"y".equals(confirm)) {
                return;
            }
        }

        saveApiKey(key);
        ConsoleUtil.printLine("✅ API密钥已保存");
    }

    /**
     * 设置默认模型
     */
    private static void setDefaultModel() {
        ConsoleUtil.printLine("\n🤖 设置默认模型");
        ConsoleUtil.printLine("1. deepseek-chat (通用聊天)");
        ConsoleUtil.printLine("2. deepseek-coder (代码专用)");

        String current = config.getProperty(KEY_MODEL, "deepseek-chat");
        ConsoleUtil.printLine("当前模型: " + current);

        String choice = ConsoleUtil.readLine("选择模型 (1/2，或直接输入模型名称): ").trim();

        String model;
        if ("1".equals(choice)) {
            model = "deepseek-chat";
        } else if ("2".equals(choice)) {
            model = "deepseek-coder";
        } else if (!choice.isEmpty()) {
            model = choice;
        } else {
            model = current;
        }

        config.setProperty(KEY_MODEL, model);
        saveConfig();
        ConsoleUtil.printLine("✅ 默认模型已设置为: " + model);
    }

    /**
     * 设置高级参数
     */
    private static void setAdvancedParams() {
        ConsoleUtil.printLine("\n⚙️ 设置高级参数");

        // 温度参数
        String currentTemp = config.getProperty(KEY_TEMPERATURE, "0.7");
        String temp = ConsoleUtil.readLine("温度参数 (0.0-1.0，当前 " + currentTemp + "): ").trim();
        if (!temp.isEmpty()) {
            try {
                double tempValue = Double.parseDouble(temp);
                if (tempValue >= 0.0 && tempValue <= 1.0) {
                    config.setProperty(KEY_TEMPERATURE, temp);
                } else {
                    ConsoleUtil.printLine("❌ 温度参数必须在0.0-1.0之间");
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printLine("❌ 请输入有效的数字");
            }
        }

        // 超时时间
        String currentTimeout = config.getProperty(KEY_TIMEOUT, "300");
        String timeout = ConsoleUtil.readLine("超时时间(秒，当前 " + currentTimeout + "): ").trim();
        if (!timeout.isEmpty()) {
            try {
                int timeoutValue = Integer.parseInt(timeout);
                if (timeoutValue > 0) {
                    config.setProperty(KEY_TIMEOUT, timeout);
                } else {
                    ConsoleUtil.printLine("❌ 超时时间必须大于0");
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printLine("❌ 请输入有效的数字");
            }
        }

        // 自动保存
        String currentAutoSave = config.getProperty(KEY_AUTO_SAVE, "true");
        String autoSave = ConsoleUtil.readLine("自动保存对话 (true/false，当前 " + currentAutoSave + "): ").trim();
        if (!autoSave.isEmpty() && ("true".equals(autoSave) || "false".equals(autoSave))) {
            config.setProperty(KEY_AUTO_SAVE, autoSave);
        }

        saveConfig();
        ConsoleUtil.printLine("✅ 高级参数已保存");
    }

    /**
     * 打开配置文件目录
     */
    private static void openConfigDirectory() {
        try {
            Path configPath = Paths.get(CONFIG_DIR);
            if (!Files.exists(configPath)) {
                Files.createDirectories(configPath);
            }

            ConsoleUtil.printLine("📁 配置文件目录: " + configPath.toAbsolutePath());
            ConsoleUtil.printLine("📄 配置文件: " + Paths.get(CONFIG_FILE).toAbsolutePath());

            // 尝试在文件浏览器中打开
            if (Desktop.isDesktopSupported()) {
                Desktop desktop = Desktop.getDesktop();
                if (desktop.isSupported(Desktop.Action.OPEN)) {
                    desktop.open(configPath.toFile());
                }
            }
        } catch (Exception e) {
            ConsoleUtil.printLine("❌ 无法打开目录: " + e.getMessage());
        }
    }

    /**
     * 加载配置文件
     */
    private static void loadConfig() {
        config = new Properties();

        try {
            Path configPath = Paths.get(CONFIG_FILE);
            if (Files.exists(configPath)) {
                try (InputStream input = Files.newInputStream(configPath)) {
                    config.load(input);
                }
            }
        } catch (IOException e) {
            // 配置文件不存在或读取失败，使用默认配置
            setDefaultConfig();
        }
    }

    /**
     * 保存配置文件
     */
    private static void saveConfig() {
        try {
            Path configDir = Paths.get(CONFIG_DIR);
            if (!Files.exists(configDir)) {
                Files.createDirectories(configDir);
            }

            try (OutputStream output = Files.newOutputStream(Paths.get(CONFIG_FILE))) {
                config.store(output, "DeepSeek Console Configuration");
            }
        } catch (IOException e) {
            ConsoleUtil.printLine("❌ 保存配置失败: " + e.getMessage());
        }
    }

    /**
     * 设置默认配置
     */
    private static void setDefaultConfig() {
        config.setProperty(KEY_MODEL, "deepseek-chat");
        config.setProperty(KEY_TEMPERATURE, "0.7");
        config.setProperty(KEY_TIMEOUT, "300");
        config.setProperty(KEY_AUTO_SAVE, "true");
    }

    /**
     * 简单加密（避免明文存储）
     */
    private static String encrypt(String text) {
        try {
            // 使用简单的XOR加密
            byte[] textBytes = text.getBytes(StandardCharsets.UTF_8);
            byte[] keyBytes = SIMPLE_ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);

            byte[] encrypted = new byte[textBytes.length];
            for (int i = 0; i < textBytes.length; i++) {
                encrypted[i] = (byte) (textBytes[i] ^ keyBytes[i % keyBytes.length]);
            }

            return Base64.getEncoder().encodeToString(encrypted);
        } catch (Exception e) {
            return text; // 加密失败，返回原文本
        }
    }

    /**
     * 解密
     */
    private static String decrypt(String encryptedText) {
        try {
            byte[] encryptedBytes = Base64.getDecoder().decode(encryptedText);
            byte[] keyBytes = SIMPLE_ENCRYPT_KEY.getBytes(StandardCharsets.UTF_8);

            byte[] decrypted = new byte[encryptedBytes.length];
            for (int i = 0; i < encryptedBytes.length; i++) {
                decrypted[i] = (byte) (encryptedBytes[i] ^ keyBytes[i % keyBytes.length]);
            }

            return new String(decrypted, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return encryptedText; // 解密失败，返回原文本
        }
    }

    /**
     * 获取配置目录路径
     */
    public static String getConfigDir() {
        return CONFIG_DIR;
    }

    /**
     * 获取配置文件路径
     */
    public static String getConfigFile() {
        return CONFIG_FILE;
    }
}