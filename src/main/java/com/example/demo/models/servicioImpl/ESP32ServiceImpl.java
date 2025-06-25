package com.example.demo.models.servicioImpl;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ConnectException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.net.URL;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.example.demo.config.ESP32Properties;
import com.example.demo.models.servicio.ESP32Service;

@Service
public class ESP32ServiceImpl implements ESP32Service{

    private static final Logger logger = LoggerFactory.getLogger(ESP32ServiceImpl.class);
    
    @Value("${esp32.ip:192.168.113.223}")
    private String esp32Ip;
    
    @Value("${esp32.port:80}")
    private int esp32Port;
    
    @Value("${esp32.timeout:5000}")
    private int esp32Timeout;

    @Override
    public boolean enviarComando(String comando) {
        logger.info("Enviando comando '{}' al ESP32 en {}:{}", comando, esp32Ip, esp32Port);
        
        try {
            return enviarComandoHTTP(comando);
        } catch (Exception e) {
            logger.error("Error al enviar comando al ESP32: ", e);
            return false;
        }

    }

    private boolean enviarComandoHTTP(String comando) {
        try {
            // Usar HttpClient moderno en lugar de URL deprecated
            String url = String.format("http://%s:%d/comando?cmd=%s", 
                esp32Ip, esp32Port, URLEncoder.encode(comando, StandardCharsets.UTF_8));
            
            logger.info("Enviando request a: {}", url);
            
            HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(esp32Timeout))
                .build();
            
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofMillis(esp32Timeout))
                .GET()
                .build();
            
            HttpResponse<String> response = client.send(request, 
                HttpResponse.BodyHandlers.ofString());
            
            int statusCode = response.statusCode();
            String responseBody = response.body();
            
            logger.info("Respuesta del ESP32 - Código: {}, Cuerpo: {}", statusCode, responseBody);
            
            return statusCode == 200;
            
        } catch (ConnectException e) {
            logger.error("No se puede conectar al ESP32 en {}:{} - Verifique que esté encendido y accesible", esp32Ip, esp32Port);
            return false;
        } catch (Exception e) {
            logger.error("Error en comunicación HTTP con ESP32: ", e);
            return false;
        }
    }

    @Override
    public String obtenerEstadoConexion() {
        try {
            logger.info("Verificando conexión con ESP32 en {}", esp32Ip);
            InetAddress inet = InetAddress.getByName(esp32Ip);
            
            if (inet.isReachable(esp32Timeout)) {
                return String.format("ESP32 conectado en %s:%d", esp32Ip, esp32Port);
            } else {
                return String.format("ESP32 no responde en %s:%d", esp32Ip, esp32Port);
            }
        } catch (Exception e) {
            logger.error("Error al verificar conexión: ", e);
            return "Error de conexión: " + e.getMessage();
        }
    }

    @Override
    public boolean conectarESP32() {
        logger.info("Intentando conectar con ESP32 en {}:{}...", esp32Ip, esp32Port);
        String estado = obtenerEstadoConexion();
        logger.info("Estado de conexión: {}", estado);
        return estado.contains("conectado");
    }

    @Override
    public void desconectarESP32() {
        logger.info("Desconexión del ESP32 completada");
    }

    public String obtenerConfiguracion() {
        return String.format("ESP32 Config - IP: %s, Puerto: %d, Timeout: %d ms", 
                           esp32Ip, esp32Port, esp32Timeout);
    }
}