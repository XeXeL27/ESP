package com.example.demo.models.servicio;

public interface ESP32Service {
    boolean enviarComando(String comando);
    String obtenerEstadoConexion();
    boolean conectarESP32();
    void desconectarESP32();
}
