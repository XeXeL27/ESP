package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.servicio.AutenticacionService;
import jakarta.servlet.http.HttpSession;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private AutenticacionService autenticacionService;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

    @GetMapping("/login")
    public String mostrarLogin() {
        return "login";
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
        
        Optional<Usuario> usuarioOpt = autenticacionService.autenticar(user_name, clave);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            String token = autenticacionService.generarTokenParaUsuario(usuario);
            
            // 🔥 GUARDAR EN SESIÓN PARA EL PANEL DE CONTROL
            session.setAttribute("userName", usuario.getUser_name());
            session.setAttribute("userToken", token);
            session.setAttribute("userId", usuario.getIdUsuario());
            
            model.addAttribute("usuario", usuario);
            model.addAttribute("token", token);
            
            // Verificar rol del usuario
            boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
            
            if (isAdmin) {
                System.out.println("👑 Login exitoso para ADMINISTRADOR: " + user_name + " - Sesión creada");
                System.out.println("🚀 Redirigiendo al PANEL DE ADMINISTRACIÓN");
                // Redirigir al dashboard administrativo
                return "redirect:/dashboard";
            } else {
                System.out.println("✅ Login exitoso para USUARIO: " + user_name + " - Sesión creada");
                System.out.println("🚀 Redirigiendo al PANEL DE CONTROL ESP32");
                // Redirigir al panel de control ESP32 para usuarios regulares
                return "redirect:/control";
            }
        } else {
            System.out.println("❌ Login fallido para: " + user_name);
            model.addAttribute("error", "Usuario o contraseña incorrectos");
            return "login";
        }
    }

   @PostMapping("/registro")
public String procesarRegistro(@RequestParam String user_name,
                               @RequestParam String clave,
                               @RequestParam String confirmarClave,
                               Model model,
                               HttpSession session) {
    
    System.out.println("📝 Procesando registro para: " + user_name);
    
    // Validaciones
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
        Usuario nuevoUsuario = autenticacionService.registrarUsuario(user_name, clave);
        
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
        
        return String.format("""
            📊 INFORMACIÓN DE SESIÓN:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━
            👤 Usuario: %s
            🆔 ID: %d
            🎭 Rol: %s
            👑 Es Admin: %s
            🔑 Token: %s...
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
            usuario.getFechaRegistro(),
            usuario.getFechaModificacion(),
            usuario.getEstado(),
            session.getId()
        );
    }
}