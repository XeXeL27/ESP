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
import com.example.demo.models.servicio.LogService;
import com.example.demo.models.dao.LogAccesoDao;
import com.example.demo.models.entity.LogAcceso;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/esp32")
@CrossOrigin(origins = "*")
public class ESP32Controller {
    
    @Autowired
    private ESP32Service esp32Service;
    
    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private LogAccesoDao logAccesoDao;

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

    /**
     * Registrar log de acceso con información completa
     */
    private void registrarLog(String userName, String accion, String resultado, 
                             String detalles, HttpServletRequest request) {
        try {
            String direccionIp = obtenerDireccionIpReal(request);
            String userAgent = request.getHeader("User-Agent");
            
            LogAcceso log = new LogAcceso();
            log.setUserName(userName);
            log.setAccion(accion);
            log.setResultado(resultado);
            log.setDetalles(detalles);
            log.setDireccionIp(direccionIp);
            log.setUserAgent(userAgent);
            log.setFechaHora(LocalDateTime.now());
            
            logAccesoDao.save(log);
            
            System.out.println("📝 Log registrado: " + userName + " - " + accion + " - " + resultado);
        } catch (Exception e) {
            System.err.println("❌ Error registrando log: " + e.getMessage());
        }
    }

    /**
     * Obtener dirección IP real del cliente
     */
    private String obtenerDireccionIpReal(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }

    @PostMapping("/abrir-puerta")
    public ResponseEntity<String> abrirPuerta(HttpSession session, HttpServletRequest request) {
        // 🔐 VERIFICAR AUTENTICACIÓN
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "ABRIR_PUERTA", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida - Inicia sesión");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🚪 Usuario " + userName + " solicita abrir puerta");
        
