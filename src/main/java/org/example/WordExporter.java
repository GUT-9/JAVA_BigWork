package org.example;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;

import java.io.IOException;
import java.nio.file.*;

public class WordExporter {

    /**
     * 导出论文到Word文档，支持Markdown格式
     */
    public static void export(String title, String outline, String body, String outFile) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {

            // 1. 添加标题
            XWPFParagraph titlePara = doc.createParagraph();
            titlePara.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun titleRun = titlePara.createRun();
            titleRun.setText(title);
            titleRun.setBold(true);
            titleRun.setFontSize(18);

            // 添加空行
            doc.createParagraph();

            // 2. 添加大纲部分标题
            XWPFParagraph outlineHeader = doc.createParagraph();
            XWPFRun outlineHeaderRun = outlineHeader.createRun();
            outlineHeaderRun.setText("论文大纲");
            outlineHeaderRun.setBold(true);
            outlineHeaderRun.setFontSize(14);
            outlineHeaderRun.setColor("2E74B5");

            // 3. 添加大纲内容（解析Markdown格式）
            if (outline != null && !outline.trim().isEmpty()) {
                MarkdownParser.addMarkdownToDocument(doc, outline);
            }

            // 添加分页符
            XWPFParagraph pageBreakPara = doc.createParagraph();
            pageBreakPara.createRun().addBreak(BreakType.PAGE);

            // 4. 添加正文部分标题
            XWPFParagraph bodyHeader = doc.createParagraph();
            XWPFRun bodyHeaderRun = bodyHeader.createRun();
            bodyHeaderRun.setText("论文正文");
            bodyHeaderRun.setBold(true);
            bodyHeaderRun.setFontSize(14);
            bodyHeaderRun.setColor("2E74B5");

            // 5. 添加正文内容（解析Markdown格式）
            if (body != null && !body.trim().isEmpty()) {
                MarkdownParser.addMarkdownToDocument(doc, body);
            }

            // 6. 添加参考文献和致谢占位符
            doc.createParagraph();
            XWPFParagraph refHeader = doc.createParagraph();
            refHeader.setStyle("Heading2");
            refHeader.createRun().setText("参考文献");

            XWPFParagraph refPara = doc.createParagraph();
            refPara.createRun().setText("（此处为参考文献）");

            doc.createParagraph();
            XWPFParagraph thanksHeader = doc.createParagraph();
            thanksHeader.setStyle("Heading2");
            thanksHeader.createRun().setText("致谢");

            XWPFParagraph thanksPara = doc.createParagraph();
            thanksPara.createRun().setText("（此处为致谢内容）");

            // 7. 保存文档
            Files.createDirectories(Paths.get(outFile).getParent());
            try (var out = Files.newOutputStream(Paths.get(outFile))) {
                doc.write(out);
            }

        } catch (Exception e) {
            throw new IOException("导出Word文档失败: " + e.getMessage(), e);
        }
    }

    /**
     * 简单导出（兼容旧版本）
     * @deprecated 使用新的export方法
     */
    @Deprecated
    public static void simpleExport(String title, String outline, String body, String outFile) throws IOException {
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFParagraph p = doc.createParagraph();
            p.setAlignment(ParagraphAlignment.CENTER);
            XWPFRun r = p.createRun();
            r.setText(title);
            r.setBold(true);
            r.setFontSize(18);

            XWPFParagraph p2 = doc.createParagraph();
            p2.createRun().setText("大纲：\n" + outline);

            XWPFParagraph p3 = doc.createParagraph();
            p3.createRun().setText("正文：\n" + body);

            Files.createDirectories(Paths.get(outFile).getParent());
            doc.write(Files.newOutputStream(Paths.get(outFile)));
        }
    }
}