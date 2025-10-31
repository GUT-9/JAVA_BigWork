package org.example.model;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class DeepSeekRequest {
    private String model;
    private List<Message> messages;
    private Double temperature;
    private Double top_p;
}