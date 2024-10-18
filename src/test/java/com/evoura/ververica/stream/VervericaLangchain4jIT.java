package com.evoura.ververica.stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.evoura.ververica.TestHelper;
import com.evoura.ververica.langchain4j.infra.configuration.AppConfig;
import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import com.evoura.ververica.langchain4j.stream.VervericaLangchain4jApplication;
import java.util.List;
import java.util.Map;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers
public class VervericaLangchain4jIT {

  private static final String USER_MESSAGE_TOPIC = "user-message";
  private static final String AI_RESPONSE_TOPIC = "ai-response";
  private static final String LLM_CONFIG_TOPIC = "llm-config";
  private static final String CHAT_MEMORY_TOPIC = "chat-memory";

  @Container
  protected static final KafkaContainer KAFKA =
      new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.5.1"))
          .withNetwork(Network.newNetwork());

  @Test
  void testVervericaLangchain4j() throws Exception {
    assertTrue(KAFKA.isRunning());

    AppConfig appConfig =
        AppConfig.fromMap(Map.of("kafka-broker-url", KAFKA.getBootstrapServers()));

    TestHelper.createKafkaTopic(appConfig, USER_MESSAGE_TOPIC);
    TestHelper.createKafkaTopic(appConfig, AI_RESPONSE_TOPIC);
    TestHelper.createKafkaTopic(appConfig, LLM_CONFIG_TOPIC);
    TestHelper.createKafkaTopic(appConfig, CHAT_MEMORY_TOPIC);

    // Execute Flink job
    StreamExecutionEnvironment pipeline =
        VervericaLangchain4jApplication.createFlinkPipeline(appConfig);
    pipeline.executeAsync();

    Long chatId = 1L;
    Long userId = 2L;
    Long userMessageId = 3L;

    // Publish LLM configuration
    LLMConfig llmConfig = new LLMConfig();
    llmConfig.setAiModel(AiModel.DEFAULT);
    llmConfig.setUserId(userId);
    llmConfig.setSystemMessage("This is the system message");
    TestHelper.publishKafkaRecords(List.of(llmConfig), appConfig, LLM_CONFIG_TOPIC);

    // Publish user message
    ChatMessage userMessage = new ChatMessage();
    userMessage.setChatId(chatId);
    userMessage.setUserId(userId);
    userMessage.setMessageId(userMessageId);
    userMessage.setMessage("Hello!");
    TestHelper.publishKafkaRecords(List.of(userMessage), appConfig, USER_MESSAGE_TOPIC);

    final Map<String, ChatMessage> aiResponses =
        TestHelper.getAiResponses(appConfig, AI_RESPONSE_TOPIC, 1);

    ChatMessage expectedAiResponse = new ChatMessage();
    expectedAiResponse.setResponse(
        "You shared this message with the AI model: " + userMessage.getMessage());
    expectedAiResponse.setMessage(userMessage.getMessage());
    expectedAiResponse.setMessageId(userMessage.getMessageId());
    expectedAiResponse.setUserId(userMessage.getUserId());
    expectedAiResponse.setChatId(userMessage.getChatId());

    assertThat(aiResponses.size()).isEqualTo(1);

    final var aiResponse = aiResponses.values().stream().findFirst().orElse(new ChatMessage());
    assertThat(aiResponse.getResponse()).isEqualTo(expectedAiResponse.getResponse());
    assertThat(aiResponse.getMessage()).isEqualTo(expectedAiResponse.getMessage());
    assertThat(aiResponse.getUserId()).isEqualTo(expectedAiResponse.getUserId());
    assertThat(aiResponse.getMessageId()).isEqualTo(expectedAiResponse.getMessageId());
    assertThat(aiResponse.getChatId()).isEqualTo(expectedAiResponse.getChatId());
  }
}
