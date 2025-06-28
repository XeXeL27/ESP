package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.dao.UsuarioDao;
import com.example.demo.models.dao.RolDao;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.entity.Rol;
import com.example.demo.models.servicio.PasswordService;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @Autowired
    private UsuarioDao usuarioDao;
    
    @Autowired
    private RolDao rolDao;
    
    @Autowired
    private PasswordService passwordService;
    
    @GetMapping("/usuarios")
    public List<Usuario> listarUsuarios() {
        return usuarioDao.findAll();
    }
    
    @GetMapping("/roles")
    public List<Rol> listarRoles() {
        return rolDao.findAll();
    }
    
    @GetMapping("/test-db")
    public String testDatabase() {
        try {
            long usuarios = usuarioDao.count();
            long roles = rolDao.count();
            return String.format("✅ BD conectada - Usuarios: %d, Roles: %d", usuarios, roles);
        } catch (Exception e) {
            return "❌ Error de BD: " + e.getMessage();
        }
    }
    
    // ========== ENDPOINTS ACTUALIZADOS PARA SHA-256 ==========
    
    /**
     * 🔐 Test de contraseña de un usuario específico (SHA-256)
     */
    @GetMapping("/test-password/{username}")
    public String testPassword(@PathVariable String username) {
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findByUserName(username);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String claveEncriptada = usuario.getClave();
                String tipoEncriptacion = passwordService.obtenerTipoEncriptacion(claveEncriptada);
                
                return String.format("""
                    🔐 ANÁLISIS DE CONTRASEÑA PARA: %s
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    📝 Contraseña en BD: %s...
                    🔍 Tipo de encriptación: %s
                    📊 Longitud: %d caracteres
                    ✅ Estado: %s
                    💡 Recomendación: %s
                    """, 
                    username, 
                    claveEncriptada.substring(0, Math.min(20, claveEncriptada.length())),
                    tipoEncriptacion,
                    claveEncriptada.length(),
                    obtenerEstadoSeguridad(tipoEncriptacion),
                    obtenerRecomendacion(tipoEncriptacion)
                );
                
            } else {
                return "❌ Usuario no encontrado: " + username;
            }
            
        } catch (Exception e) {
            return "❌ Error en test: " + e.getMessage();
        }
    }
    
    /**
     * 🔐 Test de encriptación manual SHA-256
     */
    @PostMapping("/encrypt-password")
    public String encryptPassword(@RequestParam String password) {
        try {
            String encrypted = passwordService.encriptarClave(password);
            String tipoEncriptacion = passwordService.obtenerTipoEncriptacion(encrypted);
            boolean verificacion = passwordService.verificarClave(password, encrypted);
            
            return String.format("""
                🔐 TEST DE ENCRIPTACIÓN SHA-256
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                📝 Contraseña Original: %s
                🔒 Contraseña Encriptada: %s
                🔍 Tipo: %s
                ✅ Verificación: %s
                📊 Longitud original: %d caracteres
                📊 Longitud encriptada: %d caracteres
                🔐 Formato: Salt + Hash SHA-256
                🛡️ Seguridad: ALTA
                """, 
                password,
                encrypted,
                tipoEncriptacion,
                verificacion ? "CORRECTO ✅" : "ERROR ❌",
                password.length(),
                encrypted.length()
            );
            
        } catch (Exception e) {
            return "❌ Error en encriptación: " + e.getMessage();
        }
    }
    
    /**
     * 🔐 Verificar tipo de encriptación de un usuario
     */
    @GetMapping("/check-encryption/{username}")
    public String checkEncryption(@PathVariable String username) {
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findByUserName(username);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String clave = usuario.getClave();
                String tipoEncriptacion = passwordService.obtenerTipoEncriptacion(clave);
                boolean estaEncriptada = passwordService.estaEncriptada(clave);
                boolean esSHA256 = passwordService.estaEncriptadaSHA256(clave);
                boolean esBase64 = passwordService.estaEncriptadaBase64(clave);
                
                return String.format("""
                    🔍 VERIFICACIÓN DE ENCRIPTACIÓN
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    👤 Usuario: %s
                    🔑 Contraseña: %s...
                    🔐 Tipo: %s
                    📊 ¿Está encriptada?: %s
                    🛡️ ¿Es SHA-256?: %s
                    📦 ¿Es Base64?: %s
                    🔒 ¿Es texto plano?: %s
                    📈 Nivel de seguridad: %s
                    💡 Recomendación: %s
                    """, 
                    username,
                    clave.substring(0, Math.min(15, clave.length())),
                    tipoEncriptacion,
                    estaEncriptada ? "SÍ ✅" : "NO ❌",
                    esSHA256 ? "SÍ ✅" : "NO ❌",
                    esBase64 ? "SÍ ⚠️" : "NO ❌",
                    (!estaEncriptada) ? "SÍ ⚠️" : "NO ✅",
                    obtenerNivelSeguridad(tipoEncriptacion),
                    obtenerRecomendacion(tipoEncriptacion)
                );
                
            } else {
                return "❌ Usuario no encontrado: " + username;
            }
            
        } catch (Exception e) {
            return "❌ Error en verificación: " + e.getMessage();
        }
    }
    
    /**
     * 🔐 Comparar contraseña de login con BD (SHA-256)
     */
    @PostMapping("/verify-login")
    public String verifyLogin(@RequestParam String username, @RequestParam String password) {
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findByUserName(username);
            
            if (usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                String claveDB = usuario.getClave();
                String tipoEncriptacion = passwordService.obtenerTipoEncriptacion(claveDB);
                boolean coincide = passwordService.verificarClave(password, claveDB);
                
                return String.format("""
                    🔐 VERIFICACIÓN DE LOGIN
                    ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                    👤 Usuario: %s
                    🔑 Contraseña ingresada: %s
                    🔒 Contraseña en BD: %s...
                    🔍 Tipo de encriptación: %s
                    ✅ ¿Coincide?: %s
                    📊 Método de verificación: %s
                    🛡️ Nivel de seguridad: %s
                    ⚠️ Requiere migración: %s
                    """, 
                    username,
                    password,
                    claveDB.substring(0, Math.min(20, claveDB.length())),
                    tipoEncriptacion,
                    coincide ? "SÍ ✅" : "NO ❌",
                    obtenerMetodoVerificacion(tipoEncriptacion),
                    obtenerNivelSeguridad(tipoEncriptacion),
                    requiereMigracion(tipoEncriptacion) ? "SÍ ⚠️" : "NO ✅"
                );
                
            } else {
                return "❌ Usuario no encontrado: " + username;
            }
            
        } catch (Exception e) {
            return "❌ Error en verificación: " + e.getMessage();
        }
    }
    
    /**
     * 📊 Estadísticas de encriptación del sistema (SHA-256)
     */
    @GetMapping("/encryption-stats")
    public String encryptionStats() {
        try {
            List<Usuario> todosUsuarios = usuarioDao.findAll();
            int totalUsuarios = todosUsuarios.size();
            int sha256 = 0;
            int base64 = 0;
            int textoPlano = 0;
            
            for (Usuario usuario : todosUsuarios) {
                String tipo = passwordService.obtenerTipoEncriptacion(usuario.getClave());
                switch (tipo) {
                    case "SHA-256":
                        sha256++;
                        break;
                    case "Base64":
                        base64++;
                        break;
                    case "Texto plano":
                        textoPlano++;
                        break;
                }
            }
            
            double porcentajeSHA256 = totalUsuarios > 0 ? (sha256 * 100.0 / totalUsuarios) : 0;
            double porcentajeSeguro = totalUsuarios > 0 ? ((sha256 + base64) * 100.0 / totalUsuarios) : 0;
            
            return String.format("""
                📊 ESTADÍSTICAS DE ENCRIPTACIÓN DEL SISTEMA
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                👥 Total de usuarios: %d
                
                🔐 POR TIPO DE ENCRIPTACIÓN:
                🛡️ SHA-256 (Recomendado): %d (%.1f%%)
                📦 Base64 (Medio): %d (%.1f%%)
                📝 Texto plano (Inseguro): %d (%.1f%%)
                
                📈 MÉTRICAS DE SEGURIDAD:
                ✅ Usuarios seguros: %d (%.1f%%)
                🎯 Usuarios con SHA-256: %d (%.1f%%)
                ⚠️ Requieren migración: %d (%.1f%%)
                
                📊 ESTADO DEL SISTEMA:
                %s
                
                💡 RECOMENDACIÓN:
                %s
                """, 
                totalUsuarios,
                sha256, porcentajeSHA256,
                base64, (base64 * 100.0 / Math.max(totalUsuarios, 1)),
                textoPlano, (textoPlano * 100.0 / Math.max(totalUsuarios, 1)),
                (sha256 + base64), porcentajeSeguro,
                sha256, porcentajeSHA256,
                (base64 + textoPlano), ((base64 + textoPlano) * 100.0 / Math.max(totalUsuarios, 1)),
                obtenerEstadoSistema(porcentajeSHA256, porcentajeSeguro),
                obtenerRecomendacionSistema(sha256, base64, textoPlano)
            );
            
        } catch (Exception e) {
            return "❌ Error obteniendo estadísticas: " + e.getMessage();
        }
    }
    
    /**
     * 🔧 Test básico de funcionamiento SHA-256
     */
    @GetMapping("/test-encryption")
    public String testEncryption() {
        try {
            String testPassword = "123456";
            String encrypted = passwordService.encriptarClave(testPassword);
            String tipoEncriptacion = passwordService.obtenerTipoEncriptacion(encrypted);
            boolean verification = passwordService.verificarClave(testPassword, encrypted);
            
            return String.format("""
                🧪 TEST BÁSICO DE ENCRIPTACIÓN SHA-256
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                📝 Contraseña de prueba: %s
                🔒 Encriptada: %s...
                🔍 Tipo: %s
                ✅ Verificación: %s
                🛡️ Seguridad: ALTA (SHA-256 + Salt)
                🔍 Componentes: Salt aleatorio + Hash SHA-256
                🔧 Sistema: %s
                """, 
                testPassword,
                encrypted.substring(0, Math.min(40, encrypted.length())),
                tipoEncriptacion,
                verification ? "EXITOSA ✅" : "FALLIDA ❌",
                verification ? "FUNCIONANDO CORRECTAMENTE" : "ERROR EN EL SISTEMA"
            );
            
        } catch (Exception e) {
            return "❌ Error en test básico: " + e.getMessage();
        }
    }
    
    /**
     * 🔄 Test de migración de Base64 a SHA-256
     */
    @PostMapping("/test-migration")
    public String testMigration(@RequestParam String password) {
        try {
            // Simular contraseña en Base64
            String base64Password = java.util.Base64.getEncoder().encodeToString(password.getBytes());
            
            // Migrar a SHA-256
            String sha256Password = passwordService.encriptarClave(password);
            
            // Verificar ambos formatos
            boolean verificacionBase64 = passwordService.verificarClave(password, base64Password);
            boolean verificacionSHA256 = passwordService.verificarClave(password, sha256Password);
            
            return String.format("""
                🔄 TEST DE MIGRACIÓN BASE64 → SHA-256
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                📝 Contraseña original: %s
                
                📦 FORMATO BASE64:
                🔒 Encriptada: %s
                ✅ Verificación: %s
                🔍 Tipo: %s
                
                🛡️ FORMATO SHA-256:
                🔒 Encriptada: %s...
                ✅ Verificación: %s
                🔍 Tipo: %s
                
                📊 COMPARACIÓN:
                🔧 Migración: %s
                🛡️ Mejora de seguridad: %s
                """, 
                password,
                base64Password,
                verificacionBase64 ? "EXITOSA ✅" : "FALLIDA ❌",
                passwordService.obtenerTipoEncriptacion(base64Password),
                sha256Password.substring(0, Math.min(40, sha256Password.length())),
                verificacionSHA256 ? "EXITOSA ✅" : "FALLIDA ❌",
                passwordService.obtenerTipoEncriptacion(sha256Password),
                (verificacionBase64 && verificacionSHA256) ? "EXITOSA ✅" : "CON ERRORES ❌",
                "Base64 → SHA-256 + Salt (Significativa)"
            );
            
        } catch (Exception e) {
            return "❌ Error en test de migración: " + e.getMessage();
        }
    }
    
    // ========== MÉTODOS AUXILIARES ==========
    
    private String obtenerEstadoSeguridad(String tipoEncriptacion) {
        return switch (tipoEncriptacion) {
            case "SHA-256" -> "SEGURO ✅";
            case "Base64" -> "MEDIO ⚠️";
            case "Texto plano" -> "INSEGURO ❌";
            default -> "DESCONOCIDO ❓";
        };
    }
    
    private String obtenerNivelSeguridad(String tipoEncriptacion) {
        return switch (tipoEncriptacion) {
            case "SHA-256" -> "ALTO";
            case "Base64" -> "MEDIO";
            case "Texto plano" -> "BAJO";
            default -> "DESCONOCIDO";
        };
    }
    
    private String obtenerRecomendacion(String tipoEncriptacion) {
        return switch (tipoEncriptacion) {
            case "SHA-256" -> "Usuario ya tiene máxima seguridad";
            case "Base64" -> "Migrar a SHA-256 en próximo login";
            case "Texto plano" -> "Migración urgente requerida";
            default -> "Revisar formato de contraseña";
        };
    }
    
        private String obtenerMetodoVerificacion(String tipoEncriptacion) {
            return switch (tipoEncriptacion) {
                case "SHA-256" -> "Hash SHA-256 con salt";
                case "Base64" -> "Base64 simple";
                case "Texto plano" -> "Comparación directa";
                default -> "Desconocido";
            };
        }
    
        // Método auxiliar agregado para solucionar el error de compilación
        private String obtenerEstadoSistema(double porcentajeSHA256, double porcentajeSeguro) {
            if (porcentajeSHA256 >= 90.0) {
                return "ÓPTIMO ✅ - La mayoría de los usuarios tienen contraseñas seguras (SHA-256)";
            } else if (porcentajeSeguro >= 90.0) {
                return "ACEPTABLE ⚠️ - Algunos usuarios requieren migración a SHA-256";
            } else {
                return "INSEGURO ❌ - Se recomienda migrar urgentemente a SHA-256";
            }
        }

        // Método auxiliar para recomendación general del sistema
        private String obtenerRecomendacionSistema(int sha256, int base64, int textoPlano) {
            if (textoPlano > 0) {
                return "Migrar urgentemente todas las contraseñas en texto plano a SHA-256.";
            } else if (base64 > 0) {
                return "Migrar contraseñas en Base64 a SHA-256 progresivamente.";
            } else if (sha256 > 0) {
                return "El sistema está correctamente protegido con SHA-256.";
            } else {
                return "No hay usuarios registrados o no se detectaron contraseñas.";
            }
        }

        // Método auxiliar para determinar si requiere migración
        private boolean requiereMigracion(String tipoEncriptacion) {
            return !"SHA-256".equals(tipoEncriptacion);
        }
    }