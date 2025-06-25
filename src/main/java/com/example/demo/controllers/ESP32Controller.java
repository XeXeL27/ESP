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
import com.example.demo.models.servicio.AutenticacionService;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@RestController
@RequestMapping("/api/esp32")
@CrossOrigin(origins = "*")
public class ESP32Controller {
    
    @Autowired
    private ESP32Service esp32Service;
    
    @Autowired
    private AutenticacionService autenticacionService;

    /**
     * Verificar autenticación antes de ejecutar comandos
     */
    private boolean isAuthenticated(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return false;
        }
        
        return autenticacionService.validarToken(userToken).isPresent();
    }

    @PostMapping("/abrir-puerta")
    public ResponseEntity<String> abrirPuerta(HttpSession session) {
        // 🔐 VERIFICAR AUTENTICACIÓN
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida - Inicia sesión");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🚪 Usuario " + userName + " solicita abrir puerta");
        
        try {
            boolean resultado = esp32Service.enviarComando("ABRIR_PUERTA");
            if (resultado) {
                System.out.println("✅ Puerta abierta exitosamente por: " + userName);
                return ResponseEntity.ok("🚪 Comando ABRIR_PUERTA enviado exitosamente");
            } else {
                System.out.println("❌ Error al abrir puerta para: " + userName);
                return ResponseEntity.badRequest().body("❌ Error al enviar comando ABRIR_PUERTA");
            }
        } catch (Exception e) {
            System.out.println("💥 Error crítico al abrir puerta: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @PostMapping("/cerrar-puerta")
    public ResponseEntity<String> cerrarPuerta(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🔒 Usuario " + userName + " solicita cerrar puerta");
        
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
    public ResponseEntity<String> enviarComandoPersonalizado(@PathVariable String comando, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("⚡ Usuario " + userName + " envía comando personalizado: " + comando);
        
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
    public ResponseEntity<String> obtenerEstado(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        try {
            String estado = esp32Service.obtenerEstadoConexion();
            return ResponseEntity.ok("📡 Estado: " + estado);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener estado: " + e.getMessage());
        }
    }

    @PostMapping("/test-conexion")
    public ResponseEntity<String> testConexion(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🧪 Usuario " + userName + " ejecuta test de conexión");
        
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
    public ResponseEntity<String> obtenerConfiguracion(HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        try {
            String config = "📊 Configuración actual del sistema ESP32";
            String estado = esp32Service.obtenerEstadoConexion();
            
            String respuesta = config + "\n" + estado;
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener configuración: " + e.getMessage());
        }
    }

    /**
     * Endpoint especial para verificación rápida (sin autenticación para debugging)
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping() {
        return ResponseEntity.ok("🏃‍♂️ ESP32 Controller está funcionando - " + 
                                java.time.LocalDateTime.now().toString());
    }
}