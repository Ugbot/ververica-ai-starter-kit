package com.evoura.ververica.langchain4j.sql.connector.embedding;

import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import com.evoura.ververica.langchain4j.sql.udf.LangChainEmbeddingAsyncTableFunction;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.connector.source.LookupTableSource;
import org.apache.flink.table.connector.source.lookup.AsyncLookupFunctionProvider;
import org.apache.flink.table.factories.DynamicTableFactory;
import org.apache.flink.table.types.DataType;

public class LangChainEmbeddingAsyncTableSource implements LookupTableSource {

  private final DataType physicalRowDataType;
  private final DynamicTableFactory.Context dynamicTableFactoryContext;
  private final EmbeddingConfig embeddingConfig;
  private final LangChainEmbeddingAsyncClient asyncClient;

  public LangChainEmbeddingAsyncTableSource(
      final DataType physicalRowDataType,
      final DynamicTableFactory.Context context,
      final EmbeddingConfig embeddingConfig,
      final LangChainEmbeddingAsyncClient asyncClient) {
    this.physicalRowDataType = physicalRowDataType;
    this.dynamicTableFactoryContext = context;
    this.embeddingConfig = embeddingConfig;
    this.asyncClient = asyncClient;
  }

  @Override
  public LookupRuntimeProvider getLookupRuntimeProvider(final LookupContext context) {
    return AsyncLookupFunctionProvider.of(
        new LangChainEmbeddingAsyncTableFunction(embeddingConfig, asyncClient));
  }

  @Override
  public DynamicTableSource copy() {
    return new LangChainEmbeddingAsyncTableSource(
        physicalRowDataType, dynamicTableFactoryContext, embeddingConfig, asyncClient);
  }

  @Override
  public String asSummaryString() {
    return "LangChainEmbeddingAsyncTableSource";
  }
}
