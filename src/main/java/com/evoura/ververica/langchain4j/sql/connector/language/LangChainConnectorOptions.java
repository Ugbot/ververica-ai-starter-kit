package com.evoura.ververica.langchain4j.sql.connector.language;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingStore;
import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;

public class LangChainConnectorOptions {

  public static final String CONNECTOR_PREFIX = "langchain";
  private static final String EMBEDDING_STORE_PREFIX = CONNECTOR_PREFIX + ".embedding.store";
  private static final String EMBEDDING_MODEL_PREFIX = CONNECTOR_PREFIX + ".embedding.model";

  public static final ConfigOption<AiModel> AI_MODEL =
      ConfigOptions.key(CONNECTOR_PREFIX + ".model").enumType(AiModel.class).noDefaultValue();

  public static final ConfigOption<String> MODEL_BASE_URL =
      ConfigOptions.key(CONNECTOR_PREFIX + ".model.base.url").stringType().noDefaultValue();

  public static final ConfigOption<String> MODEL_API_KEY =
      ConfigOptions.key(CONNECTOR_PREFIX + ".model.api.key").stringType().noDefaultValue();

  public static final ConfigOption<String> MODEL_NAME =
      ConfigOptions.key(CONNECTOR_PREFIX + ".model.name").stringType().noDefaultValue();

  public static final ConfigOption<String> SYSTEM_MESSAGE =
      ConfigOptions.key(CONNECTOR_PREFIX + ".system.message").stringType().noDefaultValue();

  public static final ConfigOption<String> PROMPT_TEMPLATE =
      ConfigOptions.key(CONNECTOR_PREFIX + ".prompt.template")
          .stringType()
          .defaultValue(
              "Answer the following message:\n"
                  + "\n"
                  + "Message:\n"
                  + "{{message}}\n"
                  + "\n"
                  + "Base your answer only on the following information:\n"
                  + "{{information}}\n"
                  + "Do not include any extra information.\n");

  // EMBEDDING MODEL PROPERTIES
  public static final ConfigOption<AiModel> EMBED_MODEL =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX)
          .enumType(AiModel.class)
          .defaultValue(AiModel.DEFAULT);

  public static final ConfigOption<String> EMBED_MODEL_BASE_URL =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX + ".base.url").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_MODEL_NAME =
      ConfigOptions.key(EMBEDDING_MODEL_PREFIX + ".name").stringType().noDefaultValue();

  // EMBEDDING STORE PROPERTIES
  public static final ConfigOption<EmbeddingStore> EMBED_STORE =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX)
          .enumType(EmbeddingStore.class)
          .defaultValue(EmbeddingStore.DEFAULT);

  public static final ConfigOption<String> EMBED_STORE_HOST =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".host").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_PORT =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".port").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_API_KEY =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".api.key").stringType().noDefaultValue();

  public static final ConfigOption<String> EMBED_STORE_COLLECTION_NAME =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".collection.name").stringType().noDefaultValue();

  public static final ConfigOption<Double> EMBED_STORE_MIN_SCORE =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".min.score").doubleType().defaultValue(0.7);

  public static final ConfigOption<Integer> EMBED_STORE_MAX_RESULTS =
      ConfigOptions.key(EMBEDDING_STORE_PREFIX + ".max.results").intType().defaultValue(1000);
}
