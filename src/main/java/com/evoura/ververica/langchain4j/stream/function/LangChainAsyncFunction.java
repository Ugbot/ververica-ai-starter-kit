package com.evoura.ververica.langchain4j.stream.function;

import com.evoura.ververica.langchain4j.client.LangChainAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.LangChainLanguageModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.OllamaLanguageModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.OpenAiLanguageModel;
import com.evoura.ververica.langchain4j.infra.serde.ChatMemory;
import com.evoura.ververica.langchain4j.infra.serde.ChatMemoryData;
import com.evoura.ververica.langchain4j.infra.serde.ChatMessage;
import com.google.common.base.Strings;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.output.Response;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.async.ResultFuture;
import org.apache.flink.streaming.api.functions.async.RichAsyncFunction;

public class LangChainAsyncFunction extends RichAsyncFunction<ChatMemory, ChatMessage> {

  public static final String UID = LangChainAsyncFunction.class.getSimpleName();

  private transient LangChainAsyncClient langChainAsyncClient;

  @Override
  public void open(final Configuration parameters) throws Exception {
    super.open(parameters);
    this.langChainAsyncClient =
        new LangChainAsyncClient(
            List.of(
                LangChainLanguageModel.DEFAULT_MODEL,
                new OllamaLanguageModel(),
                new OpenAiLanguageModel()));
  }

  @Override
  public void asyncInvoke(
      final ChatMemory chatMemory, final ResultFuture<ChatMessage> resultFuture) {
    final List<dev.langchain4j.data.message.ChatMessage> chatMessages =
        mapChatMemoryToMessages(chatMemory);

    final CompletableFuture<Response<AiMessage>> asyncResponse =
        langChainAsyncClient.generate(chatMessages, chatMemory.getLlmConfig());

    processAsyncResponse(chatMemory, resultFuture, asyncResponse);
  }

  @Override
  public void timeout(final ChatMemory input, final ResultFuture<ChatMessage> resultFuture)
      throws Exception {
    super.timeout(input, resultFuture);
  }

  private static List<dev.langchain4j.data.message.ChatMessage> mapChatMemoryToMessages(
      final ChatMemory chatMemory) {
    List<dev.langchain4j.data.message.ChatMessage> chatMessages = new ArrayList<>();

    // Add system message
    if (!Strings.isNullOrEmpty(chatMemory.getLlmConfig().getSystemMessage())) {
      chatMessages.add(new SystemMessage(chatMemory.getLlmConfig().getSystemMessage()));
    }

    chatMemory.getChatMemoryData().stream()
        .sorted(Comparator.comparingLong(ChatMemoryData::getTimestamp))
        .forEach(
            chatMemoryData -> {
              if (Objects.nonNull(chatMemoryData.getAiMessage())) {
                chatMessages.add(new UserMessage(chatMemoryData.getUserMessage()));
                chatMessages.add(new AiMessage(chatMemoryData.getAiMessage()));
              } else {
                chatMessages.add(new UserMessage(chatMemoryData.getUserMessage()));
              }
            });

    return chatMessages;
  }

  private static void processAsyncResponse(
      final ChatMemory chatMemory,
      final ResultFuture<ChatMessage> resultFuture,
      final CompletableFuture<Response<AiMessage>> asyncResponse) {
    asyncResponse.thenAccept(
        result -> {
          resultFuture.complete(
              Collections.singleton(
                  new ChatMessage(
                      chatMemory.getUserId(),
                      chatMemory.getChatId(),
                      chatMemory
                          .getChatMemoryData()
                          .get(chatMemory.getChatMemoryData().size() - 1)
                          .getMessageId(),
                      chatMemory
                          .getChatMemoryData()
                          .get(chatMemory.getChatMemoryData().size() - 1)
                          .getUserMessage(),
                      result.content().text(),
                      System.currentTimeMillis())));
        });
  }
}
