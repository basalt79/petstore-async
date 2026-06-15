#!/usr/bin/env bash
set -e

JAR="target/petstore-async-1.0-SNAPSHOT-jar-with-dependencies.jar"

if [[ ! -f "$JAR" ]]; then
  echo "JAR not found: $JAR — run 'mvn clean package -DskipTests' first" >&2
  exit 1
fi

: "${MONGO_URI:?MONGO_URI is required}"
: "${API_KEY:?API_KEY is required}"

exec java -jar "$JAR"
