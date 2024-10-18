package com.evoura.ververica.langchain4j.stream.function;

import com.evoura.ververica.langchain4j.infra.serde.ChatMemory;
import com.evoura.ververica.langchain4j.infra.serde.ChatMemoryData;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

public class ChatMemoryProcessingFunction
    extends KeyedCoProcessFunction<Tuple2<Long, Long>, ChatMessage, ChatMessage, ChatMemory> {

  public static final String UID = ChatMemoryProcessingFunction.class.getSimpleName();
  public static final OutputTag<ChatMessage> MEMORY_MESSAGE_OT =
      new OutputTag<>("memory-message-side-output") {};

  private MapState<Long, ChatMessage> chatMemoryState;

  @Override
  public void open(final Configuration parameters) {
    MapStateDescriptor<Long, ChatMessage> chatMemoryStateDescriptor =
        new MapStateDescriptor<>("chatMemoryState", Long.class, ChatMessage.class);
    chatMemoryState = getRuntimeContext().getMapState(chatMemoryStateDescriptor);
  }

  @Override
  public void processElement1(
      final ChatMessage userMessage,
      final KeyedCoProcessFunction<Tuple2<Long, Long>, ChatMessage, ChatMessage, ChatMemory>.Context
          context,
      final Collector<ChatMemory> collector)
      throws Exception {

    if (!chatMemoryState.contains(userMessage.getMessageId())) {

      // Process memory message
      final ChatMessage memoryMessage = new ChatMessage();
      memoryMessage.setMessage(userMessage.getMessage());
      memoryMessage.setTimestamp(userMessage.getTimestamp());

      chatMemoryState.put(userMessage.getMessageId(), memoryMessage);

      memoryMessage.setUserId(userMessage.getUserId());
      memoryMessage.setChatId(userMessage.getChatId());
      memoryMessage.setMessageId(userMessage.getMessageId());

      context.output(MEMORY_MESSAGE_OT, memoryMessage);

      // Process chat memory data
      List<ChatMemoryData> chatMemoryDataList = new ArrayList<>();
      chatMemoryState
          .entries()
          .forEach(
              data -> {
                final ChatMemoryData chatMemoryData =
                    new ChatMemoryData(
                        data.getKey(),
                        data.getValue().getMessage(),
                        data.getValue().getResponse(),
                        data.getValue().getTimestamp());
                chatMemoryDataList.add(0, chatMemoryData);
              });

      chatMemoryDataList.sort(Comparator.comparingLong(ChatMemoryData::getTimestamp));

      final ChatMemory chatMemory = new ChatMemory();
      chatMemory.setUserId(userMessage.getUserId());
      chatMemory.setChatId(userMessage.getChatId());
      chatMemory.setChatMemoryData(chatMemoryDataList);

      collector.collect(chatMemory);
    }
  }

  @Override
  public void processElement2(
      final ChatMessage memoryMessage,
      final KeyedCoProcessFunction<Tuple2<Long, Long>, ChatMessage, ChatMessage, ChatMemory>.Context
          context,
      final Collector<ChatMemory> collector)
      throws Exception {

    final var stateMemoryMessage = chatMemoryState.get(memoryMessage.getMessageId());

    // Process only if the memory message has the AI response
    if (Objects.nonNull(stateMemoryMessage)
        && Objects.nonNull(stateMemoryMessage.getMessage())
        && Objects.nonNull(memoryMessage.getResponse())) {
      stateMemoryMessage.setResponse(memoryMessage.getResponse());
      chatMemoryState.put(memoryMessage.getMessageId(), stateMemoryMessage);
    }
  }
}
