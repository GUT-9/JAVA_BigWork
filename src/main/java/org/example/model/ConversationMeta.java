package org.example.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ConversationMeta {
    private String id;                  // UUID
    private String title;               // 自动取第一条用户消息前20字
    private long createTime;
    private long lastMsgTime;
}
