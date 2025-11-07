package org.example;


import org.example.model.ConversationMeta;
import org.example.util.ConsoleUtil;

import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.IntStream;

public class HistorySelector {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("MM-dd HH:mm");

    /**
     * 通用历史列表交互
     * @return 选中的 ConversationMeta；返回 null 表示用户放弃
     */
    public static ConversationMeta select() throws IOException {
        while (true) {
            ConsoleUtil.printLine("\n------ 历史对话 ------");
            List<ConversationMeta> list = ConversationStore.listMeta();
            if (list.isEmpty()) {
                ConsoleUtil.printLine("暂无历史对话");
                return null;
            }
            IntStream.range(0, list.size())
                    .forEach(i -> ConsoleUtil.printLine(
                            (i + 1) + ". " + list.get(i).getTitle() +
                                    "  【" + Instant.ofEpochMilli(list.get(i).getLastMsgTime())
                                    .atZone(ZoneId.systemDefault()).format(FMT) + "】"));
            ConsoleUtil.printLine("提示：输入序号查看，d+序号 删除，q 返回");
            String in = ConsoleUtil.readLine("请选择: ").trim();
            if ("q".equalsIgnoreCase(in)) return null;
            if (in.startsWith("d")) {
                try {
                    int idx = Integer.parseInt(in.substring(1)) - 1;
                    if (idx < 0 || idx >= list.size()) {
                        ConsoleUtil.printLine("序号无效");
                        continue;
                    }
                    ConversationStore.delete(list.get(idx).getId());
                    ConsoleUtil.printLine("已删除: " + list.get(idx).getTitle());
                } catch (IOException e) {
                    ConsoleUtil.printLine("删除失败: " + e.getMessage());
                }
                continue;
            }
            // 正常序号 → 返回选中的 meta
            try {
                int idx = Integer.parseInt(in) - 1;
                if (idx < 0 || idx >= list.size()) {
                    ConsoleUtil.printLine("序号超出范围");
                    continue;
                }
                return list.get(idx);
            } catch (NumberFormatException e) {
                ConsoleUtil.printLine("输入无效");
            }
        }
    }
}