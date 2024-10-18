package com.evoura.ververica.langchain4j.sql.connector.embedding;

import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_MODEL;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_MODEL_BASE_URL;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_MODEL_NAME;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_STORE;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_STORE_API_KEY;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_STORE_COLLECTION_NAME;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_STORE_HOST;
import static com.evoura.ververica.langchain4j.sql.connector.embedding.LangChainEmbeddingConnectorOptions.EMBED_STORE_PORT;
import static org.apache.flink.table.api.DataTypes.FIELD;
import static org.apache.flink.table.types.utils.DataTypeUtils.removeTimeAttribute;

import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.embedding.LangChainEmbeddingModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.embedding.OllamaEmbeddingModel;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingStore;
import com.evoura.ververica.langchain4j.infra.langchain.store.LangChainEmbeddingStore;
import com.evoura.ververica.langchain4j.infra.langchain.store.QdrantEmbeddingStore;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.connector.source.DynamicTableSource;
import org.apache.flink.table.factories.DynamicTableSourceFactory;
import org.apache.flink.table.factories.FactoryUtil;
import org.apache.flink.table.types.DataType;

public class LangChainEmbeddingTableFactory implements DynamicTableSourceFactory {

  private static final String IDENTIFIER = "langchain-embedding";

  @Override
  public DynamicTableSource createDynamicTableSource(final Context context) {
    FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

    ResolvedSchema resolvedSchema = context.getCatalogTable().getResolvedSchema();

    EmbeddingConfig embeddingConfig = getEmbeddingProperties(helper.getOptions());

    DataType physicalRowDataType = toRowDataType(resolvedSchema.getColumns(), Column::isPhysical);

    LangChainEmbeddingAsyncClient asyncClient =
        new LangChainEmbeddingAsyncClient(
            List.of(LangChainEmbeddingModel.DEFAULT_MODEL, new OllamaEmbeddingModel()),
            List.of(
                LangChainEmbeddingStore.DEFAULT_MODEL,
                new QdrantEmbeddingStore(),
                new QdrantEmbeddingStore()));

    return new LangChainEmbeddingAsyncTableSource(
        physicalRowDataType, context, embeddingConfig, asyncClient);
  }

  @Override
  public String factoryIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Set<ConfigOption<?>> requiredOptions() {
    final HashSet<ConfigOption<?>> options = new HashSet<>();
    options.add(EMBED_MODEL);
    options.add(EMBED_STORE);
    return options;
  }

  @Override
  public Set<ConfigOption<?>> optionalOptions() {
    final HashSet<ConfigOption<?>> options = new HashSet<>();
    options.add(EMBED_MODEL_BASE_URL);
    options.add(EMBED_MODEL_NAME);
    options.add(EMBED_STORE_HOST);
    options.add(EMBED_STORE_PORT);
    options.add(EMBED_STORE_API_KEY);
    options.add(EMBED_STORE_COLLECTION_NAME);
    return options;
  }

  private DataType toRowDataType(List<Column> columns, Predicate<Column> columnPredicate) {
    return columns.stream()
        .filter(columnPredicate)
        .map(LangChainEmbeddingTableFactory::columnToField)
        .collect(
            Collectors.collectingAndThen(Collectors.toList(), LangChainEmbeddingTableFactory::row))
        .notNull();
  }

  private static DataTypes.Field columnToField(Column column) {
    return FIELD(column.getName(), removeTimeAttribute(column.getDataType()));
  }

  private static DataType row(List<DataTypes.Field> fields) {
    return DataTypes.ROW(fields.toArray(new DataTypes.Field[0]));
  }

  private static EmbeddingConfig getEmbeddingProperties(ReadableConfig tableOptions) {
    EmbeddingConfig config = new EmbeddingConfig();

    // Set embed model properties
    config.setEmbedModel(tableOptions.get(EMBED_MODEL));
    if (config.getEmbedModel().equals(AiModel.OLLAMA)) {
      Map<String, String> properties = new HashMap<>();
      properties.put("baseUrl", tableOptions.get(EMBED_MODEL_BASE_URL));
      properties.put("modelName", tableOptions.get(EMBED_MODEL_NAME));
      config.setModelProperties(properties);
    }

    // Set embed store properties
    config.setEmbedStore(tableOptions.get(EMBED_STORE));
    if (config.getEmbedStore().equals(EmbeddingStore.QDRANT)) {
      Map<String, String> properties = new HashMap<>();
      properties.put("host", tableOptions.get(EMBED_STORE_HOST));
      properties.put("port", tableOptions.get(EMBED_STORE_PORT));
      properties.put("apiKey", tableOptions.get(EMBED_STORE_API_KEY));
      properties.put("collectionName", tableOptions.get(EMBED_STORE_COLLECTION_NAME));
      config.setStoreProperties(properties);
    }

    return config;
  }
}
