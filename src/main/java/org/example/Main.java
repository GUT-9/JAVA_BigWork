package org.example;

import org.example.model.Message;
import org.example.util.ConsoleUtil;

import java.io.IOException;
import java.nio.file.Paths;

public class Main {
    private static final String KEY = System.getenv().getOrDefault("DEEPSEEK_KEY",
            "sk-bee2886b58d44f76b28b82df50aea92a");
    private static final DeepSeekClient CLIENT = new DeepSeekClient();
    private static final ConversationService CONV = new ConversationService();

    public static void main(String[] args) {
        try {
            CONV.load();
            ConsoleUtil.printLine("已加载历史对话 " + CONV.getHistory().size() + " 条");
        } catch (IOException e) {
            ConsoleUtil.printLine("未找到历史，开始新会话");
        }

        while (true) {
            menu();
            switch (ConsoleUtil.readLine("请选择 (1-10): ").trim()) {
                case "1" -> freeChat();
                case "2" -> translate();
                case "3" -> codeGen();
                case "4" -> summary();
                case "5" -> thesis();
                case "6" -> filePipe();
                case "8" -> showHistory();
                case "9" -> clearHistory();
                case "10" -> {
                    ConsoleUtil.printLine("再见~"); return;
                }
                default -> ConsoleUtil.printLine("输入无效");
            }
        }
    }

    private static void menu() {
        ConsoleUtil.printLine("""
                ========== DeepSeek 控制台 v2.1 ==========
                1. 自由对话（带上下文）
                2. 中英互译
                3. 代码补全/生成（自动写文件）
                4. 文本摘要
                5. 一键论文（Word）
                6. 文件管道（读→处理→写）
                8. 查看历史对话
                9. 清空历史对话
                10. 退出
                """);
    }

    /* ---------------- 功能 ---------------- */
    private static void freeChat() {
        String q = ConsoleUtil.readLine("你: ");
        try {
            String ans = CLIENT.chatWithContext(KEY, CONV.getHistory(), q);
            CONV.add(new Message("user", q));
            CONV.add(new Message("assistant", ans));
            ConsoleUtil.printLine("AI: " + ans);
        } catch (IOException e) {
            ConsoleUtil.printLine("调用失败: " + e.getMessage());
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
        String code = callCode(prompt);
        String file = "output/" + FileTool.guessFileName(lang);
        try {
            FileTool.write(file, code);
            ConsoleUtil.printLine("已生成文件: " + Paths.get(file).toAbsolutePath());
        } catch (IOException e) {
            ConsoleUtil.printLine("写文件失败: " + e.getMessage());
        }
    }

    private static void summary() {
        String q = ConsoleUtil.readLine("长文本: ");
        String prompt = "用三句话概括以下内容：\n" + q;
        ConsoleUtil.printLine("摘要: " + callChat(prompt));
    }

    private static void thesis() {
        String topic = ConsoleUtil.readLine("论文主题: ");
        ConsoleUtil.printLine("正在生成大纲与正文...");
        String outline = callChat("请为主题《" + topic + "》写一份三级大纲，用罗马数字编号：");
        String body = callChat("根据以下大纲写一篇 800 字左右论文正文：\n" + outline);
        String file = "output/" + topic.replaceAll("\\s+", "_") + ".docx";

        try {
            WordExporter.export(topic, outline, body, file);

            ConsoleUtil.printLine("Word 已生成: " + Paths.get(file).toAbsolutePath());
        } catch (IOException e) {
            ConsoleUtil.printLine("生成 Word 失败: " + e.getMessage());
        }
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
            String out = ConsoleUtil.readLine("输出文件路径: ");
            FileTool.write(out, result);
            ConsoleUtil.printLine("处理完成，已写入: " + Paths.get(out).toAbsolutePath());
        } catch (IOException e) {
            ConsoleUtil.printLine("文件操作失败: " + e.getMessage());
        }
    }

    private static void showHistory() {
        var list = CONV.getHistory();
        if (list.isEmpty()) ConsoleUtil.printLine("暂无历史");
        list.forEach(m -> ConsoleUtil.printLine(
                (m.getRole().equals("user") ? "【你】" : "【AI】") + m.getContent()));
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