package com.example.demo.models.servicio;

import org.springframework.stereotype.Service;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Servicio para encriptación y verificación de contraseñas usando SHA-256 con salt
 */
@Service
public class PasswordService {
    
    private static final String ALGORITHM = "SHA-256";
    private static final int SALT_LENGTH = 16; // 16 bytes = 128 bits
    private static final String SEPARATOR = ":"; // Separador entre salt y hash
    
    /**
     * Encripta una contraseña usando SHA-256 con salt aleatorio
     * @param claveTextoPlano La contraseña en texto plano
     * @return La contraseña encriptada en formato "salt:hash" (Base64)
     */
    public String encriptarClave(String claveTextoPlano) {
        if (claveTextoPlano == null || claveTextoPlano.isEmpty()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía");
        }
        
        try {
            // Generar salt aleatorio
            byte[] salt = generarSalt();
            
            // Generar hash SHA-256 con salt
            String hash = generarHash(claveTextoPlano, salt);
            
            // Combinar salt y hash en formato Base64
            String saltBase64 = Base64.getEncoder().encodeToString(salt);
            String resultado = saltBase64 + SEPARATOR + hash;
            
            System.out.println("🔐 Contraseña encriptada exitosamente con SHA-256");
            return resultado;
            
        } catch (Exception e) {
            System.out.println("❌ Error al encriptar contraseña: " + e.getMessage());
            throw new RuntimeException("Error en la encriptación SHA-256", e);
        }
    }
    
