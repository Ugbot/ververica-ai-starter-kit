package com.evoura.ververica.sql;

import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.junit.jupiter.api.BeforeEach;

public abstract class VervericaBaseIT {

  protected StreamTableEnvironment tEnv;

  @BeforeEach
  public void setUp() {
    StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
    EnvironmentSettings settings = EnvironmentSettings.newInstance().build();

    tEnv = StreamTableEnvironment.create(env, settings);
  }
}
