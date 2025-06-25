package com.example.demo.models.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.models.entity.Usuario;
import java.util.List;
import java.util.Optional;

@Repository
public interface UsuarioDao extends JpaRepository<Usuario, Long> {
    
    @Query("SELECT u FROM Usuario u WHERE u.user_name = :userName AND u.clave = :clave")
    Optional<Usuario> findByUserNameAndClave(@Param("userName") String userName, @Param("clave") String clave);
    
    @Query("SELECT u FROM Usuario u WHERE u.token = :token")
    Optional<Usuario> findByToken(@Param("token") String token);
    
    @Query("SELECT u FROM Usuario u WHERE u.user_name = :userName")
    Optional<Usuario> findByUserName(@Param("userName") String userName);
    
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.token = NULL WHERE u.user_name = :userName")
    void clearTokenByUserName(@Param("userName") String userName);
    
    /**
     * Buscar usuarios por nombre de usuario (búsqueda parcial, insensible a mayúsculas)
     */
    @Query("SELECT u FROM Usuario u WHERE LOWER(u.user_name) LIKE LOWER(CONCAT('%', :userName, '%'))")
    List<Usuario> findByUserNameContainingIgnoreCase(@Param("userName") String userName);
    
    /**
     * Contar usuarios por estado
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.estado = :estado")
    long countByEstado(@Param("estado") String estado);
    
    /**
     * Contar usuarios que tienen token (están conectados)
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.token IS NOT NULL")
    long countByTokenIsNotNull();
    
    /**
     * Buscar usuarios por estado
     */
    @Query("SELECT u FROM Usuario u WHERE u.estado = :estado")
    List<Usuario> findByEstado(@Param("estado") String estado);
    
    /**
     * Buscar usuarios por rol
     */
    @Query("SELECT u FROM Usuario u WHERE u.rol.nombre = :rolNombre")
    List<Usuario> findByRolNombre(@Param("rolNombre") String rolNombre);
    
    /**
     * Obtener todos los usuarios ordenados por fecha de registro (más recientes primero)
     */
    @Query("SELECT u FROM Usuario u ORDER BY u.fechaRegistro DESC")
    List<Usuario> findAllOrderByFechaRegistroDesc();
    
    /**
     * Obtener usuarios activos con token
     */
    @Query("SELECT u FROM Usuario u WHERE u.estado = 'ACTIVO' AND u.token IS NOT NULL")
    List<Usuario> findActiveUsersWithToken();
    
    /**
     * Búsqueda avanzada de usuarios (por múltiples criterios)
     */
    @Query("SELECT u FROM Usuario u LEFT JOIN u.persona p WHERE " +
           "LOWER(u.user_name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.estado) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.rol.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "(p.nombre IS NOT NULL AND LOWER(p.nombre) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) OR " +
           "(p.paterno IS NOT NULL AND LOWER(p.paterno) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) OR " +
           "(p.ci IS NOT NULL AND LOWER(p.ci) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Usuario> findBySearchTerm(@Param("searchTerm") String searchTerm);
    
    /**
     * Obtener usuarios registrados en un período de tiempo
     */
    @Query("SELECT u FROM Usuario u WHERE u.fechaRegistro BETWEEN :fechaInicio AND :fechaFin")
    List<Usuario> findByFechaRegistroBetween(@Param("fechaInicio") java.time.LocalDate fechaInicio, 
                                           @Param("fechaFin") java.time.LocalDate fechaFin);
    
    /**
     * Obtener usuarios con actividad reciente (por fecha de modificación)
     */
    @Query("SELECT u FROM Usuario u WHERE u.fechaModificacion >= :fechaLimite ORDER BY u.fechaModificacion DESC")
    List<Usuario> findByFechaModificacionAfter(@Param("fechaLimite") java.time.LocalDate fechaLimite);
    
    /**
     * Contar usuarios por rol
     */
    @Query("SELECT COUNT(u) FROM Usuario u WHERE u.rol.nombre = :rolNombre")
    long countByRolNombre(@Param("rolNombre") String rolNombre);
    
    /**
     * Obtener estadísticas completas del sistema
     */
    @Query("SELECT " +
           "COUNT(u) as total, " +
           "SUM(CASE WHEN u.estado = 'ACTIVO' THEN 1 ELSE 0 END) as activos, " +
           "SUM(CASE WHEN u.estado = 'INACTIVO' THEN 1 ELSE 0 END) as inactivos, " +
           "SUM(CASE WHEN u.token IS NOT NULL THEN 1 ELSE 0 END) as conToken, " +
           "SUM(CASE WHEN u.rol.nombre = 'ADMIN' THEN 1 ELSE 0 END) as administradores, " +
           "SUM(CASE WHEN u.rol.nombre = 'USUARIO' THEN 1 ELSE 0 END) as usuarios " +
           "FROM Usuario u")
    Object[] getSystemStats();
    
    /**
     * Buscar usuarios sin token (desconectados)
     */
    @Query("SELECT u FROM Usuario u WHERE u.token IS NULL")
    List<Usuario> findUsersWithoutToken();
    
    /**
     * Buscar usuarios con personas asociadas
     */
    @Query("SELECT u FROM Usuario u WHERE u.persona IS NOT NULL")
    List<Usuario> findUsersWithPersona();
    
    /**
     * Buscar usuarios sin personas asociadas (registros básicos)
     */
    @Query("SELECT u FROM Usuario u WHERE u.persona IS NULL")
    List<Usuario> findUsersWithoutPersona();
    
    /**
     * Obtener los últimos N usuarios registrados
     */
    @Query("SELECT u FROM Usuario u ORDER BY u.fechaRegistro DESC")
    List<Usuario> findTopByOrderByFechaRegistroDesc(org.springframework.data.domain.Pageable pageable);
    
    /**
     * Limpiar tokens de todos los usuarios (para mantenimiento)
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.token = NULL")
    void clearAllTokens();
    
    /**
     * Limpiar tokens de usuarios inactivos
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.token = NULL WHERE u.estado = 'INACTIVO'")
    void clearTokensFromInactiveUsers();
    
    /**
     * Actualizar fecha de modificación para un usuario específico
     */
    @Modifying
    @Transactional
    @Query("UPDATE Usuario u SET u.fechaModificacion = CURRENT_DATE WHERE u.idUsuario = :userId")
    void updateFechaModificacion(@Param("userId") Long userId);
    
    /**
     * Buscar usuarios por CI (a través de la relación con Persona)
     */
    @Query("SELECT u FROM Usuario u JOIN u.persona p WHERE p.ci = :ci")
    Optional<Usuario> findByPersonaCi(@Param("ci") String ci);
    
    /**
     * Obtener usuarios ordenados por última actividad
     */
    @Query("SELECT u FROM Usuario u ORDER BY " +
           "CASE WHEN u.fechaModificacion IS NOT NULL THEN u.fechaModificacion ELSE u.fechaRegistro END DESC")
    List<Usuario> findAllOrderByLastActivity();
    
    /**
     * Verificar si existe un usuario administrador
     */
    @Query("SELECT COUNT(u) > 0 FROM Usuario u WHERE u.rol.nombre = 'ADMIN' AND u.estado = 'ACTIVO'")
    boolean existsActiveAdmin();
    
    /**
     * Obtener usuarios que requieren atención (sin token por mucho tiempo)
     */
    @Query("SELECT u FROM Usuario u WHERE u.estado = 'ACTIVO' AND u.token IS NULL AND " +
           "u.fechaModificacion < :fechaLimite")
    List<Usuario> findUsersRequiringAttention(@Param("fechaLimite") java.time.LocalDate fechaLimite);
}