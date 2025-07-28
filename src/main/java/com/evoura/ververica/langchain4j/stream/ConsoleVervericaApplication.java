package com.evoura.ververica.langchain4j.stream;

import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import com.google.gson.Gson;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.DescribeTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.KafkaFuture;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;

public class ConsoleVervericaApplication {

  private static final String USER_MESSAGE_TOPIC = "user-message";
  private static final String AI_RESPONSE_TOPIC = "ai-response";
  private static final String LLM_CONFIG_TOPIC = "llm-config";
  private static final String CHAT_MEMORY_TOPIC = "chat-memory";
  private static final Gson gson = new Gson();
  private static final Long userId = ThreadLocalRandom.current().nextLong(10000);
  private static final Long chatId = ThreadLocalRandom.current().nextLong(10000);

  public static void main(String[] args) {
    AppConfig appConfig = AppConfig.fromArgs(args);

    purgeTopic(USER_MESSAGE_TOPIC, appConfig);
    purgeTopic(AI_RESPONSE_TOPIC, appConfig);
    purgeTopic(CHAT_MEMORY_TOPIC, appConfig);

    Scanner scanner = new Scanner(System.in);

    // Initialize Producer and Consumer
    KafkaProducer<String, String> kafkaProducer = createKafkaProducer(appConfig);
    KafkaConsumer<String, String> aiResponseConsumer = createAiResponseConsumer(appConfig);

    // Ask user to create LLMConfig
    LLMConfig llmConfig = createLLMConfig(scanner);
    String llmConfigJson = gson.toJson(llmConfig);

    // Send LLMConfig to the llm-config Kafka topic
    kafkaProducer.send(new ProducerRecord<>(LLM_CONFIG_TOPIC, llmConfigJson));
    System.out.println("LLMConfig sent: " + llmConfigJson);

    while (true) {
      long messageId = askUserMessage(kafkaProducer, scanner);
      waitForKafkaResponse(aiResponseConsumer, gson, 20000L, messageId);
    }
  }

  private static void waitForKafkaResponse(
      KafkaConsumer<String, String> aiResponseConsumer, Gson gson, Long timeoutMs, Long messageId) {
    long startTime = System.currentTimeMillis();
    boolean responseReceived = false;

    System.out.println("Waiting for a response...");

    // Continue polling until we receive a response or timeout occurs
    while (!responseReceived && (System.currentTimeMillis() - startTime) < timeoutMs) {
      var records = aiResponseConsumer.poll(Duration.ofMillis(200));

      for (ConsumerRecord<String, String> record : records) {
        String jsonResponse = record.value();
        // Deserialize the response JSON
        ChatMessage responseMessage = gson.fromJson(jsonResponse, ChatMessage.class);

        if (userId.equals(responseMessage.getUserId())
            && messageId.equals(responseMessage.getMessageId())) {
          System.out.println("AI: " + responseMessage.getResponse());
          responseReceived = true;
          break;
        }
      }
    }

    if (!responseReceived) {
      System.out.println("No response received within the timeout period");
    }
  }

  private static LLMConfig createLLMConfig(Scanner scanner) {

    System.out.print("Use default AI model config (yes/no)? ");
    String useDefault = scanner.nextLine();

    if ("yes".equals(useDefault)) {
      // Get system message input
      System.out.print("Enter System Message: ");
      String systemMessage = scanner.nextLine();

      return new LLMConfig(userId, AiModel.OLLAMA, Map.of("baseUrl", "http://ollama:11434", "modelName", "qwen2.5:0.5b"), systemMessage);
    }

    System.out.println("Create LLM Config");

    // Get AI model input
    System.out.print("Enter AI Model (e.g., OLLAMA, OPENAI): ");
    String modelInput = scanner.nextLine();
    AiModel aiModel = AiModel.valueOf(modelInput);

    // Get system message input
    System.out.print("Enter System Message (can be empty): ");
    String systemMessage = scanner.nextLine();

    // Get custom properties
    Map<String, String> properties = new HashMap<>();
    System.out.print("Enter number of custom properties \n" +
            "Example input:\n" +
            "\t2 (parameters)\n" +
            "\tkey: baseUrl value: http://ollama:11434\n" +
            "\tkey: modelName value: smallthinker\n" +
            ": ");
    int propertyCount = scanner.nextInt();
    scanner.nextLine(); // consume newline
    for (int i = 0; i < propertyCount; i++) {
      System.out.print("Enter property key: ");
      String key = scanner.nextLine();
      System.out.print("Enter property value: ");
      String value = scanner.nextLine();
      properties.put(key, value);
    }

    return new LLMConfig(userId, aiModel, properties, systemMessage);
  }

