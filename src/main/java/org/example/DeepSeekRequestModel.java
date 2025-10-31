package org.example;

import lombok.Builder;
import lombok.Data;
import java.util.List;

/**
 * @author hxy
 */
@Data
@Builder
public class DeepSeekRequestModel {
    /**
     * 所用DeepSeek模型
     */
    private String model;
    private List<Message> messages;

    /**
     * 消息体
     */
    @Data
    @Builder
    public static class Message {
        private String role;
        private String content;
    }
}
