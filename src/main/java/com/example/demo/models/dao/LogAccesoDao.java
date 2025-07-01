package com.example.demo.models.dao;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.models.entity.LogAcceso;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogAccesoDao extends JpaRepository<LogAcceso, Long> {
    
    /**
     * Obtener logs por usuario
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :userName ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByUserName(@Param("userName") String userName);
    
    /**
     * Obtener logs por usuario con nombre de campo correcto
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :usuario ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByUsuarioUserName(@Param("usuario") String usuario);
    
    /**
     * Obtener logs por acción
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.accion = :accion ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByAccion(@Param("accion") String accion);
    
    /**
     * Obtener logs por resultado ordenados por fecha
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.resultado = :resultado ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByResultadoOrderByFechaHoraDesc(@Param("resultado") String resultado);
    
    /**
     * Obtener todos los logs ordenados por fecha
     */
    @Query("SELECT l FROM LogAcceso l ORDER BY l.fechaHora DESC")
    List<LogAcceso> findAllOrderByFechaHoraDesc();
    
    /**
     * Obtener logs de apertura de puerta
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.accion LIKE '%ABRIR%' OR l.accion LIKE '%PUERTA%' ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsAperturaPuerta();
    
    /**
     * Obtener logs entre fechas
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.fechaHora BETWEEN :fechaInicio AND :fechaFin ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByFechaHoraBetweenOrderByFechaHoraDesc(@Param("fechaInicio") LocalDateTime fechaInicio, 
                                                              @Param("fechaFin") LocalDateTime fechaFin);
    
    /**
     * Obtener logs de la última semana
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.fechaHora >= :fechaInicio ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsUltimaSemana(@Param("fechaInicio") LocalDateTime fechaInicio);
    
    /**
     * Obtener últimos N logs con Pageable
     */
    @Query("SELECT l FROM LogAcceso l ORDER BY l.fechaHora DESC")
    List<LogAcceso> findTopNByOrderByFechaHoraDesc(Pageable pageable);

    @Query("SELECT l FROM LogAcceso l WHERE l.tipoAccion = :tipoAccion ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByTipoAccion(@Param("tipoAccion") String tipoAccion);

    @Query("SELECT COUNT(l) FROM LogAcceso l WHERE l.tipoAccion = :tipoAccion")
    long countByTipoAccion(@Param("tipoAccion") String tipoAccion);


    @Query("SELECT l.tipoAccion, COUNT(l) as total, " +
       "SUM(CASE WHEN l.resultado = 'EXITOSO' THEN 1 ELSE 0 END) as exitosos, " +
       "SUM(CASE WHEN l.resultado = 'ERROR' THEN 1 ELSE 0 END) as errores " +
       "FROM LogAcceso l GROUP BY l.tipoAccion ORDER BY total DESC")
    List<Object[]> getEstadisticasPorTipoAccion();

    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :userName AND l.tipoAccion = :tipoAccion ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByUserNameAndTipoAccion(@Param("userName") String userName, @Param("tipoAccion") String tipoAccion);

    /**
     * Obtener estadísticas generales del sistema
     */
    @Query("SELECT " +
           "COUNT(l) as totalLogs, " +
           "SUM(CASE WHEN l.resultado = 'EXITOSO' THEN 1 ELSE 0 END) as exitosos, " +
           "SUM(CASE WHEN l.resultado = 'ERROR' THEN 1 ELSE 0 END) as errores, " +
           "SUM(CASE WHEN l.resultado = 'FALLIDO' THEN 1 ELSE 0 END) as fallidos, " +
           "SUM(CASE WHEN l.accion LIKE '%ABRIR%' OR l.accion LIKE '%PUERTA%' THEN 1 ELSE 0 END) as aperturas, " +
           "SUM(CASE WHEN l.accion LIKE '%LOGIN%' OR l.accion LIKE '%AUTENTICAR%' THEN 1 ELSE 0 END) as logins " +
           "FROM LogAcceso l")
    Object[] getEstadisticasGenerales();
    
    /**
     * Obtener estadísticas de hoy - SIN FUNCIONES DE FECHA
     */
    @Query("SELECT " +
           "COUNT(l) as logsHoy, " +
           "SUM(CASE WHEN l.accion LIKE '%ABRIR%' OR l.accion LIKE '%PUERTA%' THEN 1 ELSE 0 END) as aperturasHoy, " +
           "COUNT(DISTINCT l.userName) as usuariosActivosHoy, " +
           "SUM(CASE WHEN l.resultado = 'ERROR' THEN 1 ELSE 0 END) as erroresHoy " +
           "FROM LogAcceso l WHERE l.fechaHora >= :inicioHoy AND l.fechaHora < :finHoy")
    Object[] getEstadisticasHoy(@Param("inicioHoy") LocalDateTime inicioHoy, @Param("finHoy") LocalDateTime finHoy);
    
    /**
     * Obtener usuarios más activos
     */
    @Query("SELECT l.userName, COUNT(l) as totalAcciones, " +
           "SUM(CASE WHEN l.resultado = 'EXITOSO' THEN 1 ELSE 0 END) as exitosos " +
           "FROM LogAcceso l " +
           "GROUP BY l.userName " +
           "ORDER BY totalAcciones DESC")
    List<Object[]> getUsuariosMasActivos();
    
    /**
     * Buscar logs por término de búsqueda
     */
    @Query("SELECT l FROM LogAcceso l WHERE " +
           "LOWER(l.userName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.accion) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(l.resultado) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(COALESCE(l.detalles, '')) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
           "ORDER BY l.fechaHora DESC")
    List<LogAcceso> findBySearchTerm(@Param("searchTerm") String searchTerm);
    
    /**
     * Obtener actividad por horas - SIMPLIFICADA (SOLO CUENTA POR PERÍODO)
     */
    @Query("SELECT l.fechaHora, COUNT(l) as cantidad " +
           "FROM LogAcceso l " +
           "WHERE l.fechaHora >= :inicioHoy AND l.fechaHora < :finHoy " +
           "GROUP BY l.fechaHora " +
           "ORDER BY l.fechaHora")
    List<Object[]> getActividadPorHoras(@Param("inicioHoy") LocalDateTime inicioHoy, @Param("finHoy") LocalDateTime finHoy);
    
    /**
     * Obtener errores recientes
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.resultado = 'ERROR' AND l.fechaHora >= :fechaLimite ORDER BY l.fechaHora DESC")
    List<LogAcceso> getErroresRecientes(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Eliminar logs anteriores a una fecha específica
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM LogAcceso l WHERE l.fechaHora < :fechaLimite")
    void deleteLogsAnterioresA(@Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Contar logs por usuario
     */
    @Query("SELECT COUNT(l) FROM LogAcceso l WHERE l.userName = :userName")
    long countByUserName(@Param("userName") String userName);
    
    /**
     * Obtener logs por usuario y acción
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :userName AND l.accion = :accion ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByUserNameAndAccion(@Param("userName") String userName, @Param("accion") String accion);
    
    /**
     * Obtener logs de la última hora
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.fechaHora >= :fechaHace1Hora ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsUltimaHora(@Param("fechaHace1Hora") LocalDateTime fechaHace1Hora);
    
    /**
     * Obtener logs de un usuario específico hoy
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :userName AND l.fechaHora >= :inicioHoy AND l.fechaHora < :finHoy ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsUsuarioHoy(@Param("userName") String userName, @Param("inicioHoy") LocalDateTime inicioHoy, @Param("finHoy") LocalDateTime finHoy);
    
    /**
     * Verificar si hay actividad reciente de un usuario
     */
    @Query("SELECT COUNT(l) > 0 FROM LogAcceso l WHERE l.userName = :userName AND l.fechaHora >= :fechaLimite")
    boolean hasRecentActivity(@Param("userName") String userName, @Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Obtener logs de autenticación
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.accion LIKE '%LOGIN%' OR l.accion LIKE '%LOGOUT%' OR l.accion LIKE '%AUTENTICAR%' ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsAutenticacion();
    
    /**
     * Obtener logs por IP específica
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.direccionIp = :ip ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByDireccionIp(@Param("ip") String ip);
    
    /**
     * Obtener IPs más activas
     */
    @Query("SELECT l.direccionIp, COUNT(l) as cantidad " +
           "FROM LogAcceso l " +
           "WHERE l.direccionIp IS NOT NULL " +
           "GROUP BY l.direccionIp " +
           "ORDER BY cantidad DESC")
    List<Object[]> getIpsMasActivas();
    
    /**
     * Obtener logs por tipo de resultado y período
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.resultado = :resultado AND l.fechaHora BETWEEN :fechaInicio AND :fechaFin ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByResultadoAndFechaBetween(@Param("resultado") String resultado, 
                                                  @Param("fechaInicio") LocalDateTime fechaInicio, 
                                                  @Param("fechaFin") LocalDateTime fechaFin);
    
    /**
     * Contar acciones por usuario en un período
     */
    @Query("SELECT COUNT(l) FROM LogAcceso l WHERE l.userName = :userName AND l.fechaHora BETWEEN :fechaInicio AND :fechaFin")
    long countByUserNameAndFechaBetween(@Param("userName") String userName, 
                                       @Param("fechaInicio") LocalDateTime fechaInicio, 
                                       @Param("fechaFin") LocalDateTime fechaFin);
    
    /**
     * Obtener el último log de un usuario
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.userName = :userName ORDER BY l.fechaHora DESC")
    List<LogAcceso> findTopByUserNameOrderByFechaHoraDesc(@Param("userName") String userName, Pageable pageable);
    
    /**
     * Obtener logs de comandos ESP32 específicos
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.accion IN ('ABRIR_PUERTA', 'CERRAR_PUERTA', 'TEST_CONEXION', 'COMANDO_PERSONALIZADO') ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsComandosESP32();
    
    /**
     * Estadísticas de éxito por acción
     */
    @Query("SELECT l.accion, " +
           "COUNT(l) as total, " +
           "SUM(CASE WHEN l.resultado = 'EXITOSO' THEN 1 ELSE 0 END) as exitosos, " +
           "SUM(CASE WHEN l.resultado = 'ERROR' THEN 1 ELSE 0 END) as errores " +
           "FROM LogAcceso l " +
           "GROUP BY l.accion " +
           "ORDER BY total DESC")
    List<Object[]> getEstadisticasPorAccion();
    
    /**
     * Logs de hoy con paginación
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.fechaHora >= :inicioHoy AND l.fechaHora < :finHoy ORDER BY l.fechaHora DESC")
    List<LogAcceso> findLogsHoyPaginado(@Param("inicioHoy") LocalDateTime inicioHoy, @Param("finHoy") LocalDateTime finHoy, Pageable pageable);
    
    /**
     * Eliminar logs específicos de un usuario
     */
    @Modifying
    @Transactional
    @Query("DELETE FROM LogAcceso l WHERE l.userName = :userName AND l.fechaHora < :fechaLimite")
    void deleteLogsByUserNameAndFechaAnterior(@Param("userName") String userName, @Param("fechaLimite") LocalDateTime fechaLimite);
    
    /**
     * Buscar logs por múltiples criterios
     */
    @Query("SELECT l FROM LogAcceso l WHERE " +
           "(:userName IS NULL OR l.userName = :userName) AND " +
           "(:accion IS NULL OR l.accion LIKE CONCAT('%', :accion, '%')) AND " +
           "(:resultado IS NULL OR l.resultado = :resultado) AND " +
           "(:fechaInicio IS NULL OR l.fechaHora >= :fechaInicio) AND " +
           "(:fechaFin IS NULL OR l.fechaHora <= :fechaFin) " +
           "ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByCriteriosMultiples(@Param("userName") String userName,
                                           @Param("accion") String accion,
                                           @Param("resultado") String resultado,
                                           @Param("fechaInicio") LocalDateTime fechaInicio,
                                           @Param("fechaFin") LocalDateTime fechaFin);
    
    /**
     * Obtener logs con detalles específicos
     */
    @Query("SELECT l FROM LogAcceso l WHERE l.detalles IS NOT NULL AND l.detalles LIKE CONCAT('%', :detalle, '%') ORDER BY l.fechaHora DESC")
    List<LogAcceso> findByDetallesContaining(@Param("detalle") String detalle);
    
    /**
     * MÉTODOS AUXILIARES DEFAULT PARA COMPATIBILIDAD
     */
    
    /**
     * Método auxiliar para obtener logs de hoy (sin parámetros)
     */
    default List<LogAcceso> findLogsHoy() {
        LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        return findByFechaHoraBetweenOrderByFechaHoraDesc(inicioHoy, finHoy);
    }
    
    /**
     * Método auxiliar para obtener estadísticas de hoy (sin parámetros)
     */
    default Object[] getEstadisticasHoy() {
        LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        return getEstadisticasHoy(inicioHoy, finHoy);
    }
    
    /**
     * Método auxiliar para obtener actividad por horas de hoy (simplificado)
     */
    default List<Object[]> getActividadPorHoras() {
        LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        return getActividadPorHoras(inicioHoy, finHoy);
    }
    
    /**
     * Método auxiliar para obtener logs de usuario hoy
     */
    default List<LogAcceso> findLogsUsuarioHoy(String userName) {
        LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime finHoy = inicioHoy.plusDays(1);
        return findLogsUsuarioHoy(userName, inicioHoy, finHoy);
    }
    
    /**
     * Método auxiliar simplificado para actividad por día de semana
     * (devuelve logs agrupados por fecha para procesamiento en Java)
     */
    default List<Object[]> getActividadPorDiaSemana(LocalDateTime fechaInicio) {
        // En lugar de usar funciones SQL problemáticas, devolvemos logs por fecha
        // para que el servicio los procese en Java
        return findByFechaHoraBetweenOrderByFechaHoraDesc(fechaInicio, LocalDateTime.now())
               .stream()
               .collect(java.util.stream.Collectors.groupingBy(
                   log -> log.getFechaHora().toLocalDate(),
                   java.util.stream.Collectors.counting()
               ))
               .entrySet()
               .stream()
               .map(entry -> new Object[]{entry.getKey(), entry.getValue()})
               .toList();
    }
}