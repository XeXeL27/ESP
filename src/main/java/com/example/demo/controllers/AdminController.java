package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.servicio.AutenticacionService;
import com.example.demo.models.dao.UsuarioDao;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDate;

@Controller
public class AdminController {

    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private UsuarioDao usuarioDao;

    /**
     * Mostrar dashboard con lógica administrativa
     */
    @GetMapping("/dashboard")
    public String mostrarDashboard(Model model, HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return "redirect:/login";
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            session.invalidate();
            return "redirect:/login";
        }
        
        Usuario usuario = usuarioOpt.get();
        model.addAttribute("usuario", usuario);
        model.addAttribute("token", userToken);
        
        // Verificar si es administrador
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        model.addAttribute("isAdmin", isAdmin);
        
        System.out.println("✅ Dashboard cargado para: " + userName + " (Admin: " + isAdmin + ")");
        
        // Usar la nueva vista administrativa
        return "admin-dashboard";
    }

    /**
     * API para obtener todos los usuarios (solo administradores)
     */
    @GetMapping("/api/admin/users")
    @ResponseBody
    public ResponseEntity<?> obtenerTodosLosUsuarios(HttpSession session) {
        // Verificar autenticación y permisos de administrador
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado - Se requieren permisos de administrador");
        }
        
        try {
            List<Usuario> usuarios = usuarioDao.findAll();
            System.out.println("📊 Admin consultó lista de usuarios: " + usuarios.size() + " usuarios encontrados");
            
            return ResponseEntity.ok(usuarios);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo usuarios: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener lista de usuarios");
        }
    }

    /**
     * API para obtener estadísticas del sistema (solo administradores)
     */
    @GetMapping("/api/admin/stats")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas(HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            long totalUsuarios = usuarioDao.count();
            long usuariosActivos = usuarioDao.countByEstado("ACTIVO");
            long usuariosConToken = usuarioDao.countByTokenIsNotNull();
            
            Map<String, Object> stats = new HashMap<>();
            stats.put("totalUsuarios", totalUsuarios);
            stats.put("usuariosActivos", usuariosActivos);
            stats.put("usuariosConectados", usuariosConToken);
            stats.put("fechaConsulta", LocalDate.now());
            
            System.out.println("📈 Estadísticas consultadas - Total: " + totalUsuarios + 
                             ", Activos: " + usuariosActivos + ", Con token: " + usuariosConToken);
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener estadísticas");
        }
    }

    /**
     * API para obtener detalles de un usuario específico (solo administradores)
     */
    @GetMapping("/api/admin/users/{userId}")
    @ResponseBody
    public ResponseEntity<?> obtenerDetallesUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            System.out.println("👤 Admin consultó detalles del usuario: " + usuario.getUser_name());
            
            return ResponseEntity.ok(usuario);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo detalles del usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener detalles del usuario");
        }
    }

    /**
     * API para regenerar token de un usuario (solo administradores)
     */
    @PostMapping("/api/admin/users/{userId}/regenerate-token")
    @ResponseBody
    public ResponseEntity<?> regenerarTokenUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // Regenerar token
            String nuevoToken = autenticacionService.generarTokenParaUsuario(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Token regenerado exitosamente");
            response.put("nuevoToken", nuevoToken);
            response.put("usuario", usuario.getUser_name());
            response.put("fechaRegeneracion", LocalDate.now());
            
            System.out.println("🔄 Admin " + adminUser + " regeneró token para: " + usuario.getUser_name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error regenerando token: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al regenerar token");
        }
    }

    /**
     * API para desactivar un usuario (solo administradores)
     */
    @PostMapping("/api/admin/users/{userId}/deactivate")
    @ResponseBody
    public ResponseEntity<?> desactivarUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // No permitir que el admin se desactive a sí mismo
            if (usuario.getUser_name().equals(adminUser)) {
                return ResponseEntity.badRequest()
                    .body("❌ No puedes desactivar tu propia cuenta");
            }
            
            // Desactivar usuario y limpiar token
            usuario.setEstado("INACTIVO");
            usuario.setToken(null);
            usuario.setFechaModificacion(LocalDate.now());
            usuarioDao.save(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Usuario desactivado exitosamente");
            response.put("usuario", usuario.getUser_name());
            response.put("fechaDesactivacion", LocalDate.now());
            
            System.out.println("🚫 Admin " + adminUser + " desactivó usuario: " + usuario.getUser_name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error desactivando usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al desactivar usuario");
        }
    }

    /**
     * API para activar un usuario (solo administradores)
     */
    @PostMapping("/api/admin/users/{userId}/activate")
    @ResponseBody
    public ResponseEntity<?> activarUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // Activar usuario
            usuario.setEstado("ACTIVO");
            usuario.setFechaModificacion(LocalDate.now());
            usuarioDao.save(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Usuario activado exitosamente");
            response.put("usuario", usuario.getUser_name());
            response.put("fechaActivacion", LocalDate.now());
            
            System.out.println("✅ Admin " + adminUser + " activó usuario: " + usuario.getUser_name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error activando usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al activar usuario");
        }
    }

    /**
     * API para test de conexión de un usuario específico (solo administradores)
     */
    @PostMapping("/api/admin/users/{userId}/test-connection")
    @ResponseBody
    public ResponseEntity<?> testConexionUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // Simular test de conexión
            boolean isConnected = usuario.getToken() != null && "ACTIVO".equals(usuario.getEstado());
            
            Map<String, Object> response = new HashMap<>();
            response.put("usuario", usuario.getUser_name());
            response.put("conectado", isConnected);
            response.put("estado", usuario.getEstado());
            response.put("tieneToken", usuario.getToken() != null);
            response.put("fechaTest", LocalDate.now());
            
            if (isConnected) {
                response.put("mensaje", "✅ Usuario conectado y operativo");
            } else {
                response.put("mensaje", "⚠️ Usuario sin conexión activa");
            }
            
            System.out.println("🧪 Admin " + adminUser + " probó conexión de: " + usuario.getUser_name() + 
                             " (Resultado: " + (isConnected ? "CONECTADO" : "DESCONECTADO") + ")");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error en test de conexión: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al probar conexión");
        }
    }

    /**
     * API para buscar usuarios (solo administradores)
     */
    @GetMapping("/api/admin/users/search")
    @ResponseBody
    public ResponseEntity<?> buscarUsuarios(@RequestParam String query, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            List<Usuario> usuarios = usuarioDao.findByUserNameContainingIgnoreCase(query);
            
            System.out.println("🔍 Admin buscó usuarios con término: '" + query + "' - " + usuarios.size() + " resultados");
            
            return ResponseEntity.ok(usuarios);
            
        } catch (Exception e) {
            System.out.println("❌ Error en búsqueda de usuarios: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error en la búsqueda");
        }
    }

    /**
     * API para crear un nuevo usuario (solo administradores)
     */
    @PostMapping("/api/admin/users/create")
    @ResponseBody
    public ResponseEntity<?> crearUsuario(@RequestBody Map<String, String> userData, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            String userName = userData.get("user_name");
            String clave = userData.get("clave");
            String adminUser = (String) session.getAttribute("userName");
            
            if (userName == null || clave == null || userName.trim().isEmpty() || clave.length() < 6) {
                return ResponseEntity.badRequest()
                    .body("❌ Datos inválidos - Verificar usuario y contraseña");
            }
            
            if (autenticacionService.existeUsuario(userName)) {
                return ResponseEntity.badRequest()
                    .body("❌ El usuario ya existe");
            }
            
            // Crear usuario
            Usuario nuevoUsuario = autenticacionService.registrarUsuario(userName, clave);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Usuario creado exitosamente");
            response.put("usuario", nuevoUsuario);
            response.put("creadoPor", adminUser);
            response.put("fechaCreacion", LocalDate.now());
            
            System.out.println("👥 Admin " + adminUser + " creó nuevo usuario: " + userName);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error creando usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al crear usuario: " + e.getMessage());
        }
    }

    /**
     * API para obtener logs de actividad (solo administradores)
     */
    @GetMapping("/api/admin/activity-logs")
    @ResponseBody
    public ResponseEntity<?> obtenerLogsActividad(HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            // Aquí podrías implementar un sistema de logs más sofisticado
            // Por ahora, devolvemos logs simulados
            Map<String, Object> logs = new HashMap<>();
            logs.put("mensaje", "Sistema de logs en desarrollo");
            logs.put("ultimaActividad", LocalDate.now());
            logs.put("consultadoPor", session.getAttribute("userName"));
            
            return ResponseEntity.ok(logs);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo logs: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener logs");
        }
    }

    /**
     * Método auxiliar para verificar si el usuario actual es administrador autenticado
     */
    private boolean isAdminAuthenticated(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            System.out.println("❌ Acceso denegado - Sin sesión válida");
            return false;
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Acceso denegado - Token inválido para: " + userName);
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            System.out.println("❌ Acceso denegado - Usuario " + userName + " no es administrador");
        }
        
        return isAdmin;
    }

    /**
     * Endpoint para promover un usuario a administrador (solo super-admin)
     */
    @PostMapping("/api/admin/users/{userId}/promote")
    @ResponseBody
    public ResponseEntity<?> promoverUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // Buscar rol de administrador
            Optional<com.example.demo.models.entity.Rol> rolAdminOpt = 
                autenticacionService.obtenerRolPorNombre("ADMIN");
            
            if (rolAdminOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("❌ Rol de administrador no encontrado");
            }
            
            // Promover usuario
            usuario.setRol(rolAdminOpt.get());
            usuario.setFechaModificacion(LocalDate.now());
            usuarioDao.save(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Usuario promovido a administrador");
            response.put("usuario", usuario.getUser_name());
            response.put("nuevoRol", "ADMIN");
            response.put("promovido_por", adminUser);
            response.put("fecha", LocalDate.now());
            
            System.out.println("👑 Admin " + adminUser + " promovió a administrador: " + usuario.getUser_name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error promoviendo usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al promover usuario");
        }
    }

    /**
     * Endpoint para degradar un administrador a usuario regular
     */
    @PostMapping("/api/admin/users/{userId}/demote")
    @ResponseBody
    public ResponseEntity<?> degradarUsuario(@PathVariable Long userId, HttpSession session) {
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Optional<Usuario> usuarioOpt = usuarioDao.findById(userId);
            
            if (usuarioOpt.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            Usuario usuario = usuarioOpt.get();
            String adminUser = (String) session.getAttribute("userName");
            
            // No permitir que el admin se degrade a sí mismo
            if (usuario.getUser_name().equals(adminUser)) {
                return ResponseEntity.badRequest()
                    .body("❌ No puedes degradar tu propia cuenta");
            }
            
            // Buscar rol de usuario regular
            Optional<com.example.demo.models.entity.Rol> rolUsuarioOpt = 
                autenticacionService.obtenerRolPorNombre("USUARIO");
            
            if (rolUsuarioOpt.isEmpty()) {
                return ResponseEntity.badRequest()
                    .body("❌ Rol de usuario no encontrado");
            }
            
            // Degradar usuario
            usuario.setRol(rolUsuarioOpt.get());
            usuario.setFechaModificacion(LocalDate.now());
            usuarioDao.save(usuario);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Administrador degradado a usuario regular");
            response.put("usuario", usuario.getUser_name());
            response.put("nuevoRol", "USUARIO");
            response.put("degradado_por", adminUser);
            response.put("fecha", LocalDate.now());
            
            System.out.println("📉 Admin " + adminUser + " degradó a usuario regular: " + usuario.getUser_name());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error degradando usuario: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al degradar usuario");
        }
    }
}