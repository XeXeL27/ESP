package com.example.demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "esp32")
public class ESP32Properties {
    private String ip = "192.168.1.29";  // ISKRA_Ext: 192.168.1.21
    private int port = 80;              // Valor por defecto
    private int timeout = 5000;         // Valor por defecto

    // Getters y Setters
    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    @Override
    public String toString() {
        return String.format("ESP32Properties{ip='%s', port=%d, timeout=%d}", 
                            ip, port, timeout);
    }

}
