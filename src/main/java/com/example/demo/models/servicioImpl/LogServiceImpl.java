package com.example.demo.models.servicioImpl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.models.dao.LogAccesoDao;
import com.example.demo.models.entity.LogAcceso;
import com.example.demo.models.servicio.LogService;

import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
@Transactional
public class LogServiceImpl implements LogService {
    
    @Autowired
    private LogAccesoDao logAccesoDao;
    
    @Override
    public void registrarAccesoExitoso(String userName, String accion) {
        try {
            LogAcceso log = new LogAcceso(userName, accion, "EXITOSO");
            logAccesoDao.save(log);
            System.out.println("✅ Log registrado: " + userName + " - " + accion + " - EXITOSO");
        } catch (Exception e) {
            System.err.println("❌ Error al registrar log exitoso: " + e.getMessage());
        }
    }
    
    @Override
    public void registrarAccesoError(String userName, String accion, String detallesError) {
        try {
            LogAcceso log = new LogAcceso(userName, accion, "ERROR");
            log.setDetalles(detallesError);
            logAccesoDao.save(log);
            System.out.println("❌ Log de error registrado: " + userName + " - " + accion + " - ERROR");
        } catch (Exception e) {
            System.err.println("❌ Error al registrar log de error: " + e.getMessage());
        }
    }
    
