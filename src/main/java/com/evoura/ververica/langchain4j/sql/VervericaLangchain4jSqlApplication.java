package com.evoura.ververica.langchain4j.sql;

import java.util.concurrent.ExecutionException;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.TableEnvironment;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.types.Row;
import org.apache.flink.util.CloseableIterator;

public class VervericaLangchain4jSqlApplication {

  public static void main(String[] args) throws ExecutionException, InterruptedException {
    StreamExecutionEnvironment streamEnv = StreamExecutionEnvironment.getExecutionEnvironment();

    EnvironmentSettings settings = EnvironmentSettings.newInstance().inBatchMode().build();

    TableEnvironment tableEnv = StreamTableEnvironment.create(streamEnv, settings);

    // STORE EMBEDDINGS

    tableEnv.executeSql(
        "CREATE TEMPORARY VIEW input_table AS "
            + "SELECT * FROM ("
            + "    VALUES "
            + "        ('Ewok language is called Ewokese.', PROCTIME()),"
            + "        ('Ewokese was created by Ben Burtt.', PROCTIME()),"
            + "        ('Ewok language is Tibetan mixed with Kalmyk languages.', PROCTIME())"
            + ") AS input_table(input_data, `timestamp`);");

    tableEnv.executeSql(
        "CREATE TABLE langchain_embedding ("
            + "    input_data STRING,"
            + "    response STRING"
            + ") WITH ("
            + "    'connector'= 'langchain-embedding',"
            + "    'langchain-embedding.model'= 'OLLAMA',"
            + "    'langchain-embedding.model.base.url'= 'http://localhost:11434',"
            + "    'langchain-embedding.model.name'= 'nomic-embed-text:latest',"
            + "    'langchain-embedding.store'= 'DEFAULT'"
            + ");");

    tableEnv
        .executeSql(
            "SELECT e.* "
                + "FROM "
                + "  input_table AS i "
                + "JOIN "
                + "  langchain_embedding FOR SYSTEM_TIME AS OF i.`timestamp` AS e "
                + "ON"
                + "  i.input_data = e.input_data;")
        .await();

    // ASK QUESTIONS

    tableEnv.executeSql(
        "CREATE TEMPORARY VIEW user_messages AS "
            + "SELECT * FROM ("
            + "    VALUES "
            + "        ('How is the Ewok language called?', PROCTIME()),"
            + "        ('Who created the Ewok language?', PROCTIME()),"
            + "        ('What are the languages behind Ewokese?', PROCTIME())"
            + ") AS predefined_messages(user_message, `timestamp`);");

    tableEnv.executeSql(
        "CREATE TABLE langchain_llm ("
            + "    prompt STRING,"
            + "    response STRING"
            + ") WITH ("
            + "    'connector'= 'langchain',"
            + "    'langchain.model'= 'OLLAMA',"
            + "    'langchain.model.base.url'= 'http://localhost:11434',"
            + "    'langchain.model.name'= 'llama3.1:latest',"
            + "    'langchain.embedding.model'= 'OLLAMA',"
            + "    'langchain.embedding.model.base.url'= 'http://localhost:11434',"
            + "    'langchain.embedding.model.name'= 'nomic-embed-text:latest',"
            + "    'langchain.embedding.store'= 'DEFAULT'"
            + ");");

    final var result =
        tableEnv
            .executeSql(
                "SELECT llm.* "
                    + "FROM "
                    + "  user_messages AS chat "
                    + "JOIN "
                    + "  langchain_llm FOR SYSTEM_TIME AS OF chat.`timestamp` AS llm "
                    + "ON"
                    + "  chat.user_message = llm.prompt;")
            .collect();

    printResult(result);
  }

  private static void printResult(final CloseableIterator<Row> result) {
    result.forEachRemaining(
        row -> {
          System.out.println("User: " + row.getField(0));
          System.out.println("AI: " + row.getField(1));
        });
  }
}
