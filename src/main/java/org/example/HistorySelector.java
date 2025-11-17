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

    /** 返回该实例表示“用户想新建对话” */
    private static final ConversationMeta NEW_MARKER =
            ConversationMeta.builder()
                    .id("NEW")
                    .title("新建对话")
                    .createTime(0)
                    .lastMsgTime(0)
                    .build();

    /**
     * 统一选择器：
     * 返回 NEW_MARKER 表示新建；返回 null 表示放弃；返回其它表示选中的历史。
     */
    public static ConversationMeta select() throws IOException {
        while (true) {
            ConsoleUtil.printLine("\n------ 历史对话 ------");
            List<ConversationMeta> list = ConversationStore.listMeta();

            /* 1. 把 0 放最前面 */
            ConsoleUtil.printLine("0. 新建对话");
            IntStream.range(0, list.size())
                    .forEach(i -> ConsoleUtil.printLine(
                            (i + 1) + ". " + list.get(i).getTitle() +
                                    "  【" + Instant.ofEpochMilli(list.get(i).getLastMsgTime())
                                    .atZone(ZoneId.systemDefault()).format(FMT) + "】"));

            ConsoleUtil.printLine("提示：输入序号查看，d+序号 删除，q 返回");
            String in = ConsoleUtil.readLine("请选择: ").trim();
            if ("q".equalsIgnoreCase(in)) return null;

            /* 删除逻辑保持不变 */
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

            /* 2. 处理 0 → 新建 */
            if ("0".equals(in)) return NEW_MARKER;

            /* 3. 处理 1/2/3… → 历史 */
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