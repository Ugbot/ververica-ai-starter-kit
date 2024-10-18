package com.evoura.ververica.langchain4j.sql.connector.language;

import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.AI_MODEL;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_MODEL;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_MODEL_BASE_URL;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_MODEL_NAME;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_API_KEY;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_COLLECTION_NAME;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_HOST;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_MAX_RESULTS;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_MIN_SCORE;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.EMBED_STORE_PORT;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.MODEL_API_KEY;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.MODEL_BASE_URL;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.MODEL_NAME;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.PROMPT_TEMPLATE;
import static com.evoura.ververica.langchain4j.sql.connector.language.LangChainConnectorOptions.SYSTEM_MESSAGE;
import static org.apache.flink.table.api.DataTypes.FIELD;
import static org.apache.flink.table.types.utils.DataTypeUtils.removeTimeAttribute;

import com.evoura.ververica.langchain4j.client.LangChainAsyncClient;
import com.evoura.ververica.langchain4j.client.LangChainEmbeddingAsyncClient;
import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.embedding.LangChainEmbeddingModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.embedding.OllamaEmbeddingModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.LangChainLanguageModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.OllamaLanguageModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.language.OpenAiLanguageModel;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingConfig;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingStore;
import com.evoura.ververica.langchain4j.infra.langchain.store.LangChainEmbeddingStore;
import com.evoura.ververica.langchain4j.infra.langchain.store.QdrantEmbeddingStore;
import com.evoura.ververica.langchain4j.infra.serde.LLMConfig;
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

public class LangChainTableFactory implements DynamicTableSourceFactory {

  private static final String IDENTIFIER = "langchain";

  @Override
  public DynamicTableSource createDynamicTableSource(final Context context) {
    FactoryUtil.TableFactoryHelper helper = FactoryUtil.createTableFactoryHelper(this, context);

    ResolvedSchema resolvedSchema = context.getCatalogTable().getResolvedSchema();

    LLMConfig llmConfig = getLlmConfig(helper.getOptions());
    EmbeddingConfig embeddingConfig = getEmbeddingProperties(helper.getOptions());

    DataType physicalRowDataType = toRowDataType(resolvedSchema.getColumns(), Column::isPhysical);

    LangChainAsyncClient asyncClient =
        new LangChainAsyncClient(
            List.of(
                LangChainLanguageModel.DEFAULT_MODEL,
                new OllamaLanguageModel(),
                new OpenAiLanguageModel()));

    LangChainEmbeddingAsyncClient embeddingAsyncClient =
        new LangChainEmbeddingAsyncClient(
            List.of(LangChainEmbeddingModel.DEFAULT_MODEL, new OllamaEmbeddingModel()),
            List.of(
                LangChainEmbeddingStore.DEFAULT_MODEL,
                new QdrantEmbeddingStore(),
                new QdrantEmbeddingStore()));

    return new LangChainAsyncTableSource(
        physicalRowDataType,
        context,
        llmConfig,
        asyncClient,
        embeddingConfig,
        embeddingAsyncClient);
  }

  @Override
  public String factoryIdentifier() {
    return IDENTIFIER;
  }

  @Override
  public Set<ConfigOption<?>> requiredOptions() {
    final HashSet<ConfigOption<?>> options = new HashSet<>();
    options.add(AI_MODEL);
    return options;
  }

  @Override
  public Set<ConfigOption<?>> optionalOptions() {
    final HashSet<ConfigOption<?>> options = new HashSet<>();
    options.add(MODEL_BASE_URL);
    options.add(MODEL_API_KEY);
    options.add(MODEL_NAME);
    options.add(SYSTEM_MESSAGE);
    options.add(PROMPT_TEMPLATE);
    options.add(EMBED_MODEL);
    options.add(EMBED_MODEL_BASE_URL);
    options.add(EMBED_MODEL_NAME);
    options.add(EMBED_STORE);
    options.add(EMBED_STORE_HOST);
    options.add(EMBED_STORE_PORT);
    options.add(EMBED_STORE_API_KEY);
    options.add(EMBED_STORE_COLLECTION_NAME);
    options.add(EMBED_STORE_MIN_SCORE);
    options.add(EMBED_STORE_MAX_RESULTS);
    return options;
  }

  private DataType toRowDataType(List<Column> columns, Predicate<Column> columnPredicate) {
    return columns.stream()
        .filter(columnPredicate)
        .map(LangChainTableFactory::columnToField)
        .collect(Collectors.collectingAndThen(Collectors.toList(), LangChainTableFactory::row))
        .notNull();
  }

  private static DataTypes.Field columnToField(Column column) {
    return FIELD(column.getName(), removeTimeAttribute(column.getDataType()));
  }

  private static DataType row(List<DataTypes.Field> fields) {
    return DataTypes.ROW(fields.toArray(new DataTypes.Field[0]));
  }

  private static LLMConfig getLlmConfig(ReadableConfig tableOptions) {
    LLMConfig config = new LLMConfig();
    config.setAiModel(tableOptions.get(AI_MODEL));
    config.setSystemMessage(tableOptions.get(SYSTEM_MESSAGE));

    Map<String, String> properties = new HashMap<>();
    properties.put("baseUrl", tableOptions.get(MODEL_BASE_URL));
    properties.put("apiKey", tableOptions.get(MODEL_API_KEY));
    properties.put("modelName", tableOptions.get(MODEL_NAME));
    properties.put("promptTemplate", tableOptions.get(PROMPT_TEMPLATE));
    config.setProperties(properties);

    return config;
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
    Map<String, String> embedStoreProperties = new HashMap<>();
    if (config.getEmbedStore().equals(EmbeddingStore.QDRANT)) {
      embedStoreProperties.put("host", tableOptions.get(EMBED_STORE_HOST));
      embedStoreProperties.put("port", tableOptions.get(EMBED_STORE_PORT));
      embedStoreProperties.put("apiKey", tableOptions.get(EMBED_STORE_API_KEY));
      embedStoreProperties.put("collectionName", tableOptions.get(EMBED_STORE_COLLECTION_NAME));
    }
    embedStoreProperties.put("minScore", tableOptions.get(EMBED_STORE_MIN_SCORE).toString());
    embedStoreProperties.put("maxResults", tableOptions.get(EMBED_STORE_MAX_RESULTS).toString());

    config.setStoreProperties(embedStoreProperties);

    return config;
  }
}
