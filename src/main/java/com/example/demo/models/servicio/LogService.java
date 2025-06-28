package com.example.demo.models.servicio;

import com.example.demo.models.entity.LogAcceso;
import java.util.List;
import java.time.LocalDateTime;

public interface LogService {
    
    /**
     * Registrar un acceso exitoso
     */
    void registrarAccesoExitoso(String userName, String accion);
    
    /**
     * Registrar un acceso con error
     */
    void registrarAccesoError(String userName, String accion, String detallesError);
    
    /**
     * Registrar un acceso completo
     */
    void registrarAcceso(String userName, String accion, String resultado, 
                        String direccionIp, String userAgent, String detalles);
    
    /**
     * Obtener logs de hoy
     */
    List<LogAcceso> obtenerLogsHoy();
    
    /**
     * Obtener estadísticas de hoy
     */
    Object[] obtenerEstadisticasHoy();
    
    /**
     * Obtener logs por usuario
     */
    List<LogAcceso> obtenerLogsPorUsuario(String userName);
    
    /**
     * Obtener últimos logs
     */
    List<LogAcceso> obtenerUltimosLogs(int cantidad);
    
    /**
     * Limpiar logs antiguos
     */
    void limpiarLogsAntiguos(int diasAntes);
}