    /**
     * Verifica si una contraseña en texto plano coincide con la encriptada
     * @param claveTextoPlano La contraseña en texto plano
     * @param claveEncriptada La contraseña encriptada en formato "salt:hash"
     * @return true si coinciden, false en caso contrario
     */
    public boolean verificarClave(String claveTextoPlano, String claveEncriptada) {
        if (claveTextoPlano == null || claveEncriptada == null) {
            System.out.println("❌ Parámetros nulos en verificación");
            return false;
        }
        
        try {
            // Verificar si es formato SHA-256 con salt
            if (estaEncriptadaSHA256(claveEncriptada)) {
                return verificarSHA256(claveTextoPlano, claveEncriptada);
            }
            
            // Verificar si es formato Base64 (compatibilidad hacia atrás)
            if (estaEncriptadaBase64(claveEncriptada)) {
                return verificarBase64(claveTextoPlano, claveEncriptada);
            }
            
            // Si no está encriptada, comparación directa (usuarios muy antiguos)
            boolean coincide = claveTextoPlano.equals(claveEncriptada);
            if (coincide) {
                System.out.println("✅ Contraseña verificada (texto plano - requiere migración)");
            } else {
                System.out.println("❌ Contraseña no coincide");
            }
            return coincide;
            
        } catch (Exception e) {
            System.out.println("❌ Error al verificar contraseña: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica contraseña en formato SHA-256
     */
    private boolean verificarSHA256(String claveTextoPlano, String claveEncriptada) {
        try {
            // Separar salt y hash
            String[] partes = claveEncriptada.split(SEPARATOR, 2);
            if (partes.length != 2) {
                System.out.println("❌ Formato SHA-256 inválido");
                return false;
            }
            
            String saltBase64 = partes[0];
            String hashAlmacenado = partes[1];
            
            // Decodificar salt
            byte[] salt = Base64.getDecoder().decode(saltBase64);
            
            // Generar hash con el mismo salt
            String hashGenerado = generarHash(claveTextoPlano, salt);
            
            // Comparar hashes
            boolean coincide = hashAlmacenado.equals(hashGenerado);
            if (coincide) {
                System.out.println("✅ Contraseña verificada correctamente (SHA-256)");
            } else {
                System.out.println("❌ Contraseña no coincide (SHA-256)");
            }
            
            return coincide;
            
        } catch (Exception e) {
            System.out.println("❌ Error en verificación SHA-256: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica contraseña en formato Base64 (compatibilidad)
     */
    private boolean verificarBase64(String claveTextoPlano, String claveEncriptada) {
        try {
            byte[] claveBytes = Base64.getDecoder().decode(claveEncriptada);
            String claveDesencriptada = new String(claveBytes, StandardCharsets.UTF_8);
            
            boolean coincide = claveTextoPlano.equals(claveDesencriptada);
            if (coincide) {
                System.out.println("✅ Contraseña verificada (Base64 - requiere migración a SHA-256)");
            } else {
                System.out.println("❌ Contraseña no coincide (Base64)");
            }
            
            return coincide;
        } catch (Exception e) {
            System.out.println("❌ Error en verificación Base64: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica si una contraseña está encriptada en formato SHA-256
     * @param clave La contraseña a verificar
     * @return true si está en formato SHA-256, false en caso contrario
     */
    public boolean estaEncriptadaSHA256(String clave) {
        if (clave == null || clave.isEmpty()) {
            return false;
        }
        
        try {
            // Verificar formato "salt:hash"
            String[] partes = clave.split(SEPARATOR, 2);
            if (partes.length != 2) {
                return false;
            }
            
            String saltBase64 = partes[0];
            String hash = partes[1];
            
            // Verificar que el salt sea Base64 válido
            Base64.getDecoder().decode(saltBase64);
            
            // Verificar que el hash tenga la longitud correcta para SHA-256 en Base64
            // SHA-256 produce 32 bytes -> Base64 produce ~44 caracteres
            if (hash.length() >= 40 && hash.length() <= 48) {
                Base64.getDecoder().decode(hash);
                return true;
            }
            
            return false;
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifica si una contraseña está encriptada en formato Base64 (compatibilidad)
     * @param clave La contraseña a verificar
     * @return true si está en formato Base64, false en caso contrario
     */
    public boolean estaEncriptadaBase64(String clave) {
        if (clave == null || clave.isEmpty()) {
            return false;
        }
        
        try {
            // No debe contener el separador SHA-256
            if (clave.contains(SEPARATOR)) {
                return false;
            }
            
            // Intentar decodificar Base64
            Base64.getDecoder().decode(clave);
            
            // Verificar patrón Base64 básico
            if (clave.matches("^[A-Za-z0-9+/]*={0,2}$")) {
                return true;
            }
            
            return false;
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Verifica si una contraseña está encriptada (cualquier formato)
     * @param clave La contraseña a verificar
     * @return true si está encriptada, false si es texto plano
     */
    public boolean estaEncriptada(String clave) {
        return estaEncriptadaSHA256(clave) || estaEncriptadaBase64(clave);
    }
    
    /**
     * Obtiene el tipo de encriptación de una contraseña
     * @param clave La contraseña a analizar
     * @return String indicando el tipo: "SHA-256", "Base64", "Texto plano"
     */
    public String obtenerTipoEncriptacion(String clave) {
        if (estaEncriptadaSHA256(clave)) {
            return "SHA-256";
        } else if (estaEncriptadaBase64(clave)) {
            return "Base64";
        } else {
            return "Texto plano";
        }
    }
    
    /**
     * Migra una contraseña de Base64 a SHA-256
     * @param claveTextoPlano La contraseña en texto plano
     * @return La contraseña encriptada en SHA-256
     */
    public String migrarABase64ASha256(String claveTextoPlano) {
        System.out.println("🔄 Migrando contraseña de Base64 a SHA-256");
        return encriptarClave(claveTextoPlano);
    }
    
    /**
     * Genera un salt aleatorio
     * @return Array de bytes con el salt
     */
    private byte[] generarSalt() {
        SecureRandom random = new SecureRandom();
        byte[] salt = new byte[SALT_LENGTH];
        random.nextBytes(salt);
        return salt;
    }
    
    /**
     * Genera hash SHA-256 con salt
     * @param claveTextoPlano La contraseña en texto plano
     * @param salt El salt a usar
     * @return El hash en Base64
     */
    private String generarHash(String claveTextoPlano, byte[] salt) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance(ALGORITHM);
        
        // Agregar salt
        digest.update(salt);
        
        // Generar hash
        byte[] hashBytes = digest.digest(claveTextoPlano.getBytes(StandardCharsets.UTF_8));
        
        // Convertir a Base64
        return Base64.getEncoder().encodeToString(hashBytes);
    }
    
    /**
     * Método para testing - NO usar en producción
     * Desencripta una contraseña Base64 (solo para compatibilidad)
     */
    @Deprecated
    public String desencriptarClaveBase64(String claveEncriptada) {
        if (claveEncriptada == null || claveEncriptada.isEmpty()) {
            throw new IllegalArgumentException("La contraseña encriptada no puede estar vacía");
        }
        
        try {
            // Decodificar desde Base64
            byte[] claveBytes = Base64.getDecoder().decode(claveEncriptada);
            String claveTextoPlano = new String(claveBytes, StandardCharsets.UTF_8);
            
            System.out.println("🔓 Contraseña Base64 desencriptada (método deprecated)");
            return claveTextoPlano;
            
        } catch (Exception e) {
            System.out.println("❌ Error al desencriptar contraseña Base64: " + e.getMessage());
            throw new RuntimeException("Error en la desencriptación Base64", e);
        }
    }
}