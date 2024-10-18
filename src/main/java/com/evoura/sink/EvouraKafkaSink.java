package com.evoura.sink;

import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import org.apache.flink.connector.kafka.sink.KafkaRecordSerializationSchema;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.formats.json.JsonSerializationSchema;

public class EvouraKafkaSink {
  private EvouraKafkaSink() {}

  public static KafkaSink<ChatMessage> create(AppConfig appConfig, String topic) {
    return org.apache.flink.connector.kafka.sink.KafkaSink.<ChatMessage>builder()
        .setBootstrapServers(appConfig.getKafkaBrokerUrl())
        .setKafkaProducerConfig(appConfig.getKafkaProperties())
        .setRecordSerializer(
            KafkaRecordSerializationSchema.builder()
                .setTopic(topic)
                .setValueSerializationSchema(new JsonSerializationSchema<ChatMessage>())
                .build())
        .build();
  }
}
