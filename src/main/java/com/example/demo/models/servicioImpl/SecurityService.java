package com.example.demo.models.servicioImpl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

@Service
public class SecurityService {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityService.class);
    
    // Almacenamiento en memoria de intentos por IP
    private final Map<String, AttemptInfo> loginAttempts = new ConcurrentHashMap<>();
    
    // Configuración
    private static final int MAX_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION = 3600000; // 1 hora en milisegundos
    
    // Clase interna para almacenar información de intentos
    public static class AttemptInfo {
        private int attempts;
        private long lastAttemptTime;
        private boolean isBlocked;
        
        public AttemptInfo(int attempts, long lastAttemptTime, boolean isBlocked) {
            this.attempts = attempts;
            this.lastAttemptTime = lastAttemptTime;
            this.isBlocked = isBlocked;
        }
        
        // Getters y setters
        public int getAttempts() { return attempts; }
        public void setAttempts(int attempts) { this.attempts = attempts; }
        
        public long getLastAttemptTime() { return lastAttemptTime; }
        public void setLastAttemptTime(long lastAttemptTime) { this.lastAttemptTime = lastAttemptTime; }
        
        public boolean isBlocked() { return isBlocked; }
        public void setBlocked(boolean blocked) { isBlocked = blocked; }
    }
    
    /**
     * Verificar si una IP está bloqueada
     */
    public boolean isBlocked(String ip) {
        AttemptInfo info = loginAttempts.get(ip);
        
        if (info == null) {
            return false;
        }
        
        // Verificar si el bloqueo ha expirado
        if (info.isBlocked()) {
            long timeElapsed = System.currentTimeMillis() - info.getLastAttemptTime();
            
            if (timeElapsed > LOCKOUT_DURATION) {
                // El bloqueo ha expirado, eliminar el registro
                loginAttempts.remove(ip);
                logger.info("🔓 Bloqueo expirado para IP: {}", ip);
                return false;
            }
            
            return true; // Aún está bloqueado
        }
        
        return false;
    }
    
    /**
     * Registrar un intento de login fallido
     */
    public void recordFailedAttempt(String ip) {
        long currentTime = System.currentTimeMillis();
        
        AttemptInfo info = loginAttempts.computeIfAbsent(ip, 
            k -> new AttemptInfo(0, currentTime, false));
        
        // Incrementar contador de intentos
        info.setAttempts(info.getAttempts() + 1);
        info.setLastAttemptTime(currentTime);
        
        logger.warn("⚠️ Intento de login fallido #{} para IP: {}", info.getAttempts(), ip);
        
        // Verificar si se alcanzó el límite
        if (info.getAttempts() >= MAX_ATTEMPTS) {
            info.setBlocked(true);
            logger.warn("🚨 IP BLOQUEADA: {} - {} intentos fallidos consecutivos", 
                ip, info.getAttempts());
        }
    }
    
    /**
     * Registrar un login exitoso (limpiar intentos fallidos)
     */
    public void recordSuccessfulLogin(String ip) {
        AttemptInfo removed = loginAttempts.remove(ip);
        if (removed != null) {
            logger.info("✅ Login exitoso para IP: {} - Intentos fallidos eliminados", ip);
        }
    }
    
    /**
     * Obtener intentos restantes para una IP
     */
    public int getRemainingAttempts(String ip) {
        AttemptInfo info = loginAttempts.get(ip);
        
        if (info == null) {
            return MAX_ATTEMPTS;
        }
        
        return Math.max(0, MAX_ATTEMPTS - info.getAttempts());
    }
    
    /**
     * Obtener tiempo restante de bloqueo en minutos
     */
    public long getRemainingLockoutTime(String ip) {
        AttemptInfo info = loginAttempts.get(ip);
        
        if (info == null || !info.isBlocked()) {
            return 0;
        }
        
        long timeElapsed = System.currentTimeMillis() - info.getLastAttemptTime();
        long remainingTime = LOCKOUT_DURATION - timeElapsed;
        
        return Math.max(0, remainingTime / (60 * 1000)); // Convertir a minutos
    }
    
    /**
     * Obtener información detallada de una IP (para debugging)
     */
    public String getIPStatus(String ip) {
        AttemptInfo info = loginAttempts.get(ip);
        
        if (info == null) {
            return String.format("IP: %s - Sin intentos registrados", ip);
        }
        
        return String.format(
            "IP: %s - Intentos: %d/%d - Bloqueado: %s - Tiempo restante: %d min",
            ip, 
            info.getAttempts(), 
            MAX_ATTEMPTS,
            info.isBlocked() ? "SÍ" : "NO",
            getRemainingLockoutTime(ip)
        );
    }
    
    /**
     * Desbloquear manualmente una IP (para administradores)
     */
    public boolean unblockIP(String ip) {
        AttemptInfo removed = loginAttempts.remove(ip);
        if (removed != null) {
            logger.info("🔓 IP desbloqueada manualmente: {}", ip);
            return true;
        }
        return false;
    }
    
    /**
     * Obtener estadísticas del sistema de seguridad
     */
    public Map<String, Object> getSecurityStats() {
        Map<String, Object> stats = new ConcurrentHashMap<>();
        
        long blockedIPs = loginAttempts.values().stream()
            .mapToLong(info -> info.isBlocked() ? 1 : 0)
            .sum();
        
        long totalAttempts = loginAttempts.values().stream()
            .mapToLong(AttemptInfo::getAttempts)
            .sum();
        
        stats.put("totalTrackedIPs", loginAttempts.size());
        stats.put("blockedIPs", blockedIPs);
        stats.put("totalFailedAttempts", totalAttempts);
        stats.put("maxAttemptsAllowed", MAX_ATTEMPTS);
        stats.put("lockoutDurationMinutes", LOCKOUT_DURATION / (60 * 1000));
        
        return stats;
    }
}