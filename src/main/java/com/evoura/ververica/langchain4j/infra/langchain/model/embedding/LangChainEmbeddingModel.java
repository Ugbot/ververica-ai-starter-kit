package com.evoura.ververica.langchain4j.infra.langchain.model.embedding;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import java.io.Serializable;
import java.util.Map;

public interface LangChainEmbeddingModel extends Serializable {

  LangChainEmbeddingModel DEFAULT_MODEL = new DefaultEmbeddingModel();

  EmbeddingModel getModel(Map<String, String> properties);

  AiModel getName();
}
