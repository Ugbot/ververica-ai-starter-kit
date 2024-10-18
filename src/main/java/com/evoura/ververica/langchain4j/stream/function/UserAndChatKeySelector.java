package com.evoura.ververica.langchain4j.stream.function;

import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import org.apache.flink.api.java.functions.KeySelector;
import org.apache.flink.api.java.tuple.Tuple2;

public class UserAndChatKeySelector implements KeySelector<ChatMessage, Tuple2<Long, Long>> {
  @Override
  public Tuple2<Long, Long> getKey(ChatMessage message) {
    return new Tuple2<>(message.getUserId(), message.getChatId());
  }
}
