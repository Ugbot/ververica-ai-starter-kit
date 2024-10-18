package com.evoura.ververica.langchain4j.infra.langchain.model.language;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.model.ollama.OllamaChatModel;
import java.util.Map;

public class OllamaLanguageModel implements LangChainLanguageModel {

  @Override
  public ChatLanguageModel getModel(final Map<String, String> properties) {
    return OllamaChatModel.builder()
        .baseUrl(properties.get("baseUrl"))
        .modelName(properties.get("modelName"))
        .build();
  }

  @Override
  public AiModel getName() {
    return AiModel.OLLAMA;
  }
}
