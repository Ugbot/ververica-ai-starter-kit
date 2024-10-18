package com.evoura.ververica.langchain4j.sql.connector.language;

import com.evoura.ververica.langchain4j.client.LangChainAsyncClient;
import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
import com.evoura.ververica.langchain4j.sql.udf.LangChainAsyncTableFunction;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.lookup.AsyncLookupFunctionProvider;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.types.DataType;

public class LangChainAsyncTableSource implements LookupTableSource {

  private final DataType physicalRowDataType;
  private final DynamicTableFactory.Context dynamicTableFactoryContext;
  private final LLMConfig llmConfig;
  private final LangChainAsyncClient asyncClient;
  private final EmbeddingConfig embeddingConfig;
  private final LangChainEmbeddingAsyncClient embeddingAsyncClient;

  public LangChainAsyncTableSource(
      final DataType physicalRowDataType,
      final DynamicTableFactory.Context context,
      final LLMConfig llmConfig,
      final LangChainAsyncClient asyncClient,
      final EmbeddingConfig embeddingConfig,
      final LangChainEmbeddingAsyncClient embeddingAsyncClient) {
    this.physicalRowDataType = physicalRowDataType;
    this.dynamicTableFactoryContext = context;
    this.llmConfig = llmConfig;
    this.asyncClient = asyncClient;
    this.embeddingConfig = embeddingConfig;
    this.embeddingAsyncClient = embeddingAsyncClient;
  }

  @Override
  public LookupRuntimeProvider getLookupRuntimeProvider(final LookupContext context) {
    return AsyncLookupFunctionProvider.of(
        new LangChainAsyncTableFunction(
            llmConfig, asyncClient, embeddingConfig, embeddingAsyncClient));
  }

  @Override
  public DynamicTableSource copy() {
    return new LangChainAsyncTableSource(
        physicalRowDataType,
        dynamicTableFactoryContext,
        llmConfig,
        asyncClient,
        embeddingConfig,
        embeddingAsyncClient);
  }

  @Override
  public String asSummaryString() {
    return "LangChainAsyncTableSource";
  }
}
