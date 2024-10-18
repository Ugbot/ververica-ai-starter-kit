package com.evoura.ververica.langchain4j.sql.udf;

import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import dev.langchain4j.data.embedding.Embedding;
import dev.langchain4j.model.output.Response;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.data.StringData;
import org.apache.flink.table.functions.AsyncLookupFunction;

public class LangChainEmbeddingAsyncTableFunction extends AsyncLookupFunction
    implements Serializable {

  private final LangChainEmbeddingAsyncClient embeddingAsyncClient;
  private final EmbeddingConfig embeddingConfig;

  public LangChainEmbeddingAsyncTableFunction(
      EmbeddingConfig embeddingConfig, LangChainEmbeddingAsyncClient asyncClient) {
    this.embeddingAsyncClient = asyncClient;
    this.embeddingConfig = embeddingConfig;
  }

  @Override
  public CompletableFuture<Collection<RowData>> asyncLookup(final RowData keyRow) {
    CompletableFuture<Collection<RowData>> future = new CompletableFuture<>();

    final var textInput = ((GenericRowData) keyRow).getField(0).toString();

    final CompletableFuture<Response<Embedding>> asyncResponse =
        embeddingAsyncClient.embed(textInput, embeddingConfig);

    processAsyncResponse(keyRow, future, asyncResponse);

    return future;
  }

  private static void processAsyncResponse(
      final RowData keyRow,
      final CompletableFuture<Collection<RowData>> resultFuture,
      final CompletableFuture<Response<Embedding>> asyncResponse) {
    asyncResponse.thenAccept(
        result -> {
          GenericRowData resultRow = new GenericRowData(2);
          resultRow.setField(
              0, StringData.fromString(((GenericRowData) keyRow).getField(0).toString()));
          resultRow.setField(1, StringData.fromString(result.content().vectorAsList().toString()));
          resultFuture.complete(Collections.singleton(resultRow));
        });
  }
}
