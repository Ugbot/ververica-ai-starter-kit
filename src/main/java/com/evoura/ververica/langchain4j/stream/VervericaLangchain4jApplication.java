package com.evoura.ververica.langchain4j.stream;

import com.evoura.sink.EvouraKafkaSink;
import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.evoura.ververica.langchain4j.stream.function.LangchainEnrichmentStream;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.configuration.PipelineOptions;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

public class VervericaLangchain4jApplication {

  public static void main(String[] args) throws Exception {
    AppConfig appConfig = AppConfig.fromArgs(args);

    StreamExecutionEnvironment pipeline = createFlinkPipeline(appConfig);
    pipeline.execute("VervericaLangchain4j");
  }

  public static StreamExecutionEnvironment createFlinkPipeline(AppConfig appConfig) {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

    // Disable Kryo
    Configuration config = new Configuration();
    config.set(PipelineOptions.GENERIC_TYPES, false);
    config.set(PipelineOptions.AUTO_GENERATE_UIDS, false);
    env.configure(config);

    SingleOutputStreamOperator<ChatMessage> asyncAiResponseStream =
        new LangchainEnrichmentStream(env, appConfig)
            .createEnrichedStream("user-message", "chat-memory", "llm-config");

    // Sink AI response to ai-response kafka topic
    KafkaSink<ChatMessage> aiResponseSink = EvouraKafkaSink.create(appConfig, "ai-response");
    asyncAiResponseStream.sinkTo(aiResponseSink).uid("AIResponseSink").name("AIResponseSink");

    return env;
  }
}