    @Override
    public void registrarAcceso(String userName, String accion, String resultado, 
                               String direccionIp, String userAgent, String detalles) {
        try {
            LogAcceso log = new LogAcceso(userName, accion, resultado, direccionIp, userAgent, detalles);
            logAccesoDao.save(log);
            System.out.println("📝 Log completo registrado: " + userName + " - " + accion + " - " + resultado);
        } catch (Exception e) {
            System.err.println("❌ Error al registrar log completo: " + e.getMessage());
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerLogsHoy() {
        try {
            LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
            LocalDateTime finHoy = inicioHoy.plusDays(1);
            return logAccesoDao.findByFechaHoraBetweenOrderByFechaHoraDesc(inicioHoy, finHoy);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener logs de hoy: " + e.getMessage());
            return List.of(); // Lista vacía en caso de error
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public Object[] obtenerEstadisticasHoy() {
        try {
            return logAccesoDao.getEstadisticasHoy();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener estadísticas de hoy: " + e.getMessage());
            return new Object[]{0L, 0L, 0L, 0L}; // Valores por defecto
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerLogsPorUsuario(String userName) {
        try {
            return logAccesoDao.findByUserName(userName);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener logs por usuario: " + e.getMessage());
            return List.of();
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerUltimosLogs(int cantidad) {
        try {
            return logAccesoDao.findTopNByOrderByFechaHoraDesc(PageRequest.of(0, cantidad));
        } catch (Exception e) {
            System.err.println("❌ Error al obtener últimos logs: " + e.getMessage());
            return List.of();
        }
    }
    
    @Override
    @Transactional
    public void limpiarLogsAntiguos(int diasAntes) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntes);
            logAccesoDao.deleteLogsAnterioresA(fechaLimite);
            System.out.println("🧹 Logs anteriores a " + fechaLimite + " eliminados");
        } catch (Exception e) {
            System.err.println("❌ Error al limpiar logs antiguos: " + e.getMessage());
        }
    }
    
    /**
     * Registrar acceso con información HTTP completa
     */
    public void registrarAccesoHttp(String userName, String accion, String resultado, 
                                   HttpServletRequest request) {
        try {
            String direccionIp = obtenerDireccionIpReal(request);
            String userAgent = request.getHeader("User-Agent");
            
            registrarAcceso(userName, accion, resultado, direccionIp, userAgent, null);
        } catch (Exception e) {
            System.err.println("❌ Error al registrar acceso HTTP: " + e.getMessage());
        }
    }
    
    /**
     * Registrar acceso HTTP con detalles específicos
     */
    public void registrarAccesoHttpDetallado(String userName, String accion, String resultado, 
                                           String detalles, HttpServletRequest request) {
        try {
            String direccionIp = obtenerDireccionIpReal(request);
            String userAgent = request.getHeader("User-Agent");
            
            registrarAcceso(userName, accion, resultado, direccionIp, userAgent, detalles);
        } catch (Exception e) {
            System.err.println("❌ Error al registrar acceso HTTP detallado: " + e.getMessage());
        }
    }
    
    /**
     * Registrar login exitoso
     */
    public void registrarLogin(String userName, HttpServletRequest request) {
        registrarAccesoHttpDetallado(userName, "LOGIN", "EXITOSO", 
                                   "Inicio de sesión exitoso", request);
    }
    
    /**
     * Registrar login fallido
     */
    public void registrarLoginFallido(String userName, String razon, HttpServletRequest request) {
        registrarAccesoHttpDetallado(userName, "LOGIN", "ERROR", 
                                   "Login fallido: " + razon, request);
    }
    
    /**
     * Registrar logout
     */
    public void registrarLogout(String userName, HttpServletRequest request) {
        registrarAccesoHttpDetallado(userName, "LOGOUT", "EXITOSO", 
                                   "Cierre de sesión", request);
    }
    
    /**
     * Registrar comando ESP32
     */
    public void registrarComandoESP32(String userName, String comando, String resultado, 
                                    String detalles, HttpServletRequest request) {
        String accion = "ESP32_" + comando.toUpperCase();
        registrarAccesoHttpDetallado(userName, accion, resultado, detalles, request);
    }
    
    /**
     * Registrar apertura de puerta
     */
    public void registrarAperturaPuerta(String userName, String resultado, 
                                      String detalles, HttpServletRequest request) {
        registrarAccesoHttpDetallado(userName, "ABRIR_PUERTA", resultado, detalles, request);
    }
    
    /**
     * Registrar acceso denegado
     */
    public void registrarAccesoDenegado(String userName, String accion, String razon, 
                                      HttpServletRequest request) {
        registrarAccesoHttpDetallado(userName != null ? userName : "ANONIMO", 
                                   accion, "DENEGADO", "Acceso denegado: " + razon, request);
    }
    
    /**
     * Obtener estadísticas generales del sistema
     */
    @Transactional(readOnly = true)
    public Object[] obtenerEstadisticasGenerales() {
        try {
            return logAccesoDao.getEstadisticasGenerales();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener estadísticas generales: " + e.getMessage());
            return new Object[]{0L, 0L, 0L, 0L, 0L, 0L};
        }
    }
    
    /**
     * Obtener logs por filtro específico
     */
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerLogsPorFiltro(String filtro) {
        try {
            switch (filtro.toLowerCase()) {
                case "exitosos":
                    return logAccesoDao.findByResultadoOrderByFechaHoraDesc("EXITOSO");
                case "errores":
                    return logAccesoDao.findByResultadoOrderByFechaHoraDesc("ERROR");
                case "aperturas":
                    return logAccesoDao.findLogsAperturaPuerta();
                case "autenticacion":
                    return logAccesoDao.findLogsAutenticacion();
                case "esp32":
                    return logAccesoDao.findLogsComandosESP32();
                default:
                    return logAccesoDao.findAllOrderByFechaHoraDesc();
            }
        } catch (Exception e) {
            System.err.println("❌ Error al obtener logs por filtro: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Buscar logs con término de búsqueda
     */
    @Transactional(readOnly = true)
    public List<LogAcceso> buscarLogs(String termino) {
        try {
            return logAccesoDao.findBySearchTerm(termino);
        } catch (Exception e) {
            System.err.println("❌ Error al buscar logs: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtener logs entre fechas
     */
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerLogsEntreFechas(LocalDateTime fechaInicio, LocalDateTime fechaFin) {
        try {
            return logAccesoDao.findByFechaHoraBetweenOrderByFechaHoraDesc(fechaInicio, fechaFin);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener logs entre fechas: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtener actividad por horas
     */
    @Transactional(readOnly = true)
    public List<Object[]> obtenerActividadPorHoras() {
        try {
            // Usar el método default que calcula las fechas automáticamente
            return logAccesoDao.getActividadPorHoras();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener actividad por horas: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtener usuarios más activos
     */
    @Transactional(readOnly = true)
    public List<Object[]> obtenerUsuariosMasActivos() {
        try {
            return logAccesoDao.getUsuariosMasActivos();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener usuarios más activos: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtener errores recientes
     */
    @Transactional(readOnly = true)
    public List<LogAcceso> obtenerErroresRecientes(int horas) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusHours(horas);
            return logAccesoDao.getErroresRecientes(fechaLimite);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener errores recientes: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Verificar actividad reciente de un usuario
     */
    @Transactional(readOnly = true)
    public boolean tieneActividadReciente(String userName, int minutos) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusMinutes(minutos);
            return logAccesoDao.hasRecentActivity(userName, fechaLimite);
        } catch (Exception e) {
            System.err.println("❌ Error al verificar actividad reciente: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtener estadísticas por acción
     */
    @Transactional(readOnly = true)
    public List<Object[]> obtenerEstadisticasPorAccion() {
        try {
            return logAccesoDao.getEstadisticasPorAccion();
        } catch (Exception e) {
            System.err.println("❌ Error al obtener estadísticas por acción: " + e.getMessage());
            return List.of();
        }
    }
    
    /**
     * Obtener resumen del sistema
     */
    @Transactional(readOnly = true)
    public Map<String, Object> obtenerResumenSistema() {
        Map<String, Object> resumen = new HashMap<>();
        
        try {
            // Estadísticas generales
            Object[] estadisticas = obtenerEstadisticasGenerales();
            if (estadisticas != null && estadisticas.length >= 6) {
                resumen.put("totalLogs", ((Number) estadisticas[0]).longValue());
                resumen.put("logsExitosos", ((Number) estadisticas[1]).longValue());
                resumen.put("logsErrores", ((Number) estadisticas[2]).longValue());
                resumen.put("aperturasPuerta", ((Number) estadisticas[4]).longValue());
            }
            
            // Estadísticas de hoy
            Object[] estadisticasHoy = obtenerEstadisticasHoy();
            if (estadisticasHoy != null && estadisticasHoy.length >= 4) {
                resumen.put("actividadHoy", ((Number) estadisticasHoy[0]).longValue());
                resumen.put("usuariosActivosHoy", ((Number) estadisticasHoy[2]).longValue());
            }
            
            // Actividad reciente
            LocalDateTime hace1Hora = LocalDateTime.now().minusHours(1);
            List<LogAcceso> actividadReciente = logAccesoDao.findLogsUltimaHora(hace1Hora);
            resumen.put("actividadUltimaHora", actividadReciente.size());
            
            // Errores recientes
            List<LogAcceso> erroresRecientes = obtenerErroresRecientes(24);
            resumen.put("errores24Horas", erroresRecientes.size());
            
            resumen.put("timestamp", LocalDateTime.now());
            resumen.put("sistemaOperativo", true);
            
        } catch (Exception e) {
            System.err.println("❌ Error al obtener resumen del sistema: " + e.getMessage());
            resumen.put("error", "Error al obtener resumen");
            resumen.put("sistemaOperativo", false);
        }
        
        return resumen;
    }
    
    /**
     * Exportar logs a formato CSV
     */
    @Transactional(readOnly = true)
    public String exportarLogsCSV(List<LogAcceso> logs) {
        StringBuilder csv = new StringBuilder();
        csv.append("Fecha,Usuario,Accion,Resultado,IP,Detalles\n");
        
        for (LogAcceso log : logs) {
            csv.append(log.getFechaHora()).append(",")
               .append(log.getUserName()).append(",")
               .append(log.getAccion()).append(",")
               .append(log.getResultado()).append(",")
               .append(log.getDireccionIp() != null ? log.getDireccionIp() : "").append(",")
               .append(log.getDetalles() != null ? log.getDetalles().replace(",", ";") : "")
               .append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Obtener la dirección IP real del cliente
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
    
    /**
     * Contar logs por usuario
     */
    @Transactional(readOnly = true)
    public long contarLogsPorUsuario(String userName) {
        try {
            return logAccesoDao.countByUserName(userName);
        } catch (Exception e) {
            System.err.println("❌ Error al contar logs por usuario: " + e.getMessage());
            return 0L;
        }
    }
    
    /**
     * Obtener último log de un usuario
     */
    @Transactional(readOnly = true)
    public LogAcceso obtenerUltimoLogUsuario(String userName) {
        try {
            List<LogAcceso> logs = logAccesoDao.findTopByUserNameOrderByFechaHoraDesc(
                userName, PageRequest.of(0, 1));
            return logs.isEmpty() ? null : logs.get(0);
        } catch (Exception e) {
            System.err.println("❌ Error al obtener último log del usuario: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Limpiar logs de un usuario específico
     */
    @Transactional
    public void limpiarLogsPorUsuario(String userName, int diasAntes) {
        try {
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAntes);
            logAccesoDao.deleteLogsByUserNameAndFechaAnterior(userName, fechaLimite);
            System.out.println("🧹 Logs del usuario " + userName + " anteriores a " + fechaLimite + " eliminados");
        } catch (Exception e) {
            System.err.println("❌ Error al limpiar logs por usuario: " + e.getMessage());
        }
    }
}