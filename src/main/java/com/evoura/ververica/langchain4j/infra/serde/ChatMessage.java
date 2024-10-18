package com.evoura.ververica.langchain4j.infra.serde;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {

  private Long userId;
  private Long chatId;
  private Long messageId;
  private String message;
  private String response;
  private Long timestamp;
}
