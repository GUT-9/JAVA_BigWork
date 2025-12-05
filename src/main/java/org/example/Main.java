package org.example;

import org.example.config.ConfigManager;
import org.example.model.ConversationMeta;
import org.example.model.Message;
import org.example.model.User;
import org.example.model.UserService;
import org.example.util.ConsoleUtil;


import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Main {
    // 移除硬编码的KEY
    // private static final String KEY = System.getenv().getOrDefault("DEEPSEEK_KEY",
    //        "sk-43e04ed77b224c2aa53dc642d6cf58c3");

    private static String currentApiKey = null; // 动态获取API密钥
    private static final DeepSeekClient CLIENT = new DeepSeekClient();
    private static final ConversationService CONV = new ConversationService();
    private static final UserService USER_SERVICE = new UserService();

    private static User currentUser = null;

    public static void main(String[] args) throws IOException {
        // 测试数据库连接
        System.out.println("正在初始化数据库...");
        if (!org.example.config.DatabaseConfig.testConnection()) {
            ConsoleUtil.printLine("❌ 数据库连接失败，请检查SQL Server服务是否启动");
            return;
        }



        // 用户登录/注册
        if (!userAuth()) {
            ConsoleUtil.printLine("认证失败，程序退出");
            return;
        }

        try {
            CONV.load();
            ConsoleUtil.printLine("已加载历史对话");
        } catch (IOException e) {
            ConsoleUtil.printLine("未找到历史，开始新会话");
        }

        boolean running = true;
        while (running) {
            clearScreen();
            menu();
            String choice = ConsoleUtil.readLine("请选择 (1-9): ").trim();
            switch (choice) {
                case "1" -> freeChat();
                case "2" -> translate();
                case "3" -> codeGen();
                case "4" -> summary();
                case "5" -> thesis();
                case "6" -> filePipe();
                case "7" -> showHistory();
                case "8" -> clearHistory();
                case "9" -> {
                    ConsoleUtil.printLine("再见~");
                    running = false;
                }
                case "config" -> ConfigManager.openConfigMenu();
                default -> {
                    ConsoleUtil.printLine("输入无效");
                    pause();
                }
            }
        }
    }
    /**
     * 初始化API密钥
     */
    private static void initializeApiKey() {
        currentApiKey = ConfigManager.getApiKey();

        if (currentApiKey == null || currentApiKey.isEmpty()) {
            ConsoleUtil.printLine("⚠️ 警告：未配置API密钥，部分功能可能受限");
            ConsoleUtil.printLine("   输入 'config' 进入配置菜单进行设置");
        } else {
            // 验证密钥格式（简单检查）
            if (currentApiKey.startsWith("sk-")) {
                ConsoleUtil.printLine("✅ API密钥已加载");
            } else {
                ConsoleUtil.printLine("⚠️ 警告：API密钥格式可能不正确");
            }
        }
    }

    /**
     * 获取当前API密钥（动态检查）
     */
    private static String getApiKey() {
        if (currentApiKey == null || currentApiKey.isEmpty()) {
            currentApiKey = ConfigManager.getApiKey();
        }
        return currentApiKey;
    }

    /**
     * 检查API密钥是否有效
     */
    private static boolean checkApiKey() {
        String key = getApiKey();
        if (key == null || key.isEmpty()) {
            ConsoleUtil.printLine("❌ 未配置API密钥，无法使用此功能");
            ConsoleUtil.printLine("   请先配置API密钥：");
            ConsoleUtil.printLine("   1. 在主菜单输入 'config' 进入配置");
            ConsoleUtil.printLine("   2. 设置环境变量 DEEPSEEK_API_KEY");
            ConsoleUtil.printLine("   3. 在配置文件中配置");
            return false;
        }
        return true;
    }

    private static void clearScreen() {
        System.out.print("\033[H\033[2J");
        System.out.flush();
    }

    private static void pause() {
        ConsoleUtil.readLine("按回车继续...\n");
    }

    private static boolean userAuth() {
        while (true) {
            ConsoleUtil.printLine("""
                    ========== 用户认证 ==========
                    1. 登录
                    2. 注册
                    3. 退出
                    """);

            String choice = ConsoleUtil.readLine("请选择: ").trim();
            switch (choice) {
                case "1" -> {
                    String username = ConsoleUtil.readLine("用户名: ").trim();
                    String password = ConsoleUtil.readLine("密码: ").trim();

                    Optional<User> user = USER_SERVICE.login(username, password);
                    if (user.isPresent()) {
                        currentUser = user.get();
                        ConsoleUtil.printLine("登录成功！欢迎 " + username);
                        return true;
                    } else {
                        ConsoleUtil.printLine("用户名或密码错误！");
                    }
                }
                case "2" -> {
                    String username = ConsoleUtil.readLine("用户名: ").trim();
                    if (USER_SERVICE.userExists(username)) {
                        ConsoleUtil.printLine("用户名已存在！");
                        continue;
                    }
                    String password = ConsoleUtil.readLine("密码: ").trim();

                    if (USER_SERVICE.register(username, password)) {
                        ConsoleUtil.printLine("注册成功！请登录");
                    } else {
                        ConsoleUtil.printLine("注册失败！");
                    }
                }
                case "3" -> {
                    return false;
                }
                default -> ConsoleUtil.printLine("输入无效");
            }
        }
    }

    private static void menu() {
        String username = currentUser != null ? currentUser.getUsername() : "未知用户";

        // ANSI 颜色代码
        final String CYAN = "\033[96m";
        final String YELLOW = "\033[93m";
        final String GRAY = "\033[90m";
        final String BLUE = "\033[94m";
        final String RED = "\033[91m";
        final String GREEN = "\033[92m";
        final String RESET = "\033[0m";

        // 构建API密钥状态行
        String apiStatusLine;
        if (getApiKey() != null) {
            apiStatusLine = "                API密钥:" + GREEN + " ✓ 已配置 " + CYAN;
        } else {
            apiStatusLine = "                API密钥:" + RED + " ✗ 未配置 " + CYAN;
        }

        // 构建用户名行（限制用户名长度）
        String displayUsername = username;
        if (displayUsername.length() > 18) {
            displayUsername = displayUsername.substring(0, 15) + "...";
        }
        String userLine = "                    用户:" + BLUE + String.format("%-18s", displayUsername) + CYAN;

        // 构建菜单字符串
        String menu = CYAN + """
    ╔═══════════════════════════════════════════════════╗
                    DeepSeek 控制台
    """ +
                userLine + "\n" +
                apiStatusLine + "\n" + CYAN + """
    ╚═══════════════════════════════════════════════════╝
    """ + RESET +
                YELLOW + """
      🗨    1. 自由对话（带上下文）
      🔤   2. 中英互译
      💻   3. 代码补全/生成（自动写文件）
      📄   4. 文本摘要
      📝   5. 一键论文（Word）
      📂   6. 文件管道（读→处理→写）
      📊   7. 查看历史对话
      🗑    8. 清空历史对话
      ⚠    9. 退出系统
    """ + RESET +
                GRAY + """
    ╔═══════════════════════════════════════════════════╗
    ║         输入选项编号 [1-9] 并按 Enter 确认            ║
    ║         输入 'config' 进入配置管理                    ║
    ╚═══════════════════════════════════════════════════╝
    """ + RESET;

        ConsoleUtil.printLine(menu);

        // 如果没有配置API密钥，显示提醒
        if (getApiKey() == null) {
            ConsoleUtil.printLine(RED + """
    ╔═══════════════════════════════════════════════════╗
    ║  ⚠️  警告：未配置API密钥，部分功能可能受限              ║
    ║     请在主菜单输入 'config' 进入配置管理               ║
    ╚═══════════════════════════════════════════════════╝
    """ + RESET);
        }
    }

    /* ---------------- 功能 ---------------- */
    /* =================  自由对话 v2  ================= */
    /* =================  自由对话优化版 ================= */
    private static void freeChat() throws IOException {
        if (!checkApiKey()) {
            pause();
            return;
        }
        boolean inFreeChat = true;

        while (inFreeChat) {
            clearScreen();
            ConsoleUtil.printLine("\n" + "=".repeat(40));
            ConsoleUtil.printLine("          自由对话模式");
            ConsoleUtil.printLine("=".repeat(40));

            ConversationMeta selected = HistorySelector.select(currentUser.getId());

            if (selected == null) {
                // 用户在消息列表输入q，退出自由对话模式
                inFreeChat = false;
            } else if ("NEW".equals(selected.getId())) {
                // 新建对话
                boolean conversationCompleted = newConversation();
                // 新建对话结束后直接回到消息列表，不询问
            } else {
                // 继续现有对话
                continueConversation(selected);
                // 对话结束后直接回到消息列表，不询问
            }
        }
    }

    /* --------------- 子流程1：新建对话 --------------- */
    /* --------------- 新建对话 --------------- */
    private static boolean newConversation() {
        if (!checkApiKey()) {
            return false;
        }
        clearScreen();
        ConsoleUtil.printLine("\n" + "=".repeat(40));
        ConsoleUtil.printLine("          新建对话");
        ConsoleUtil.printLine("=".repeat(40));

        String first = ConsoleUtil.readLine("\n请输入第一句话: ").trim();
        if (first.isEmpty()) {
            return false;
        }

        String id = UUID.randomUUID().toString();
        ConversationMeta meta = ConversationMeta.builder()
                .id(id)
                .title(first.length() > 20 ? first.substring(0, 20) + "…" : first)
                .createTime(System.currentTimeMillis())
                .lastMsgTime(System.currentTimeMillis())
                .userId(currentUser.getId())
                .build();
        List<Message> msgs = new ArrayList<>();
        msgs.add(new Message("user", first));

        try {
            // 显示处理中提示
            System.out.print("🤔 AI正在思考中...");
            String resp = CLIENT.chatWithContext(getApiKey(), msgs, first);
            // 清除处理中提示
            System.out.print("\r✅ AI回复完成！\n\n");

            msgs.add(new Message("assistant", resp));
            ConversationStore.save(meta, msgs);
            ConversationStore.saveMetaToDatabase(meta);

            ConsoleUtil.printLine("🤖 AI: " + resp);
            ConsoleUtil.printLine("\n" + "─".repeat(50));

            // 新建对话完成后直接进入继续对话流程
            return continueSingleConversation(meta, msgs);

        } catch (IOException | SQLException e) {
            System.out.print("\r❌ 调用失败\n");
            ConsoleUtil.printLine("错误: " + e.getMessage());
            return false;
        }
    }
    /* --------------- 子流程2：继续对话 --------------- */
    /* --------------- 继续对话 --------------- */
    private static void continueConversation(ConversationMeta meta) {
        try {
            List<Message> msgs = ConversationStore.loadMsg(meta.getId(), currentUser.getId());
            continueSingleConversation(meta, msgs);
        } catch (IOException e) {
            ConsoleUtil.printLine("❌ 加载失败: " + e.getMessage());
            pause();
        }
    }

    /* --------------- 单次对话流程 --------------- */
    private static boolean continueSingleConversation(ConversationMeta meta, List<Message> msgs) {
        boolean inConversation = true;

        while (inConversation) {
            clearScreen();
            ConsoleUtil.printLine("\n📝 对话: " + meta.getTitle());
            ConsoleUtil.printLine("─".repeat(50));

            // 显示最近消息
            int startIndex = Math.max(0, msgs.size() - 5);
            for (int i = startIndex; i < msgs.size(); i++) {
                Message m = msgs.get(i);
                String prefix = m.getRole().equals("user") ? "👤 你" : "🤖 AI";
                ConsoleUtil.printLine(prefix + ": " + m.getContent());
                if (i < msgs.size() - 1) {
                    ConsoleUtil.printLine("─".repeat(30));
                }
            }
            ConsoleUtil.printLine("─".repeat(50));

            String in = ConsoleUtil.readLine("\n💭 你的消息 (输入 q 返回消息列表): ").trim();
            if ("q".equalsIgnoreCase(in)) {
                inConversation = false;
            } else if (!in.isEmpty()) {
                // 处理用户输入
                msgs.add(new Message("user", in));

                // 显示处理中提示
                System.out.print("🤔 AI正在思考中...");
                try {
                    String resp = CLIENT.chatWithContext(getApiKey(), msgs, in);
                    // 清除处理中提示
                    System.out.print("\r✅ AI回复完成！\n\n");

                    ConsoleUtil.printLine("🤖 AI: " + resp);
                    msgs.add(new Message("assistant", resp));
                    meta.setLastMsgTime(System.currentTimeMillis());
                    ConversationStore.save(meta, msgs);
                    ConversationStore.saveMetaToDatabase(meta);

                    ConsoleUtil.printLine("─".repeat(50));
                    pause(); // 等待用户查看回复

                } catch (IOException | SQLException e) {
                    System.out.print("\r❌ 调用失败\n");
                    ConsoleUtil.printLine("错误: " + e.getMessage());
                    pause();
                }
            }
        }

        return true;
    }
    private static void translate() {
        String q = ConsoleUtil.readLine("文本: ");
        String lang = ConsoleUtil.readLine("目标语言 (zh/en): ");
        String prompt = "请将以下文本翻译为" + ("zh".equals(lang) ? "中文" : "英文") + "，只给译文：\n" + q;
        ConsoleUtil.printLine("译文: " + callChat(prompt));
    }

    private static void codeGen() {
        String lang = ConsoleUtil.readLine("语言 (java/python/go等): ");
        String desc = ConsoleUtil.readLine("需求描述: ");
        String prompt = "请用 " + lang + " 实现以下需求，只返回完整代码：\n" + desc;

        ConsoleUtil.printLine("正在生成代码，请稍候...");
        String raw   = callCode(prompt);          // 调接口
        String[] arr = CodeExtractor.split(raw);  // 分离
        String text  = arr[0];
        String code  = arr[1];

        if (code != null) {
            String file = "output/" + FileTool.guessFileName(lang);
            try {
                FileTool.write(file, code);
                ConsoleUtil.printLine("代码已生成 → " + Paths.get(file).toAbsolutePath());
            } catch (IOException e) {
                ConsoleUtil.printLine("写文件失败: " + e.getMessage());
            }
        }
        // 无论有没有代码，都把文字部分弹窗/控制台显示
        if (!text.isEmpty()) {
            ConsoleUtil.printLine("------ 文字说明 ------");
            ConsoleUtil.printLine(text);
        }
    }

    private static void summary() {
        String q = ConsoleUtil.readLine("长文本: ");
        String prompt = "用三句话概括以下内容：\n" + q;
        ConsoleUtil.printLine("摘要: " + callChat(prompt));
    }

    private static void thesis() {
        String topic = ConsoleUtil.readLine("论文主题: ");

        // 获取用户指定的字数
        int wordCount = getWordCountFromUser();

        ConsoleUtil.printLine("正在生成 " + wordCount + " 字的大纲与正文...");

        // 根据字数调整大纲和正文的提示词
        String outlinePrompt = buildOutlinePrompt(topic, wordCount);
        String outline = callChat(outlinePrompt);

        String bodyPrompt = buildBodyPrompt(outline, wordCount);
        String body = callChat(bodyPrompt);

        // 清理和预处理Markdown内容
        outline = preprocessMarkdown(outline);
        body = preprocessMarkdown(body);

        String file = "output/" + topic.replaceAll("\\s+", "_") + "_" + wordCount + "字.docx";

        try {
            WordExporter.export(topic, outline, body, file);
            ConsoleUtil.printLine("✅ Word 已生成: " + Paths.get(file).toAbsolutePath());
            ConsoleUtil.printLine("📝 生成字数: " + wordCount + " 字");
            ConsoleUtil.printLine("📋 格式: 已自动解析Markdown格式（粗体、斜体、标题等）");
        } catch (IOException e) {
            ConsoleUtil.printLine("❌ 生成 Word 失败: " + e.getMessage());
        }
    }

    /**
     * 预处理Markdown文本
     */
    private static String preprocessMarkdown(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }

        // 去除可能的多余前缀
        if (text.startsWith("大纲：")) {
            text = text.substring(3);
        }
        if (text.startsWith("正文：")) {
            text = text.substring(3);
        }

        // 去除AI回复的常见前缀
        text = text.replaceAll("^好的，[^\\n]+\\n", "");
        text = text.replaceAll("^遵照您的要求[^\\n]+\\n", "");
        text = text.replaceAll("^以下是根据[^\\n]+\\n", "");

        // 标准化换行符
        text = text.replaceAll("\r\n", "\n");

        return text.trim();
    }

    private static int getWordCountFromUser() {
        while (true) {
            String wordCountInput = ConsoleUtil.readLine("论文字数 (100-5000，默认800): ").trim();

            if (wordCountInput.isEmpty()) {
                return 800;
            }

            try {
                int wordCount = Integer.parseInt(wordCountInput);
                if (wordCount < 100) {
                    ConsoleUtil.printLine("❌ 字数不能少于100字，请重新输入");
                } else if (wordCount > 5000) {
                    ConsoleUtil.printLine("❌ 字数不能超过5000字，请重新输入");
                } else {
                    return wordCount;
                }
            } catch (NumberFormatException e) {
                ConsoleUtil.printLine("❌ 请输入有效的数字");
            }
        }
    }

    private static String buildOutlinePrompt(String topic, int wordCount) {
        if (wordCount <= 1000) {
            return "请为主题《" + topic + "》写一份简洁的三级大纲，用罗马数字编号（适合" + wordCount + "字短文）：";
        } else if (wordCount <= 3000) {
            return "请为主题《" + topic + "》写一份详细的三级大纲，用罗马数字编号（适合" + wordCount + "字论文）：";
        } else {
            return "请为主题《" + topic + "》写一份全面的四级大纲，用罗马数字编号（适合" + wordCount + "字长文）：";
        }
    }

    private static String buildBodyPrompt(String outline, int wordCount) {
        return "根据以下大纲写一篇 " + wordCount + " 字左右的论文正文，要求结构完整、内容充实、逻辑清晰：\n" + outline;
    }

    private static void filePipe() {
        String in = ConsoleUtil.readLine("输入文件路径: ");
        try {
            String content = FileTool.read(in);
            ConsoleUtil.printLine("处理方式：1 摘要 2 翻译 3 代码补全");
            String opt = ConsoleUtil.readLine("编号: ");
            String prompt = switch (opt) {
                case "1" -> "请摘要：\n" + content;
                case "2" -> "请翻译为英文：\n" + content;
                case "3" -> "请补全代码：\n" + content;
                default -> content;
            };
            String result = opt.equals("3") ? callCode(prompt) : callChat(prompt);
            String out = ConsoleUtil.readLine("输出目标文件路径(建议填写为“你想要的文件名.md”): ");
            FileTool.write(out, result);
            ConsoleUtil.printLine("处理完成，已写入: " + Paths.get(out).toAbsolutePath());
        } catch (IOException e) {
            ConsoleUtil.printLine("文件操作失败: " + e.getMessage());
        }
    }

    private static void showHistory() throws IOException {
        ConsoleUtil.printLine("\n====== 查看历史 ======");
        ConversationMeta selected = HistorySelector.select(currentUser.getId());
        if (selected == null) return;
        // 只读方式展示
        try {
            List<Message> msgs = ConversationStore.loadMsg(selected.getId(), currentUser.getId());
            msgs.forEach(m -> ConsoleUtil.printLine(
                    (m.getRole().equals("user") ? "【你】" : "【AI】") + m.getContent()));
        } catch (IOException e) {
            ConsoleUtil.printLine("加载失败: " + e.getMessage());
        }
    }

    private static void clearHistory() {
        try {
            CONV.clear();
            ConsoleUtil.printLine("已清空历史");
        } catch (IOException e) {
            ConsoleUtil.printLine("清空失败: " + e.getMessage());
        }
    }

    private static String callChat(String prompt) {
        String key = getApiKey();
        if (key == null || key.isEmpty()) {
            return "❌ 未配置API密钥，请先配置";
        }
        try {
            return CLIENT.chat(key, prompt);
        } catch (IOException e) {
            return "调用失败: " + e.getMessage();
        }
    }

    private static String callCode(String prompt) {
        String key = getApiKey();
        if (key == null || key.isEmpty()) {
            return "❌ 未配置API密钥，请先配置";
        }
        try {
            return CLIENT.code(key, prompt);
        } catch (IOException e) {
            return "调用失败: " + e.getMessage();
        }
    }

}