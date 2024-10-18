package com.evoura.ververica.sql;

import com.evoura.ververica.langchain4j.infra.langchain.model.AiModel;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.flink.types.Row;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class VervericaLanguageIT extends VervericaBaseIT {

  @Test
  void testVervericaLangchain4jSql() {
    final var inputMessages = List.of("Message 1", "Message 2", "Message 3");

    createUserMessagesTable(inputMessages);
    createLangChainTable(AiModel.DEFAULT);

    final var resultedRows = getResult();
    assertResult(resultedRows, inputMessages);
  }

  private void assertResult(final List<Row> resultedRows, final List<String> userMessages) {
    final var responses =
        resultedRows.stream().map(row -> row.getField(1)).collect(Collectors.toList());

    userMessages.forEach(
        userMessage -> {
          Assertions.assertThat(
                  responses.contains("You shared this message with the AI model: " + userMessage))
              .isTrue();
        });
  }

  private List<Row> getResult() {
    String selectStatement =
        "SELECT ai.* "
            + "FROM "
            + "  user_messages AS chat "
            + "JOIN "
            + "  langchain4j_source FOR SYSTEM_TIME AS OF chat.`timestamp` AS ai "
            + "ON"
            + "  chat.user_message = ai.prompt;";

    List<Row> result = new ArrayList<>();
    tEnv.executeSql(selectStatement).collect().forEachRemaining(result::add);

    return result;
  }

  private void createLangChainTable(AiModel model) {
    String langChainTableSql =
        "CREATE TABLE langchain4j_source ("
            + "    prompt STRING,"
            + "    response STRING"
            + ") WITH ("
            + "    'connector'= 'langchain',"
            + "    'langchain.model'= '"
            + model
            + "',"
            + "    'langchain.embedding.model'= '"
            + model
            + "',"
            + "    'langchain.embedding.store'= 'DEFAULT'"
            + ");";

    tEnv.executeSql(langChainTableSql);
  }

  private void createUserMessagesTable(List<String> userMessages) {
    StringBuilder sqlBuilder = new StringBuilder();
    sqlBuilder
        .append("CREATE TEMPORARY VIEW user_messages AS ")
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

    sqlBuilder.append(") AS predefined_messages(user_message, `timestamp`);");

    tEnv.executeSql(sqlBuilder.toString());
  }
}
