# Usa Java 17 como base
FROM openjdk:17-jdk-slim

# Define el directorio de trabajo dentro del contenedor
WORKDIR /app

# Copia el archivo .jar desde tu proyecto al contenedor
COPY target/demo-0.0.1-SNAPSHOT.jar app.jar

# Expone el puerto correcto (de tu application.properties)
EXPOSE 7777

# Comando para ejecutar tu aplicación
ENTRYPOINT ["java", "-jar", "app.jar"]
