package com.evoura.ververica.langchain4j.stream.function;

import com.evoura.ververica.langchain4j.infra.serde.ChatMemory;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import java.util.Objects;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.co.KeyedCoProcessFunction;
import org.apache.flink.util.Collector;

public class ChatMemoryWithLLMConfigJoinFunction
    extends KeyedCoProcessFunction<Long, ChatMemory, LLMConfig, ChatMemory> {

  public static final String UID = ChatMemoryWithLLMConfigJoinFunction.class.getSimpleName();

  private ValueState<LLMConfig> llmConfigValueState;
  private ListState<ChatMemory> chatMemoryListState;

  @Override
  public void open(final Configuration parameters) {
    ValueStateDescriptor<LLMConfig> llmConfigStateDescriptor =
        new ValueStateDescriptor<>("llmConfigValueState", LLMConfig.class);
    llmConfigValueState = getRuntimeContext().getState(llmConfigStateDescriptor);

    ListStateDescriptor<ChatMemory> chatMemoryStateDescriptor =
        new ListStateDescriptor<>("chatMemoryListState", ChatMemory.class);
    chatMemoryListState = getRuntimeContext().getListState(chatMemoryStateDescriptor);
  }

  @Override
  public void processElement1(
      final ChatMemory chatMemory,
      final KeyedCoProcessFunction<Long, ChatMemory, LLMConfig, ChatMemory>.Context ctx,
      final Collector<ChatMemory> out)
      throws Exception {

    final LLMConfig llmConfig = llmConfigValueState.value();

    if (Objects.nonNull(llmConfig)) {
      collect(chatMemory, llmConfig, out);
    } else {
      chatMemoryListState.add(chatMemory);
    }
  }

  @Override
  public void processElement2(
      final LLMConfig llmConfig,
      final KeyedCoProcessFunction<Long, ChatMemory, LLMConfig, ChatMemory>.Context ctx,
      final Collector<ChatMemory> out)
      throws Exception {

    llmConfigValueState.update(llmConfig);

    chatMemoryListState.get().forEach(chatMemory -> collect(chatMemory, llmConfig, out));
    chatMemoryListState.clear();
  }

  private static void collect(
      final ChatMemory chatMemory, final LLMConfig llmConfig, final Collector<ChatMemory> out) {
    chatMemory.setLlmConfig(llmConfig);
    out.collect(chatMemory);
  }
}
