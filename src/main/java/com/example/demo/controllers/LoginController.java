package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.servicio.AutenticacionService;
import com.example.demo.models.servicio.PasswordService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private PasswordService passwordService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "/login";
    }

    @GetMapping("/registro")
    public String mostrarRegistro() {
        return "registro";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String user_name,
                                @RequestParam String clave,
                                Model model,
                                HttpSession session) {
        System.out.println("🔐 Procesando login para: " + user_name);
        
        try {
            // 🔐 PRIMERO: Intentar autenticación con contraseña encriptada
            Optional<Usuario> usuarioOpt = autenticacionService.autenticarConEncriptacion(user_name, clave);
            
            if (usuarioOpt.isPresent()) {
                // ✅ Login exitoso con contraseña encriptada
                Usuario usuario = usuarioOpt.get();
                return procesarLoginExitoso(usuario, model, session, user_name);
                
            } else {
                // 🔄 FALLBACK: Intentar autenticación tradicional para usuarios no migrados
                System.out.println("🔄 Probando autenticación tradicional para: " + user_name);
                Optional<Usuario> usuarioTradicionalOpt = autenticacionService.autenticar(user_name, clave);
                
                if (usuarioTradicionalOpt.isPresent()) {
                    Usuario usuario = usuarioTradicionalOpt.get();
                    
                    // 🔧 MIGRAR CONTRASEÑA A FORMATO ENCRIPTADO
                    System.out.println("🔧 Migrando contraseña a formato encriptado para: " + user_name);
                    try {
                        String claveEncriptada = passwordService.encriptarClave(clave);
                        usuario.setClave(claveEncriptada);
                        autenticacionService.actualizarUsuario(usuario);
                        System.out.println("✅ Contraseña migrada exitosamente para: " + user_name);
                    } catch (Exception e) {
                        System.out.println("⚠️ Error al migrar contraseña para " + user_name + ": " + e.getMessage());
                    }
                    
                    return procesarLoginExitoso(usuario, model, session, user_name);
                    
                } else {
                    System.out.println("❌ Login fallido para: " + user_name);
                    model.addAttribute("error", "Usuario o contraseña incorrectos");
                    return "login";
                }
            }
        } catch (Exception e) {
            System.out.println("❌ Error en autenticación para " + user_name + ": " + e.getMessage());
            model.addAttribute("error", "Error en el proceso de autenticación");
            return "login";
        }
    }
    private Collection<? extends GrantedAuthority> mapearRoles(Usuario usuario) {
        if (usuario.getRol() != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
        } else {
            return List.of(); // Sin rol asignado
        }
    }
    
    /**
     * 🔧 Método auxiliar para procesar login exitoso
     */
    private String procesarLoginExitoso(Usuario usuario, Model model, HttpSession session, String user_name) {
        String token = autenticacionService.generarTokenParaUsuario(usuario);
        
        // 🔥 GUARDAR EN SESIÓN
        session.setAttribute("userName", usuario.getUser_name());
        session.setAttribute("userToken", token);
        session.setAttribute("userId", usuario.getIdUsuario());
        
        model.addAttribute("usuario", usuario);
        model.addAttribute("token", token);

        // ✅ AUTENTICACIÓN SPRING SECURITY
        var authorities = mapearRoles(usuario);
        var authToken = new UsernamePasswordAuthenticationToken(
            usuario.getUser_name(), null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(authToken);
        SecurityContextImpl securityContext = new SecurityContextImpl(authToken);
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, securityContext);
        
        // ✅ REDIRECCIÓN
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());

        if (isAdmin) {
            System.out.println("👑 Login exitoso para ADMINISTRADOR: " + user_name);
            return "redirect:/dashboard";
        } else {
            System.out.println("✅ Login exitoso para USUARIO: " + user_name);
            return "redirect:/control";
        }
    }


    



    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String user_name,
                                   @RequestParam String clave,
                                   @RequestParam String confirmarClave,
                                   Model model,
                                   HttpSession session) {
        
        System.out.println("📝 Procesando registro para: " + user_name);
        
        // Validaciones básicas
        if (user_name == null || user_name.trim().isEmpty()) {
            model.addAttribute("error", "El nombre de usuario es obligatorio");
            return "registro";
        }
        
        if (clave == null || clave.length() < 6) {
            model.addAttribute("error", "La contraseña debe tener al menos 6 caracteres");
            return "registro";
        }
        
        if (!clave.equals(confirmarClave)) {
            model.addAttribute("error", "Las contraseñas no coinciden");
            return "registro";
        }
        
        if (autenticacionService.existeUsuario(user_name)) {
            model.addAttribute("error", "El usuario ya existe");
            return "registro";
        }
        
        try {
            System.out.println("💾 Intentando registrar usuario básico: " + user_name);
            
            // 🔐 ENCRIPTAR LA CONTRASEÑA ANTES DE GUARDAR
            String claveEncriptada = passwordService.encriptarClave(clave);
            System.out.println("🔒 Contraseña encriptada para usuario: " + user_name);
            
            // Registrar usuario con contraseña encriptada
            Usuario nuevoUsuario = autenticacionService.registrarUsuario(user_name, claveEncriptada);
            
            // 🔥 CREAR SESIÓN AUTOMÁTICAMENTE DESPUÉS DEL REGISTRO
            session.setAttribute("userName", nuevoUsuario.getUser_name());
            session.setAttribute("userToken", nuevoUsuario.getToken());
            session.setAttribute("userId", nuevoUsuario.getIdUsuario());
            
            model.addAttribute("usuario", nuevoUsuario);
            model.addAttribute("token", nuevoUsuario.getToken());
            model.addAttribute("mensaje", "¡Registro exitoso! Bienvenido " + user_name);
            
            System.out.println("✅ Registro exitoso para: " + user_name + " con ID: " + nuevoUsuario.getIdUsuario());
            
            // 🚀 Los usuarios recién registrados van al control ESP32 (son usuarios regulares por defecto)
            return "redirect:/control";
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/validar")
    public String validarToken(@RequestParam String token, Model model, HttpSession session) {
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // Crear sesión si no existe
            session.setAttribute("userName", usuario.getUser_name());
            session.setAttribute("userToken", token);
            session.setAttribute("userId", usuario.getIdUsuario());
            
            model.addAttribute("mensaje", "Token válido. Bienvenido " + usuario.getUser_name());
            model.addAttribute("usuario", usuario);
            
            // Verificar rol y redirigir apropiadamente
            boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
            
            if (isAdmin) {
                System.out.println("👑 Token válido para administrador: " + usuario.getUser_name());
                return "redirect:/dashboard";
            } else {
                System.out.println("✅ Token válido para usuario: " + usuario.getUser_name());
                return "redirect:/control";
            }
        } else {
            model.addAttribute("error", "Token inválido o expirado");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String cerrarSesion(@RequestParam String user_name, Model model, HttpSession session) {
        System.out.println("🚪 Cerrando sesión para: " + user_name);
        autenticacionService.cerrarSesion(user_name);
        session.invalidate(); // Limpiar sesión completa
        model.addAttribute("mensaje", "Sesión cerrada correctamente");
        return "login";
    }

    /**
     * Endpoint especial para cambiar entre vistas (útil para testing)
     */
    @GetMapping("/switch-view")
    public String switchView(@RequestParam(defaultValue = "auto") String view, 
                           HttpSession session, Model model) {
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
        
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        switch (view.toLowerCase()) {
            case "admin":
                if (isAdmin) {
                    System.out.println("🔄 Cambiando a vista administrativa para: " + userName);
                    return "redirect:/dashboard";
                } else {
                    model.addAttribute("error", "Acceso denegado - Se requieren permisos de administrador");
                    return "redirect:/control";
                }
            
            case "control":
                System.out.println("🔄 Cambiando a vista de control ESP32 para: " + userName);
                return "redirect:/control";
            
            case "dashboard":
                System.out.println("🔄 Cambiando a dashboard regular para: " + userName);
                return "dashboard";
            
            case "auto":
            default:
                // Comportamiento automático basado en el rol
                if (isAdmin) {
                    return "redirect:/dashboard";
                } else {
                    return "redirect:/control";
                }
        }
    }

    /**
     * Método auxiliar para debugging - mostrar información de sesión
     */
    @GetMapping("/session-info")
    @ResponseBody
    public String sessionInfo(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        Long userId = (Long) session.getAttribute("userId");
        
        if (userName == null) {
            return "❌ No hay sesión activa";
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido para usuario: " + userName;
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        // 🔐 MOSTRAR INFORMACIÓN DE ENCRIPTACIÓN EN DEBUG
        String passwordInfo = "No disponible";
        try {
            String claveEncriptada = usuario.getClave();
            if (claveEncriptada != null) {
                try {
                    String claveDesencriptada = passwordService.desencriptarClaveBase64(claveEncriptada);
                    passwordInfo = String.format("Encriptada: %s... | Desencriptada: %s", 
                        claveEncriptada.substring(0, Math.min(12, claveEncriptada.length())), 
                        claveDesencriptada);
                } catch (Exception e) {
                    passwordInfo = "Formato tradicional (sin encriptar): " + claveEncriptada;
                }
            }
        } catch (Exception e) {
            passwordInfo = "Error al analizar contraseña";
        }
        
        return String.format("""
            📊 INFORMACIÓN DE SESIÓN:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            👤 Usuario: %s
            🆔 ID: %d
            🎭 Rol: %s
            👑 Es Admin: %s
            🔑 Token: %s...
            🔐 Contraseña: %s
            📅 Fecha Registro: %s
            🔄 Última Modificación: %s
            📊 Estado: %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            🌐 Session ID: %s
            """, 
            userName,
            userId,
            usuario.getRol() != null ? usuario.getRol().getNombre() : "SIN ROL",
            isAdmin ? "SÍ" : "NO",
            userToken != null ? userToken.substring(0, 8) : "NULL",
            passwordInfo,
            usuario.getFechaRegistro(),
            usuario.getFechaModificacion(),
            usuario.getEstado(),
            session.getId()
        );
    }
    
    /**
     * 🔧 ENDPOINT PARA MIGRAR CONTRASEÑAS DE USUARIOS EXISTENTES
     */
    @PostMapping("/admin/migrate-passwords")
    @ResponseBody
    public String migratePasswords(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return "❌ Acceso denegado - No hay sesión activa";
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido";
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            return "❌ Acceso denegado - Se requieren permisos de administrador";
        }
        
        try {
            StringBuilder resultado = new StringBuilder();
            resultado.append("🔧 PROCESO DE MIGRACIÓN DE CONTRASEÑAS\n");
            resultado.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            
            var todosLosUsuarios = autenticacionService.obtenerTodosLosUsuarios();
            int migrados = 0;
            int yaEncriptados = 0;
            int errores = 0;
            
            for (Usuario usr : todosLosUsuarios) {
                try {
                    String claveActual = usr.getClave();
                    
                    // Verificar si ya está encriptada
                    if (passwordService.estaEncriptada(claveActual)) {
                        yaEncriptados++;
                        resultado.append(String.format("✅ %s: Ya encriptada\n", usr.getUser_name()));
                    } else {
                        // No está encriptada, migrar
                        String claveEncriptada = passwordService.encriptarClave(claveActual);
                        usr.setClave(claveEncriptada);
                        autenticacionService.actualizarUsuario(usr);
                        migrados++;
                        resultado.append(String.format("🔄 %s: Migrada exitosamente\n", usr.getUser_name()));
                    }
                    
                } catch (Exception e) {
                    errores++;
                    resultado.append(String.format("❌ %s: Error - %s\n", usr.getUser_name(), e.getMessage()));
                }
            }
            
            resultado.append("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");
            resultado.append(String.format("📊 RESUMEN:\n"));
            resultado.append(String.format("✅ Migradas: %d\n", migrados));
            resultado.append(String.format("🔐 Ya encriptadas: %d\n", yaEncriptados));
            resultado.append(String.format("❌ Errores: %d\n", errores));
            resultado.append(String.format("📋 Total procesadas: %d\n", todosLosUsuarios.size()));
            
            System.out.println("🔧 Migración completada por administrador: " + userName);
            
            return resultado.toString();
            
        } catch (Exception e) {
            return "❌ Error en migración: " + e.getMessage();
        }
    }
}