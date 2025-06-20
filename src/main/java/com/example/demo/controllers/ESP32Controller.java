package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.models.servicio.ESP32Service;

@RestController
@RequestMapping("/api/esp32")
@CrossOrigin(origins = "*")
public class ESP32Controller {
    @Autowired
    private ESP32Service esp32Service;

    @PostMapping("/abrir-puerta")
    public ResponseEntity<String> abrirPuerta() {
        try {
            boolean resultado = esp32Service.enviarComando("ABRIR_PUERTA");
            if (resultado) {
                return ResponseEntity.ok("🚪 Comando ABRIR_PUERTA enviado exitosamente");
            } else {
                return ResponseEntity.badRequest().body("❌ Error al enviar comando ABRIR_PUERTA");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @PostMapping("/cerrar-puerta")
    public ResponseEntity<String> cerrarPuerta() {
        try {
            boolean resultado = esp32Service.enviarComando("CERRAR_PUERTA");
            if (resultado) {
                return ResponseEntity.ok("🔒 Comando CERRAR_PUERTA enviado exitosamente");
            } else {
                return ResponseEntity.badRequest().body("❌ Error al enviar comando CERRAR_PUERTA");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @PostMapping("/comando/{comando}")
    public ResponseEntity<String> enviarComandoPersonalizado(@PathVariable String comando) {
        try {
            boolean resultado = esp32Service.enviarComando(comando);
            if (resultado) {
                return ResponseEntity.ok("✅ Comando '" + comando + "' enviado exitosamente");
            } else {
                return ResponseEntity.badRequest().body("❌ Error al enviar comando: " + comando);
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/estado")
    public ResponseEntity<String> obtenerEstado() {
        try {
            String estado = esp32Service.obtenerEstadoConexion();
            return ResponseEntity.ok("📡 Estado: " + estado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener estado: " + e.getMessage());
        }
    }

    @PostMapping("/test-conexion")
    public ResponseEntity<String> testConexion() {
        try {
            boolean resultado = esp32Service.enviarComando("TEST_CONNECTION");
            if (resultado) {
                return ResponseEntity.ok("🧪 Test de conexión enviado - Verifica el parpadeo del LED en el receptor");
            } else {
                return ResponseEntity.badRequest().body("❌ Error en test de conexión");
            }
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error en test: " + e.getMessage());
        }
    }

    @GetMapping("/configuracion")
    public ResponseEntity<String> obtenerConfiguracion() {
        try {
            // Asumiendo que tienes un método para obtener la configuración
            String config = "📊 Configuración actual del sistema ESP32";
            String estado = esp32Service.obtenerEstadoConexion();
            
            String respuesta = config + "\n" + estado;
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener configuración: " + e.getMessage());
        }
    }
}
