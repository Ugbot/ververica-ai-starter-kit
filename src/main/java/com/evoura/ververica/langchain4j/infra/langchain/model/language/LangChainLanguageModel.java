package com.evoura.ververica.langchain4j.infra.langchain.model.language;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import dev.langchain4j.model.chat.ChatLanguageModel;
import java.io.Serializable;
import java.util.Map;

public interface LangChainLanguageModel extends Serializable {

  LangChainLanguageModel DEFAULT_MODEL = new DefaultLanguageModel();

  ChatLanguageModel getModel(Map<String, String> properties);

  AiModel getName();
}
