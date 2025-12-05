package org.example;

import org.apache.poi.xwpf.usermodel.*;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown格式解析器，用于将Markdown转换为Word样式
 */
public class MarkdownParser {

    // 定义各种Markdown模式
    private static final Pattern HEADER_PATTERN = Pattern.compile("^(#{1,6})\\s+(.*)$");
    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("\\*(.*?)\\*");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern CODE_BLOCK_PATTERN = Pattern.compile("```(?:\\w+)?\\s*\\n([\\s\\S]*?)\\n```");
    private static final Pattern LINK_PATTERN = Pattern.compile("\\[([^\\]]+)\\]\\(([^\\)]+)\\)");
    private static final Pattern LIST_ITEM_PATTERN = Pattern.compile("^\\s*[-*+]\\s+(.*)$");
    private static final Pattern NUMBERED_LIST_PATTERN = Pattern.compile("^\\s*(\\d+)\\.\\s+(.*)$");
    private static final Pattern QUOTE_PATTERN = Pattern.compile("^>\\s+(.*)$");
    private static final Pattern TABLE_PATTERN = Pattern.compile("\\|(.+)\\|");
    private static final Pattern HORIZONTAL_RULE_PATTERN = Pattern.compile("^[-*_]{3,}\\s*$");

    /**
     * 将Markdown文本转换为Word文档格式
     */
    public static void addMarkdownToDocument(XWPFDocument document, String markdownText) {
        String[] lines = markdownText.split("\n");
        XWPFParagraph currentParagraph = null;
        boolean inCodeBlock = false;
        List<String> codeBlockLines = new ArrayList<>();
        String currentLanguage = "";

        for (String line : lines) {
            // 检查是否是代码块开始或结束
            if (line.trim().startsWith("```")) {
                if (!inCodeBlock) {
                    // 代码块开始
                    inCodeBlock = true;
                    currentLanguage = extractLanguage(line);
                    codeBlockLines.clear();
                } else {
                    // 代码块结束
                    inCodeBlock = false;
                    addCodeBlockToDocument(document, codeBlockLines, currentLanguage);
                    currentLanguage = "";
                    continue;
                }
            } else if (inCodeBlock) {
                // 代码块中的行
                codeBlockLines.add(line);
                continue;
            }

            // 检查空行
            if (line.trim().isEmpty()) {
                if (currentParagraph != null && currentParagraph.getText().isEmpty()) {
                    continue; // 跳过连续的空段落
                }
                currentParagraph = document.createParagraph();
                currentParagraph.createRun(); // 创建空段落
                continue;
            }

            // 检查标题
            Matcher headerMatcher = HEADER_PATTERN.matcher(line);
            if (headerMatcher.matches()) {
                currentParagraph = document.createParagraph();
                String headerLevel = headerMatcher.group(1);
                String headerText = headerMatcher.group(2);

                // 设置标题样式
                currentParagraph.setStyle(getHeaderStyle(headerLevel.length()));

                // 添加标题文本（解析内联格式）
                addTextWithFormatting(currentParagraph, headerText);
                continue;
            }

            // 检查引用
            Matcher quoteMatcher = QUOTE_PATTERN.matcher(line);
            if (quoteMatcher.matches()) {
                currentParagraph = document.createParagraph();
                currentParagraph.setIndentationLeft(200); // 缩进

                XWPFRun run = currentParagraph.createRun();
                run.setText(quoteMatcher.group(1));
                run.setItalic(true);
                continue;
            }

            // 检查无序列表
            Matcher listMatcher = LIST_ITEM_PATTERN.matcher(line);
            if (listMatcher.matches()) {
                currentParagraph = document.createParagraph();
                currentParagraph.setIndentationLeft(200);

                XWPFRun bulletRun = currentParagraph.createRun();
                bulletRun.setText("• ");
                bulletRun.setBold(true);

                addTextWithFormatting(currentParagraph, listMatcher.group(1));
                continue;
            }

            // 检查有序列表
            Matcher numberedMatcher = NUMBERED_LIST_PATTERN.matcher(line);
            if (numberedMatcher.matches()) {
                currentParagraph = document.createParagraph();
                currentParagraph.setIndentationLeft(200);

                XWPFRun numberRun = currentParagraph.createRun();
                numberRun.setText(numberedMatcher.group(1) + ". ");
                numberRun.setBold(true);

                addTextWithFormatting(currentParagraph, numberedMatcher.group(2));
                continue;
            }

            // 检查水平线
            if (HORIZONTAL_RULE_PATTERN.matcher(line).matches()) {
                addHorizontalRule(document);
                continue;
            }

            // 普通段落
            currentParagraph = document.createParagraph();
            addTextWithFormatting(currentParagraph, line);
        }

        // 如果最后还在代码块中，添加剩余的代码块
        if (inCodeBlock && !codeBlockLines.isEmpty()) {
            addCodeBlockToDocument(document, codeBlockLines, currentLanguage);
        }
    }

    /**
     * 向段落中添加带有格式的文本
     */
    private static void addTextWithFormatting(XWPFParagraph paragraph, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }

