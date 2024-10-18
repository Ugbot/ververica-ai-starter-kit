package com.evoura.ververica;

import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.google.gson.Gson;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
import lombok.val;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.testcontainers.shaded.org.awaitility.Awaitility;

public abstract class TestHelper {

  private static final Gson GSON = new Gson();

  public static void publishKafkaRecords(List<Object> records, AppConfig appConfig, String topic) {
    try (var kafkaProducer = createKafkaProducer(appConfig)) {
      records.forEach(
          record -> {
            String llmConfigJson = GSON.toJson(record);
            kafkaProducer.send(new ProducerRecord<>(topic, llmConfigJson));
          });
    }
  }

  public static Map<String, ChatMessage> getAiResponses(
      AppConfig appConfig, String topic, int expectedRecords) {
    return getKafkaRecords(appConfig, topic, expectedRecords).stream()
        .collect(
            Collectors.toMap(
                ConsumerRecord<String, String>::key,
                record -> GSON.fromJson(record.value(), ChatMessage.class)));
  }

  public static List<ConsumerRecord<String, String>> getKafkaRecords(
      AppConfig appConfig, String topic, int expectedRecords) {
    List<ConsumerRecord<String, String>> result = new ArrayList<>();

    try (val consumer = createKafkaConsumer(appConfig)) {
      consumer.subscribe(List.of(topic));

      Awaitility.await()
          .pollDelay(Duration.ofSeconds(1))
          .pollInterval(Duration.ofSeconds(1))
          .timeout(Duration.ofSeconds(15))
          .until(
              () -> {
                val consumerRecords = consumer.poll(Duration.ofSeconds(1));
                consumerRecords.iterator().forEachRemaining(result::add);
                return result.size() == expectedRecords;
              });
    }

    return result;
  }

  public static void createKafkaTopic(AppConfig appConfig, String topicName)
      throws ExecutionException, InterruptedException {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());

    try (AdminClient adminClient = AdminClient.create(properties)) {
      NewTopic newTopic = new NewTopic(topicName, Optional.empty(), Optional.empty());
      adminClient.createTopics(Collections.singleton(newTopic)).all().get();
    }
  }

  private static KafkaProducer<String, String> createKafkaProducer(final AppConfig appConfig) {
    Properties properties = new Properties();
    properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());
    properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    properties.putAll(appConfig.getKafkaProperties());
    return new KafkaProducer<>(properties);
  }

  private static KafkaConsumer<String, String> createKafkaConsumer(final AppConfig appConfig) {
    Properties properties = new Properties();
    properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());
    properties.put(ConsumerConfig.GROUP_ID_CONFIG, "ververica-ai-agent-" + UUID.randomUUID());
    properties.put(
        ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(
        ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    return new KafkaConsumer<>(properties);
  }
}
