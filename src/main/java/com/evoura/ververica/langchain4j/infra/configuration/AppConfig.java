package com.evoura.ververica.langchain4j.infra.configuration;

import java.util.Map;
import java.util.Properties;
import org.apache.flink.api.java.utils.ParameterTool;

public class AppConfig {

  private final ParameterTool params;

  private AppConfig(final ParameterTool params) {
    this.params = params;
  }

  public static AppConfig fromArgs(String[] args) {
    return new AppConfig(ParameterTool.fromArgs(args));
  }

  public static AppConfig fromMap(Map<String, String> properties) {
    return new AppConfig(ParameterTool.fromMap(properties));
  }

  public String getKafkaBrokerUrl() {
    return params.get("kafka-broker-url", "localhost:9092");
  }

  public Properties getKafkaProperties() {
    Properties properties = new Properties();

    setIfNotNull(properties, "security.protocol", params.get("kafka-security-protocol"));
    setIfNotNull(properties, "sasl.mechanism", params.get("kafka-sasl-mechanism"));
    setIfNotNull(properties, "sasl.jaas.config", params.get("kafka-sasl-jaas-config"));

    // SSL properties
    setIfNotNull(
        properties, "ssl.truststore.location", params.get("kafka-ssl-truststore-location"));
    setIfNotNull(
        properties, "ssl.truststore.password", params.get("kafka-ssl-truststore-password"));
    setIfNotNull(properties, "ssl.keystore.location", params.get("kafka-ssl-keystore-location"));
    setIfNotNull(properties, "ssl.keystore.password", params.get("kafka-ssl-keystore-password"));
    setIfNotNull(properties, "ssl.key.password", params.get("kafka-ssl-key-password"));

    return properties;
  }

  private void setIfNotNull(Properties properties, String key, String value) {
    if (value != null) {
      properties.setProperty(key, value);
    }
  }
}
