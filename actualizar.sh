#!/bin/bash

echo "🔧 Iniciando actualización del sistema ESP..."

cd "$(dirname "$0")" || exit 1

# Guardar fecha y hora
FECHA=$(date +"%Y-%m-%d %H:%M:%S")
echo "🕒 [$FECHA]"

# Guardar cambios locales si los hay
if [[ -n $(git status --porcelain) ]]; then
  echo "📦 Cambios locales detectados, aplicando git stash..."
  git stash
else
  echo "✅ No hay cambios locales, continuando..."
fi

# Hacer pull del repositorio
echo "🔄 Sincronizando cambios desde GitHub..."
git pull origin main

# Restaurar stash si hubo
if git stash list | grep -q "stash@{0}"; then
  echo "📂 Restaurando cambios locales guardados..."
  git stash pop
fi

# Compilar el .jar antes de construir la imagen
echo "🛠️ Compilando el proyecto con Maven..."
./mvnw clean package -DskipTests

# Detener y reconstruir contenedores
echo "🧱 Reconstruyendo contenedores con Docker..."
docker-compose down
docker-compose build
docker-compose up -d

echo "✅ Sistema actualizado y corriendo."
