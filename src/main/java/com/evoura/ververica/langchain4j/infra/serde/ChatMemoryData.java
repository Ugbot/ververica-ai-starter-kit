package com.evoura.ververica.langchain4j.infra.serde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMemoryData {

  private Long messageId;
  private String userMessage;
  private String aiMessage;
  private Long timestamp;
}
