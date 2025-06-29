#!/bin/bash

echo "🔧 Compilando proyecto Spring Boot..."
./mvnw clean package -DskipTests

echo "🐳 Levantando contenedores con Docker Compose..."
docker-compose up --build
