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
import com.example.demo.models.dao.UsuarioDao;
import com.example.demo.models.entity.LogAcceso;
import com.example.demo.models.entity.Usuario;

import java.util.HashMap;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@RestController
@RequestMapping("/api/esp32")
@CrossOrigin(origins = "*")
public class ESP32Controller {
    private final Map<String, Long> ultimoAccesoPorUsuario = new ConcurrentHashMap<>();
    @Autowired
    private ESP32Service esp32Service;
    
    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private LogAccesoDao logAccesoDao;

    @Autowired
    private UsuarioDao usuarioDao;

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
        
        // Obtener el ID del usuario desde la base de datos
        Long idUsuario = null;
        if (userName != null && !userName.equals("ANONIMO") && !userName.equals("SISTEMA")) {
            try {
                Optional<Usuario> usuarioOpt = usuarioDao.findByUserName(userName);
                if (usuarioOpt.isPresent()) {
                    idUsuario = usuarioOpt.get().getIdUsuario();
                    System.out.println("✅ ID Usuario encontrado: " + idUsuario + " para " + userName);
                } else {
                    System.out.println("⚠️ Usuario no encontrado en BD: " + userName);
                }
            } catch (Exception e) {
                System.err.println("⚠️ Error al buscar usuario en BD: " + userName + " - " + e.getMessage());
            }
        }
        
        LogAcceso log = new LogAcceso();
        log.setIdUsuario(idUsuario); // CRÍTICO: Setear el ID del usuario
        log.setUserName(userName);
        log.setAccion(accion);
        log.setResultado(resultado);
        log.setDetalles(detalles);
        log.setDireccionIp(direccionIp);
        log.setUserAgent(userAgent);
        log.setFechaHora(LocalDateTime.now());
        
        // Determinar tipo de acción basado en la acción
        String tipoAccion = determinarTipoAccion(accion);
        log.setTipoAccion(tipoAccion);
        
        // Guardar el log
        logAccesoDao.save(log);
        
        System.out.println("📝 Log registrado exitosamente: " + userName + " (ID: " + idUsuario + ") - " + accion + " - " + resultado);
        
    } catch (Exception e) {
        System.err.println("❌ Error registrando log: " + e.getMessage());
        e.printStackTrace(); // Para debug detallado
    }
}

/**
 * Determinar tipo de acción basado en la acción realizada
 */
private String determinarTipoAccion(String accion) {
    if (accion == null) return "SISTEMA";
    
    String accionUpper = accion.toUpperCase();
    
    if (accionUpper.contains("LOGIN") || accionUpper.contains("LOGOUT") || accionUpper.contains("AUTENTICAR")) {
        return "AUTENTICACION";
    } else if (accionUpper.contains("ABRIR") || accionUpper.contains("CERRAR") || 
               accionUpper.contains("ESP32") || accionUpper.contains("COMANDO") || 
               accionUpper.contains("TEST")) {
        return "ESP32";
    } else if (accionUpper.contains("ADMIN") || accionUpper.contains("DASHBOARD") || 
               accionUpper.contains("CONSULTAR")) {
        return "ADMINISTRACION";
    } else {
        return "SISTEMA";
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
    if (!isAuthenticated(session)) {
        registrarLog("ANONIMO", "ABRIR_PUERTA", "ERROR", "Sesión no válida", request);
        return ResponseEntity.status(401).body("❌ Sesión no válida - Inicia sesión");
    }

    String userName = (String) session.getAttribute("userName");

    // Protección de intervalo mínimo de 3 segundos
    long ahora = System.currentTimeMillis();
    long ultimo = ultimoAccesoPorUsuario.getOrDefault(userName, 0L);
    if (ahora - ultimo < 3000) {
        registrarLog(userName, "ABRIR_PUERTA", "IGNORADO", "Intento antes de los 3 segundos", request);
        return ResponseEntity.status(429).body("⚠️ Espera 3 segundos antes de volver a abrir la puerta");
    }

    ultimoAccesoPorUsuario.put(userName, ahora);
    System.out.println("🚪 Usuario " + userName + " solicita abrir puerta");

    try {
        boolean resultado = esp32Service.enviarComando("ABRIR_PUERTA");

        if (resultado) {
            registrarLog(userName, "ABRIR_PUERTA", "EXITOSO", "Comando enviado al ESP32", request);
            return ResponseEntity.ok("🚪 Comando ABRIR_PUERTA enviado exitosamente");
        } else {
            registrarLog(userName, "ABRIR_PUERTA", "FALLIDO", "ESP32 no respondió correctamente", request);
            return ResponseEntity.badRequest().body("❌ Error al enviar comando ABRIR_PUERTA");
        }

    } catch (Exception e) {
        registrarLog(userName, "ABRIR_PUERTA", "ERROR", "Error crítico: " + e.getMessage(), request);
        return ResponseEntity.internalServerError().body("💥 Error interno: " + e.getMessage());
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