  private static Long askUserMessage(KafkaProducer<String, String> producer, Scanner scanner) {
    String userInput;

    // Keep asking for valid input until a non-empty message is provided
    do {
      System.out.print("You: ");
      userInput = scanner.nextLine();

      if (userInput == null || userInput.trim().isEmpty()) {
        System.out.println("Message cannot be empty. Please enter a valid message.");
      }
    } while (userInput == null || userInput.trim().isEmpty());

    long messageId = ThreadLocalRandom.current().nextLong(1000);

    // Create message object and serialize to JSON
    ChatMessage message =
        new ChatMessage(userId, chatId, messageId, userInput, null, System.currentTimeMillis());
    String jsonMessage = gson.toJson(message);

    // Send JSON message to the input Kafka topic
    producer.send(new ProducerRecord<>(USER_MESSAGE_TOPIC, jsonMessage));
    System.out.println("Message sent: " + jsonMessage);

    return messageId;
  }

  private static KafkaProducer<String, String> createKafkaProducer(final AppConfig appConfig) {
    Properties props = new Properties();
    props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());
    props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
    props.putAll(appConfig.getKafkaProperties());
    return new KafkaProducer<>(props);
  }

  private static KafkaConsumer<String, String> createAiResponseConsumer(final AppConfig appConfig) {
    Properties props = new Properties();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "ververica-ai-agent-" + UUID.randomUUID());
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.putAll(appConfig.getKafkaProperties());

    KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props);
    consumer.subscribe(
        Collections.singletonList(AI_RESPONSE_TOPIC)); // Subscribe to the response topic
    return consumer;
  }

  public static void purgeTopic(String topic, final AppConfig appConfig) {
    Properties properties = new Properties();
    properties.setProperty(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, appConfig.getKafkaBrokerUrl());
    properties.setProperty(
        ConsumerConfig.GROUP_ID_CONFIG, "flink-sql-test-runner-" + UUID.randomUUID());
    properties.putAll(appConfig.getKafkaProperties());

    try (AdminClient adminClient = AdminClient.create(properties)) {
      try {
        adminClient.deleteTopics(List.of(topic)).all().get(10, TimeUnit.SECONDS);
      } catch (ExecutionException e) {
        //                System.out.println("Topic " + topic + " does not exist. Creating...");
      }

      // Wait for the topic to be deleted
      int retryCount = 0;
      int maxRetries = 5;
      while (retryCount < maxRetries) {
        DescribeTopicsResult describeTopicsResult = adminClient.describeTopics(List.of(topic));
        KafkaFuture<TopicDescription> topicDescriptionFuture =
            describeTopicsResult.values().get(topic);
        try {
          TopicDescription topicDescription = topicDescriptionFuture.get(10, TimeUnit.SECONDS);
          if (topicDescription == null) {
            break;
          }
        } catch (Exception e) {
          break;
        }
        retryCount++;
        Thread.sleep(1000);

        if (retryCount == maxRetries) {
          throw new RuntimeException(
              "Failed to purge topic " + topic + ", unable to delete old topic.");
        }
      }

      Thread.sleep(1000);
      NewTopic newTopic = new NewTopic(topic, 1, (short) 1);
      adminClient.createTopics(List.of(newTopic)).all().get(10, TimeUnit.SECONDS);

      //            System.out.println("Topic " + topic + " purged successfully");
    } catch (Exception e) {
      //            System.out.println("Failed to purge topic " + topic + ". " + e.getMessage());
    }
  }
}