        // 处理代码块（在行内）
        String remainingText = text;

        // 先处理链接（因为链接可能包含其他格式）
        remainingText = processLinkPattern(paragraph, remainingText);

        // 处理粗体
        remainingText = processSinglePattern(paragraph, remainingText, BOLD_PATTERN,
                (run, content) -> run.setBold(true));

        // 处理斜体
        remainingText = processSinglePattern(paragraph, remainingText, ITALIC_PATTERN,
                (run, content) -> run.setItalic(true));

        // 处理内联代码
        remainingText = processSinglePattern(paragraph, remainingText, CODE_PATTERN,
                (run, content) -> {
                    run.setFontFamily("Courier New");
                    run.setFontSize(10);
                    run.setColor("FF0000");
                });

        // 添加剩余文本（无特殊格式）
        if (remainingText != null && !remainingText.isEmpty()) {
            XWPFRun defaultRun = paragraph.createRun();
            defaultRun.setText(remainingText);
        }
    }

    /**
     * 处理单个捕获组的正则匹配模式
     */
    private static String processSinglePattern(XWPFParagraph paragraph, String text, Pattern pattern,
                                               SinglePatternFormatter formatter) {
        Matcher matcher = pattern.matcher(text);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // 添加匹配前的普通文本
            if (matcher.start() > lastEnd) {
                XWPFRun run = paragraph.createRun();
                run.setText(text.substring(lastEnd, matcher.start()));
            }

            // 添加匹配的格式化文本
            XWPFRun run = paragraph.createRun();
            String content = matcher.group(1);
            run.setText(content);
            formatter.format(run, content);

            lastEnd = matcher.end();
        }

        if (lastEnd == 0) {
            return text; // 没有匹配
        }

        // 添加剩余文本
        if (lastEnd < text.length()) {
            result.append(text.substring(lastEnd));
        }

        return result.toString();
    }

    /**
     * 处理链接模式
     */
    private static String processLinkPattern(XWPFParagraph paragraph, String text) {
        Matcher matcher = LINK_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        int lastEnd = 0;

        while (matcher.find()) {
            // 添加匹配前的普通文本
            if (matcher.start() > lastEnd) {
                XWPFRun run = paragraph.createRun();
                run.setText(text.substring(lastEnd, matcher.start()));
            }

            // 添加链接文本（带下划线）
            XWPFRun run = paragraph.createRun();
            String linkText = matcher.group(1);
            run.setText(linkText);
            run.setUnderline(UnderlinePatterns.SINGLE);
            run.setColor("0000FF");

            lastEnd = matcher.end();
        }

        if (lastEnd == 0) {
            return text; // 没有匹配
        }

        // 添加剩余文本
        if (lastEnd < text.length()) {
            result.append(text.substring(lastEnd));
        }

        return result.toString();
    }

    /**
     * 添加代码块到文档
     */
    private static void addCodeBlockToDocument(XWPFDocument document, List<String> codeLines, String language) {
        if (codeLines.isEmpty()) {
            return;
        }

        XWPFParagraph codePara = document.createParagraph();
        codePara.setIndentationLeft(200);

        // 添加语言标签（如果有）
        if (!language.isEmpty()) {
            XWPFRun langRun = codePara.createRun();
            langRun.setText("[" + language + "] ");
            langRun.setColor("666666");
            langRun.setItalic(true);
        }

        // 添加代码内容
        XWPFRun codeRun = codePara.createRun();
        StringBuilder codeBuilder = new StringBuilder();
        for (String line : codeLines) {
            codeBuilder.append(line).append("\n");
        }
        codeRun.setText(codeBuilder.toString());
        codeRun.setFontFamily("Courier New");
        codeRun.setFontSize(10);
        codeRun.setColor("000000");
    }

    /**
     * 添加水平分隔线
     */
    private static void addHorizontalRule(XWPFDocument document) {
        XWPFParagraph hrPara = document.createParagraph();
        XWPFRun hrRun = hrPara.createRun();
        hrRun.setText("________________________________________");
        hrRun.setColor("CCCCCC");
        hrRun.addBreak();
    }

    /**
     * 从代码块标记中提取语言
     */
    private static String extractLanguage(String line) {
        String trimmed = line.trim();
        if (trimmed.startsWith("```") && trimmed.length() > 3) {
            String lang = trimmed.substring(3).trim();
            return lang.isEmpty() ? "" : lang;
        }
        return "";
    }

    /**
     * 获取标题样式名称
     */
    private static String getHeaderStyle(int level) {
        return switch (level) {
            case 1 -> "Heading1";
            case 2 -> "Heading2";
            case 3 -> "Heading3";
            case 4 -> "Heading4";
            case 5 -> "Heading5";
            case 6 -> "Heading6";
            default -> "Normal";
        };
    }

    /**
     * 单个模式格式化回调接口
     */
    @FunctionalInterface
    private interface SinglePatternFormatter {
        void format(XWPFRun run, String content);
    }
}