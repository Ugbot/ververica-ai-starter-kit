package com.evoura.ververica.langchain4j.infra.serde;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.flink.api.common.typeinfo.TypeInfo;
import org.apache.flink.api.common.typeinfo.TypeInfoFactory;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TypeInfo(ChatMemory.TypeInfo.class)
public class ChatMemory {
  private Long userId;
  private Long chatId;
  private List<ChatMemoryData> chatMemoryData;
  private LLMConfig llmConfig;

  public static class TypeInfo extends TypeInfoFactory<ChatMemory> {
    @Override
    public TypeInformation<ChatMemory> createTypeInfo(
        Type type, Map<String, TypeInformation<?>> map) {
      return Types.POJO(
          ChatMemory.class,
          Map.of(
              "userId",
              Types.LONG,
              "chatId",
              Types.LONG,
              "chatMemoryData",
              Types.LIST(Types.POJO(ChatMemoryData.class)),
              "llmConfig",
              Types.POJO(LLMConfig.class)));
    }
  }
}
