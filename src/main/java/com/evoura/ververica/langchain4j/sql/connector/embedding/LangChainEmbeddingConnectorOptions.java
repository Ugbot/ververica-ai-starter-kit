package com.evoura.ververica.langchain4j.sql.connector.embedding;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingStore;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

public class LangChainEmbeddingConnectorOptions {

  public static final String CONNECTOR_PREFIX = "langchain-embedding";
  private static final String EMBEDDING_MODEL_PREFIX = CONNECTOR_PREFIX + ".model";
  private static final String EMBEDDING_STORE_PREFIX = CONNECTOR_PREFIX + ".store";

  // MODEL PROPERTIES
  public static final ConfigOption<AiModel> EMBED_MODEL =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX).enumType(AiModel.class).noDefaultValue();

  public static final ConfigOption<String> EMBED_MODEL_BASE_URL =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX + ".base.url").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_MODEL_NAME =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX + ".name").stringType().noDefaultValue();

  // STORE PROPERTIES
  public static final ConfigOption<EmbeddingStore> EMBED_STORE =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX).enumType(EmbeddingStore.class).noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_HOST =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".host").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_PORT =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".port").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_API_KEY =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".api.key").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_COLLECTION_NAME =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".collection.name").stringType().noDefaultValue();
}
