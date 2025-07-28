#!/bin/bash
set -euo pipefail

echo "🐋 Shutting down previous docker..."
docker compose --profile flink down --remove-orphans --volumes

echo "👷‍♂️ Building project..."
./mvnw clean install jib:dockerBuild > /dev/null
echo "✅ Project built."
echo "🐳 Image 'ververica-langchain4j:0.0.1-SNAPSHOT' build."

echo "🚀 Starting Docker Compose in detached mode..."
docker compose --profile flink up -d
echo "✅ Docker Compose command executed."


echo "---------------------------------------------"
echo "📢 Service Status:"
echo "👁️ AKHQ (Kafka visualization tool) is running on http://localhost:8085"
echo "🛠️ Kafka is running on port 9092 -> localhost"
echo "🐿️ Flink UI is running on http://localhost:8082"
echo "---------------------------------------------"

java -cp ./target/ververica-langchain4j-0.0.1-SNAPSHOT.jar com.evoura.ververica.langchain4j.stream.ConsoleVervericaApplication
