#!/bin/bash

echo "🛑 Deteniendo y eliminando contenedores..."
docker-compose down

echo "🧹 (Opcional) Limpieza de imágenes y volúmenes:"
# docker system prune -f       # Elimina contenedores, redes y cachés sin usar
# docker volume rm postgres_data  # Elimina volumen si quieres resetear la base

echo "✅ Sistema detenido."
