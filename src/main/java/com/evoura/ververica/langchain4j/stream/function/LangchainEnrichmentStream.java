package com.evoura.ververica.langchain4j.stream.function;

import com.evoura.sink.EvouraKafkaSink;
import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.serde.ChatMemory;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import java.util.concurrent.TimeUnit;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.formats.json.JsonDeserializationSchema;
import org.apache.flink.streaming.api.datastream.AsyncDataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.async.AsyncRetryStrategy;
import org.apache.flink.streaming.util.retryable.AsyncRetryStrategies;
import org.apache.flink.streaming.util.retryable.RetryPredicates;

public class LangchainEnrichmentStream {

  public static final String CONSUMER_GROUP_ID = "ververica-langchain4j-app";

  private final StreamExecutionEnvironment env;
  private final AppConfig appConfig;

  public LangchainEnrichmentStream(StreamExecutionEnvironment env, AppConfig appConfig) {
    this.env = env;
    this.appConfig = appConfig;
  }

  public SingleOutputStreamOperator<ChatMessage> createEnrichedStream(
      String userMessageTopic, String chatMemoryTopic, String llmConfigTopic) {
    final SingleOutputStreamOperator<ChatMessage> userMessageStream =
        createStreamFromKafkaSource(env, appConfig, userMessageTopic, ChatMessage.class);

    final SingleOutputStreamOperator<ChatMessage> memoryStream =
        createStreamFromKafkaSource(env, appConfig, chatMemoryTopic, ChatMessage.class);

    final SingleOutputStreamOperator<LLMConfig> llmConfigStream =
        createStreamFromKafkaSource(env, appConfig, llmConfigTopic, LLMConfig.class);

    // Process chat memory
    final SingleOutputStreamOperator<ChatMemory> chatMemoryStream =
        userMessageStream
            .keyBy(new UserAndChatKeySelector())
            .connect(memoryStream.keyBy(new UserAndChatKeySelector()))
            .process(new ChatMemoryProcessingFunction())
            .uid(ChatMemoryProcessingFunction.UID)
            .name(ChatMemoryProcessingFunction.UID);

    chatMemoryStream
        .getSideOutput(ChatMemoryProcessingFunction.MEMORY_MESSAGE_OT)
        .sinkTo(EvouraKafkaSink.create(appConfig, chatMemoryTopic))
        .uid("UserMemorySink")
        .name("UserMemorySink");

    // Enrich chat memory with LLM configuration
    final SingleOutputStreamOperator<ChatMemory> enrichedChatMemoryStream =
        chatMemoryStream
            .keyBy(ChatMemory::getUserId)
            .connect(llmConfigStream.keyBy(LLMConfig::getUserId))
            .process(new ChatMemoryWithLLMConfigJoinFunction())
            .uid(ChatMemoryWithLLMConfigJoinFunction.UID)
            .name(ChatMemoryWithLLMConfigJoinFunction.UID);

    SingleOutputStreamOperator<ChatMessage> asyncAiResponseStream =
        createAsyncAiResponseStream(enrichedChatMemoryStream);

    // Sink AI response to chat-memory kafka topic
    asyncAiResponseStream
        .sinkTo(EvouraKafkaSink.create(appConfig, chatMemoryTopic))
        .uid("AIResponseMemorySink")
        .name("AIResponseMemorySink");

    return asyncAiResponseStream;
  }

  private static <TYPE> SingleOutputStreamOperator<TYPE> createStreamFromKafkaSource(
      final StreamExecutionEnvironment env,
      final AppConfig appConfig,
      final String topic,
      final Class<TYPE> messageType) {
    KafkaSource<TYPE> kafkaSource =
        KafkaSource.<TYPE>builder()
            .setBootstrapServers(appConfig.getKafkaBrokerUrl())
            .setProperties(appConfig.getKafkaProperties())
            .setTopics(topic)
            .setGroupId(CONSUMER_GROUP_ID)
            .setStartingOffsets(OffsetsInitializer.earliest())
            .setValueOnlyDeserializer(new JsonDeserializationSchema<>(messageType))
            .build();

    final String uid = topic + "-source";
    return env.fromSource(kafkaSource, WatermarkStrategy.noWatermarks(), uid).uid(uid);
  }

  private static SingleOutputStreamOperator<ChatMessage> createAsyncAiResponseStream(
      final SingleOutputStreamOperator<ChatMemory> finalChatMemoryStream) {

    // Async retry strategy with maxAttempts=3, fixedDelay=100ms
    final AsyncRetryStrategy<ChatMessage> asyncRetryStrategy =
        new AsyncRetryStrategies.FixedDelayRetryStrategyBuilder<ChatMessage>(3, 1000L)
            .ifException(RetryPredicates.HAS_EXCEPTION_PREDICATE)
            .build();

    return AsyncDataStream.orderedWaitWithRetry(
            finalChatMemoryStream,
            new LangChainAsyncFunction(),
            10000,
            TimeUnit.MILLISECONDS,
            100,
            asyncRetryStrategy)
        .uid(LangChainAsyncFunction.UID)
        .name(LangChainAsyncFunction.UID);
  }
}
