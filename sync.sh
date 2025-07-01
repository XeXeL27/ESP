#!/bin/bash

echo "🔄 Verificando cambios locales..."
git restore .gitignore mvnw src/main/resources/application.properties 2>/dev/null

echo "📥 Haciendo pull del repositorio..."
git pull origin main

echo "🔧 Compilando y reconstruyendo contenedor..."
./start.sh
