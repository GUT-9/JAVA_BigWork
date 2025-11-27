package org.example;

import org.example.model.ConversationMeta;
import org.example.model.Message;
import org.example.model.User;
import org.example.model.UserService;
import org.example.util.ConsoleUtil;


import java.io.IOException;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class Main {
    private static final String KEY = System.getenv().getOrDefault("DEEPSEEK_KEY",
            "sk-43e04ed77b224c2aa53dc642d6cf58c3");
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
            ConsoleUtil.printLine("已加载历史对话 " + CONV.getHistory().size() + " 条");
        } catch (IOException e) {
            ConsoleUtil.printLine("未找到历史，开始新会话");
        }

        while (true) {
            menu();
            switch (ConsoleUtil.readLine("请选择 (1-9): ").trim()) {
                case "1" -> freeChat();
                case "2" -> translate();
                case "3" -> codeGen();
                case "4" -> summary();
                case "5" -> thesis();
                case "6" -> filePipe();
                case "7" -> showHistory();
                case "8" -> clearHistory();
                case "9" -> {
                    ConsoleUtil.printLine("再见~"); return;
                }
                default -> ConsoleUtil.printLine("输入无效");
            }
        }
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
        final String RESET = "\033[0m";

        String menu = String.format(
                CYAN + """
    ╔═══════════════════════════════════════════════════╗
    ║                DeepSeek 控制台 v2.1                ║
    ║                    用户: """ + BLUE + "%-26s" + CYAN + """ 
    ║
    ╠═══════════════════════════════════════════════════╣
    """ + RESET +
                         YELLOW + """
    ║  🗨    1. 自由对话（带上下文）                        ║
    ║  🔤   2. 中英互译                                  ║
    ║  💻   3. 代码补全/生成（自动写文件）                   ║
    ║  📄   4. 文本摘要                                  ║
    ║  📝   5. 一键论文（Word）                           ║
    ║  📂   6. 文件管道（读→处理→写）                      ║
    ║  📊   7. 查看历史对话                               ║
    ║  🗑    8. 清空历史对话                               ║
    ║  ⚠    9. 退出系统                                  ║
    """ + RESET +
                        GRAY + """
    ╠═══════════════════════════════════════════════════╣
    ║         输入选项编号 [1-9] 并按 Enter 确认            ║
    ╚═══════════════════════════════════════════════════╝
    """ + RESET,
                username
        );

        ConsoleUtil.printLine(menu);
    }

    /* ---------------- 功能 ---------------- */
    /* =================  自由对话 v2  ================= */
    private static void freeChat() throws IOException {
        ConsoleUtil.printLine("\n====== 自由对话 ======");
        ConversationMeta selected = HistorySelector.select(currentUser.getId());
        if (selected == null)                 return; // q
        if ("NEW".equals(selected.getId()))   newConversation();
        else                                  continueConversation(selected);
    }

    /* --------------- 子流程1：新建对话 --------------- */
    private static void newConversation() {
        String first = ConsoleUtil.readLine("请输入第一句话: ").trim();
        if (first.isEmpty()) return;
        String id = UUID.randomUUID().toString();
        ConversationMeta meta = ConversationMeta.builder()
                .id(id)
                .title(first.length() > 20 ? first.substring(0, 20) + "…" : first)
                .createTime(System.currentTimeMillis())
                .lastMsgTime(System.currentTimeMillis())
                .userId(currentUser.getId())  // 关联当前用户
                .build();
        List<Message> msgs = new ArrayList<>();
        msgs.add(new Message("user", first));
        try {
            String resp = CLIENT.chatWithContext(KEY, msgs, first);
            msgs.add(new Message("assistant", resp));
            ConversationStore.save(meta, msgs);
            // 保存到数据库
            ConversationStore.saveMetaToDatabase(meta);
            ConsoleUtil.printLine("AI: " + resp);
        } catch (IOException | SQLException e) {
            ConsoleUtil.printLine("调用失败: " + e.getMessage());
        }
    }

    /* --------------- 子流程2：继续对话 --------------- */
    private static void continueConversation(ConversationMeta meta) {
        ConsoleUtil.printLine("---- 历史消息 ----");
        try {
            List<Message> msgs = ConversationStore.loadMsg(meta.getId(), currentUser.getId());
            msgs.forEach(m -> ConsoleUtil.printLine(
                    (m.getRole().equals("user") ? "【你】" : "【AI】") + m.getContent()));
            ConsoleUtil.printLine("------------------");
            while (true) {
                String in = ConsoleUtil.readLine("你（输入 q 返回）: ").trim();
                if ("q".equalsIgnoreCase(in)) break;
                msgs.add(new Message("user", in));
                String resp = CLIENT.chatWithContext(KEY, msgs, in);
                ConsoleUtil.printLine("AI: " + resp);
                msgs.add(new Message("assistant", resp));
                meta.setLastMsgTime(System.currentTimeMillis());
                ConversationStore.save(meta, msgs);
                // 更新数据库中的最后消息时间
                ConversationStore.saveMetaToDatabase(meta);
            }
        } catch (IOException | SQLException e) {
            ConsoleUtil.printLine("加载失败: " + e.getMessage());
        }
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

        String file = "output/" + topic.replaceAll("\\s+", "_") + "_" + wordCount + "字.docx";

        try {
            WordExporter.export(topic, outline, body, file);
            ConsoleUtil.printLine("✅ Word 已生成: " + Paths.get(file).toAbsolutePath());
            ConsoleUtil.printLine("📝 生成字数: " + wordCount + " 字");
        } catch (IOException e) {
            ConsoleUtil.printLine("❌ 生成 Word 失败: " + e.getMessage());
        }
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
        try {
            return CLIENT.chat(KEY, prompt);
        } catch (IOException e) {
            return "调用失败: " + e.getMessage();
        }
    }

    private static String callCode(String prompt) {
        try {
            return CLIENT.code(KEY, prompt);
        } catch (IOException e) {
            return "调用失败: " + e.getMessage();
        }
    }
}