        try {
            boolean resultado = esp32Service.enviarComando("ABRIR_PUERTA");
            
            if (resultado) {
                System.out.println("✅ Puerta abierta exitosamente por: " + userName);
                
                // Registrar éxito en logs
                registrarLog(userName, "ABRIR_PUERTA", "EXITOSO", 
                           "Comando enviado exitosamente al ESP32", request);
                
                return ResponseEntity.ok("🚪 Comando ABRIR_PUERTA enviado exitosamente");
            } else {
                System.out.println("❌ Error al abrir puerta para: " + userName);
                
                // Registrar fallo en logs
                registrarLog(userName, "ABRIR_PUERTA", "FALLIDO", 
                           "ESP32 no respondió correctamente", request);
                
                return ResponseEntity.badRequest().body("❌ Error al enviar comando ABRIR_PUERTA");
            }
        } catch (Exception e) {
            System.out.println("💥 Error crítico al abrir puerta: " + e.getMessage());
            
            // Registrar error crítico en logs
            registrarLog(userName, "ABRIR_PUERTA", "ERROR", 
                       "Error crítico: " + e.getMessage(), request);
            
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @PostMapping("/cerrar-puerta")
    public ResponseEntity<String> cerrarPuerta(HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "CERRAR_PUERTA", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🔒 Usuario " + userName + " solicita cerrar puerta");
        
        try {
            boolean resultado = esp32Service.enviarComando("CERRAR_PUERTA");
            
            if (resultado) {
                registrarLog(userName, "CERRAR_PUERTA", "EXITOSO", 
                           "Comando enviado exitosamente al ESP32", request);
                return ResponseEntity.ok("🔒 Comando CERRAR_PUERTA enviado exitosamente");
            } else {
                registrarLog(userName, "CERRAR_PUERTA", "FALLIDO", 
                           "ESP32 no respondió correctamente", request);
                return ResponseEntity.badRequest().body("❌ Error al enviar comando CERRAR_PUERTA");
            }
        } catch (Exception e) {
            registrarLog(userName, "CERRAR_PUERTA", "ERROR", 
                       "Error crítico: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @PostMapping("/comando/{comando}")
    public ResponseEntity<String> enviarComandoPersonalizado(@PathVariable String comando, 
                                                           HttpSession session, 
                                                           HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "COMANDO_PERSONALIZADO", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("⚡ Usuario " + userName + " envía comando personalizado: " + comando);
        
        try {
            boolean resultado = esp32Service.enviarComando(comando);
            
            if (resultado) {
                registrarLog(userName, "COMANDO_PERSONALIZADO", "EXITOSO", 
                           "Comando: " + comando + " enviado exitosamente", request);
                return ResponseEntity.ok("✅ Comando '" + comando + "' enviado exitosamente");
            } else {
                registrarLog(userName, "COMANDO_PERSONALIZADO", "FALLIDO", 
                           "Comando: " + comando + " - ESP32 no respondió", request);
                return ResponseEntity.badRequest().body("❌ Error al enviar comando: " + comando);
            }
        } catch (Exception e) {
            registrarLog(userName, "COMANDO_PERSONALIZADO", "ERROR", 
                       "Comando: " + comando + " - Error: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error interno: " + e.getMessage());
        }
    }

    @GetMapping("/estado")
    public ResponseEntity<String> obtenerEstado(HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "CONSULTAR_ESTADO", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        
        try {
            String estado = esp32Service.obtenerEstadoConexion();
            
            registrarLog(userName, "CONSULTAR_ESTADO", "EXITOSO", 
                       "Estado consultado: " + estado, request);
            
            return ResponseEntity.ok("📡 Estado: " + estado);
        } catch (Exception e) {
            registrarLog(userName, "CONSULTAR_ESTADO", "ERROR", 
                       "Error obteniendo estado: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener estado: " + e.getMessage());
        }
    }

    @PostMapping("/test-conexion")
    public ResponseEntity<String> testConexion(HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "TEST_CONEXION", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        System.out.println("🧪 Usuario " + userName + " ejecuta test de conexión");
        
        try {
            boolean resultado = esp32Service.enviarComando("TEST_CONNECTION");
            
            if (resultado) {
                registrarLog(userName, "TEST_CONEXION", "EXITOSO", 
                           "Test de conexión enviado exitosamente", request);
                return ResponseEntity.ok("🧪 Test de conexión enviado - Verifica el parpadeo del LED en el receptor");
            } else {
                registrarLog(userName, "TEST_CONEXION", "FALLIDO", 
                           "ESP32 no respondió al test", request);
                return ResponseEntity.badRequest().body("❌ Error en test de conexión");
            }
        } catch (Exception e) {
            registrarLog(userName, "TEST_CONEXION", "ERROR", 
                       "Error en test: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error en test: " + e.getMessage());
        }
    }

    @GetMapping("/configuracion")
    public ResponseEntity<String> obtenerConfiguracion(HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "CONSULTAR_CONFIG", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        
        try {
            String config = "📊 Configuración actual del sistema ESP32";
            String estado = esp32Service.obtenerEstadoConexion();
            
            String respuesta = config + "\n" + estado;
            
            registrarLog(userName, "CONSULTAR_CONFIG", "EXITOSO", 
                       "Configuración consultada exitosamente", request);
            
            return ResponseEntity.ok(respuesta);
        } catch (Exception e) {
            registrarLog(userName, "CONSULTAR_CONFIG", "ERROR", 
                       "Error obteniendo configuración: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener configuración: " + e.getMessage());
        }
    }

    /**
     * Endpoint especial para verificación rápida (sin autenticación para debugging)
     */
    @GetMapping("/ping")
    public ResponseEntity<String> ping(HttpServletRequest request) {
        // Log para ping sin autenticación (debugging)
        registrarLog("SISTEMA", "PING", "EXITOSO", "Ping de verificación del sistema", request);
        
        return ResponseEntity.ok("🏃‍♂️ ESP32 Controller está funcionando - " + 
                                LocalDateTime.now().toString());
    }

    /**
     * Endpoint para obtener estadísticas de uso del ESP32
     */
    @GetMapping("/stats")
    public ResponseEntity<?> obtenerEstadisticasESP32(HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) {
            registrarLog("ANONIMO", "CONSULTAR_STATS_ESP32", "ERROR", "Sesión no válida", request);
            return ResponseEntity.status(401).body("❌ Sesión no válida");
        }
        
        String userName = (String) session.getAttribute("userName");
        
        try {
            // Obtener estadísticas de uso del ESP32 desde los logs
            long totalComandos = logAccesoDao.countByUserName(userName);
            long aperturasHoy = logAccesoDao.findLogsUsuarioHoy(userName).stream()
                .mapToLong(log -> log.getAccion().contains("ABRIR") ? 1 : 0)
                .sum();
            
            java.util.Map<String, Object> stats = new java.util.HashMap<>();
            stats.put("usuario", userName);
            stats.put("totalComandosEnviados", totalComandos);
            stats.put("aperturasHoy", aperturasHoy);
            stats.put("timestamp", LocalDateTime.now());
            
            registrarLog(userName, "CONSULTAR_STATS_ESP32", "EXITOSO", 
                       "Estadísticas ESP32 consultadas", request);
            
            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            registrarLog(userName, "CONSULTAR_STATS_ESP32", "ERROR", 
                       "Error obteniendo estadísticas: " + e.getMessage(), request);
            return ResponseEntity.internalServerError()
                .body("💥 Error al obtener estadísticas: " + e.getMessage());
        }
    }
}