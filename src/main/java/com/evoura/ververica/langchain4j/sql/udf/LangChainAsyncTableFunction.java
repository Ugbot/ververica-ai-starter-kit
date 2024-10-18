package com.evoura.ververica.langchain4j.sql.udf;

import static java.util.stream.Collectors.joining;

import com.evoura.ververica.langchain4j.client.LangChainAsyncClient;
import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import com.google.common.base.Strings;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.input.Prompt;
import dev.langchain4j.model.input.PromptTemplate;
import dev.langchain4j.model.output.Response;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.functions.AsyncLookupFunction;

public class LangChainAsyncTableFunction extends AsyncLookupFunction implements Serializable {

  private final LangChainAsyncClient langChainAsyncClient;
  private final LLMConfig llmConfig;

  private final LangChainEmbeddingAsyncClient embeddingAsyncClient;
  private final EmbeddingConfig embeddingConfig;

  public LangChainAsyncTableFunction(
      LLMConfig llmConfig,
      LangChainAsyncClient llmAsyncClient,
      EmbeddingConfig embeddingConfig,
      LangChainEmbeddingAsyncClient embeddingAsyncClient) {
    this.langChainAsyncClient = llmAsyncClient;
    this.llmConfig = llmConfig;
    this.embeddingAsyncClient = embeddingAsyncClient;
    this.embeddingConfig = embeddingConfig;
  }

  @Override
  public CompletableFuture<Collection<RowData>> asyncLookup(final RowData keyRow) {
    String userMessage = ((GenericRowData) keyRow).getField(0).toString();

    final var enrichedUserMessage = enrichUserMessage(userMessage);

    final var userChatMemory = getChatMemory(enrichedUserMessage);

    final CompletableFuture<Response<AiMessage>> asyncResponse =
        langChainAsyncClient.generate(userChatMemory, llmConfig);

    CompletableFuture<Collection<RowData>> future = new CompletableFuture<>();
    processAsyncResponse(keyRow, future, asyncResponse);

    return future;
  }

  private List<ChatMessage> getChatMemory(UserMessage userMessage) {
    List<ChatMessage> chatMemory = new ArrayList<>();

    // Add system message
    if (!Strings.isNullOrEmpty(llmConfig.getSystemMessage())) {
      chatMemory.add(new SystemMessage(llmConfig.getSystemMessage()));
    }

    // Add user message
    chatMemory.add(userMessage);

    return chatMemory;
  }

  private static void processAsyncResponse(
      final RowData keyRow,
      final CompletableFuture<Collection<RowData>> resultFuture,
      final CompletableFuture<Response<AiMessage>> asyncResponse) {
    asyncResponse.thenAccept(
        result -> {
          GenericRowData resultRow = new GenericRowData(2);
          resultRow.setField(
              0, StringData.fromString(((GenericRowData) keyRow).getField(0).toString()));
          resultRow.setField(1, StringData.fromString(result.content().text()));
          resultFuture.complete(Collections.singleton(resultRow));
        });
  }

  private UserMessage enrichUserMessage(String userMessage) {
    final var relevantEmbeddings = embeddingAsyncClient.search(userMessage, embeddingConfig);
    final var promptTemplateValue = llmConfig.getProperties().get("promptTemplate");

    if (relevantEmbeddings.isEmpty() || Strings.isNullOrEmpty(promptTemplateValue)) {
      return UserMessage.from(userMessage);
    }

    PromptTemplate promptTemplate = PromptTemplate.from(promptTemplateValue);

    String information = relevantEmbeddings.stream().map(TextSegment::text).collect(joining("\n"));

    Map<String, Object> variables = new HashMap<>();
    variables.put("message", userMessage);
    variables.put("information", information);

    Prompt prompt = promptTemplate.apply(variables);

    return prompt.toUserMessage();
  }
}
