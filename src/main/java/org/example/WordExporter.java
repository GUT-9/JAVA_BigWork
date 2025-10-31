package org.example;

import org.apache.poi.xwpf.usermodel.*;
import java.io.IOException;
import java.nio.file.*;

public class WordExporter {
    public static void export(String title, String outline, String body, String outFile) throws IOException {
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