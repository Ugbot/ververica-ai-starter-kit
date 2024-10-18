package com.evoura.ververica.sql;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import com.evoura.ververica.langchain4j.infra.langchain.model.embedding.DefaultEmbeddingModel;
import com.evoura.ververica.langchain4j.infra.langchain.store.EmbeddingStore;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.flink.types.Row;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class VervericaEmbeddingIT extends VervericaBaseIT {

  @Test
  void testVervericaLangchain4jSql() {
    final var inputData = List.of("Message 1", "Message 2", "Message 3");

    createInputDataTable(inputData);
    createLangChainEmbeddingTable(AiModel.DEFAULT, EmbeddingStore.DEFAULT);

    final var resultedRows = getResult();
    assertResult(resultedRows, inputData);
  }

  private void assertResult(final List<Row> resultedRows, final List<String> inputMessages) {
    final var responses =
        resultedRows.stream().map(row -> row.getField(1)).collect(Collectors.toList());

    inputMessages.forEach(
        inputMessage -> {
          Assertions.assertThat(
                  responses.contains(
                      DefaultEmbeddingModel.generateEmbedding(inputMessage).toString()))
              .isTrue();
        });
  }

  private List<Row> getResult() {
    String selectStatement =
        "SELECT embedding.* "
            + "FROM "
            + "  input_table AS i "
            + "JOIN "
            + "  langchain_embedding_sink FOR SYSTEM_TIME AS OF i.`timestamp` AS embedding "
            + "ON"
            + "  i.input_data = embedding.input_data;";

    List<Row> result = new ArrayList<>();
    tEnv.executeSql(selectStatement).collect().forEachRemaining(result::add);

    return result;
  }

  private void createLangChainEmbeddingTable(AiModel model, final EmbeddingStore store) {
    String langChainTableSql =
        "CREATE TABLE langchain_embedding_sink ("
            + "    input_data STRING,"
            + "    response STRING"
            + ") WITH ("
            + "    'connector'= 'langchain-embedding',"
            + "    'langchain-embedding.model'= '"
            + model
            + "',"
            + "    'langchain-embedding.store'= '"
            + store
            + "'"
            + ");";

    tEnv.executeSql(langChainTableSql);
  }

  private void createInputDataTable(List<String> userMessages) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder
        .append("CREATE TEMPORARY VIEW input_table AS ")
        .append("SELECT * FROM (")
        .append("    VALUES ");

    // Append each message to the VALUES clause
    IntStream.range(0, userMessages.size())
        .forEach(
            i -> {
              sqlBuilder
                  .append("('")
                  .append(userMessages.get(i).replace("'", "''"))
                  .append("', PROCTIME())");
              if (i < userMessages.size() - 1) {
                sqlBuilder.append(",");
              }
            });

    sqlBuilder.append(") AS input_table(input_data, `timestamp`);");

    tEnv.executeSql(sqlBuilder.toString());
  }
}
