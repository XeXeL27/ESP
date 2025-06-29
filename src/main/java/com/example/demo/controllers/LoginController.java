
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
import com.example.demo.models.servicioImpl.SecurityService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Controller
public class LoginController {

    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private PasswordService passwordService;

    @Autowired
    private SecurityService securityService;

    @Autowired
    private HttpServletRequest request;

    @Autowired
    private HttpServletResponse response;

    @GetMapping("/")
    public String index() {
        return "redirect:/login";
    }

@GetMapping("/login")
public String mostrarLogin(HttpServletRequest request, Model model) {
    String clientIP = getClientIP(request);

    if (securityService.isBlocked(clientIP)) {
        long tiempoRestante = securityService.getRemainingLockoutTime(clientIP);
        model.addAttribute("error", 
            String.format("🚫 IP bloqueada por múltiples intentos fallidos. " +
            "Tiempo restante: %d minutos", tiempoRestante));
        model.addAttribute("isBlocked", true);
        model.addAttribute("tiempoRestante", tiempoRestante);
        System.out.println("🚨 Intento de acceso desde IP bloqueada: " + clientIP);
    } else {
        int intentosRestantes = securityService.getRemainingAttempts(clientIP);
        model.addAttribute("intentosRestantes", intentosRestantes);
        model.addAttribute("showWarning", intentosRestantes < 5);
        
        model.addAttribute("isBlocked", false);
    }

    return "login";
}


    @GetMapping("/registro")
    public String mostrarRegistro(HttpServletRequest request, Model model) {
        String clientIP = getClientIP(request);
        
        // 🔒 OPCIONAL: También verificar bloqueo en registro
        if (securityService.isBlocked(clientIP)) {
            long tiempoRestante = securityService.getRemainingLockoutTime(clientIP);
            model.addAttribute("error", 
                String.format("IP temporalmente bloqueada. Intente en %d minutos", tiempoRestante));
            model.addAttribute("isBlocked", true);
        }
        
        return "registro";
    }

    @PostMapping("/login")
    public String procesarLogin(@RequestParam String user_name,
                                @RequestParam String clave,
                                HttpServletRequest request,
                                Model model,
                                HttpSession session) {
        
        String clientIP = getClientIP(request);
        System.out.println("🔐 Procesando login para: " + user_name + " desde IP: " + clientIP);
        
        // 🔒 PASO 1: VERIFICAR SI LA IP ESTÁ BLOQUEADA
        if (securityService.isBlocked(clientIP)) {
            long tiempoRestante = securityService.getRemainingLockoutTime(clientIP);
            model.addAttribute("error", 
                String.format("🚫 IP bloqueada por intentos fallidos. " +
                "Intente nuevamente en %d minutos", tiempoRestante));
            model.addAttribute("isBlocked", true);
            model.addAttribute("tiempoRestante", tiempoRestante);
            
            System.out.println("🚨 Intento de login desde IP bloqueada: " + clientIP + 
                             " - Usuario: " + user_name);

            model.addAttribute("isBlocked", true);
            model.addAttribute("tiempoRestante", tiempoRestante);
            model.addAttribute("showWarning", false); // importante
            model.addAttribute("intentosRestantes", 0);

            return "login";
        }
        
        // 🔒 PASO 2: VALIDACIONES BÁSICAS
        if (user_name == null || user_name.trim().isEmpty() || 
            clave == null || clave.length() < 3) {
            
            securityService.recordFailedAttempt(clientIP);
            model.addAttribute("error", "Credenciales inválidas");
            model.addAttribute("intentosRestantes", securityService.getRemainingAttempts(clientIP));
            
            System.out.println("⚠️ Credenciales inválidas desde IP: " + clientIP);
            model.addAttribute("isBlocked", false);
            model.addAttribute("showWarning", true);
            model.addAttribute("intentosRestantes", securityService.getRemainingAttempts(clientIP));

            return "login";
        }
        
        try {
            // 🔒 PASO 3: INTENTAR AUTENTICACIÓN
            Optional<Usuario> usuarioOpt = null;
            boolean loginExitoso = false;
            
            // 🔐 PRIMERO: Intentar autenticación con contraseña encriptada
            usuarioOpt = autenticacionService.autenticarConEncriptacion(user_name, clave);
            
            if (usuarioOpt.isPresent()) {
                loginExitoso = true;
                System.out.println("✅ Autenticación exitosa con contraseña encriptada para: " + user_name);
            } else {
                // 🔄 FALLBACK: Intentar autenticación tradicional para usuarios no migrados
                System.out.println("🔄 Probando autenticación tradicional para: " + user_name);
                usuarioOpt = autenticacionService.autenticar(user_name, clave);
                
                if (usuarioOpt.isPresent()) {
                    loginExitoso = true;
                    Usuario usuario = usuarioOpt.get();
                    
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
                }
            }
            
            if (loginExitoso && usuarioOpt.isPresent()) {
                Usuario usuario = usuarioOpt.get();
                
                // 🔒 VERIFICAR ESTADO DEL USUARIO
                if (!"ACTIVO".equals(usuario.getEstado())) {
                    securityService.recordFailedAttempt(clientIP);
                    model.addAttribute("error", "Cuenta desactivada");
                    model.addAttribute("intentosRestantes", securityService.getRemainingAttempts(clientIP));
                    
                    System.out.println("⚠️ Intento de login con cuenta desactivada: " + user_name + " desde IP: " + clientIP);
                    return "login";
                }
                
                // 🔒 PASO 4: LOGIN EXITOSO - LIMPIAR INTENTOS FALLIDOS
                securityService.recordSuccessfulLogin(clientIP);
                System.out.println("✅ Login exitoso para: " + user_name + " desde IP: " + clientIP + 
                                 " - Intentos fallidos eliminados");
                
                return procesarLoginExitoso(usuario, model, session, user_name, clientIP);
                
            } else {
                // 🔒 PASO 5: LOGIN FALLIDO - REGISTRAR INTENTO
                securityService.recordFailedAttempt(clientIP);
                
                int intentosRestantes = securityService.getRemainingAttempts(clientIP);
                
                System.out.println("❌ Login fallido para: " + user_name + " desde IP: " + clientIP + 
                                 ". Intentos restantes: " + intentosRestantes);
                
                if (intentosRestantes > 0) {
                    model.addAttribute("error", String.format("Credenciales incorrectas. Te quedan %d intentos", intentosRestantes));
                    model.addAttribute("intentosRestantes", intentosRestantes);
                    model.addAttribute("showWarning", true);
                    model.addAttribute("isBlocked", false);


                } else {
                    long tiempoBloqueo = 60L; // o el valor dinámico que uses
                    model.addAttribute("error", String.format(
                    "🚫 Seguridad activada: tu IP ha sido bloqueada por %d minutos debido a múltiples intentos fallidos.", 
                    tiempoBloqueo));
                    model.addAttribute("isBlocked", true);
                    model.addAttribute("tiempoRestante", tiempoBloqueo);
                }

                
                return "login";
            }
            
        } catch (Exception e) {
            // 🔒 ERROR EN AUTENTICACIÓN - REGISTRAR COMO INTENTO FALLIDO
            securityService.recordFailedAttempt(clientIP);
            
            System.out.println("❌ Error en autenticación para " + user_name + " desde IP " + clientIP + ": " + e.getMessage());
            e.printStackTrace();
            
            model.addAttribute("error", "Error en el proceso de autenticación");
            model.addAttribute("intentosRestantes", securityService.getRemainingAttempts(clientIP));

            model.addAttribute("isBlocked", false);
            model.addAttribute("showWarning", true);
            model.addAttribute("intentosRestantes", securityService.getRemainingAttempts(clientIP));

            return "login";
        }
    }

    /**
     * 🔧 Método auxiliar para procesar login exitoso
     */
    private String procesarLoginExitoso(Usuario usuario, Model model, HttpSession session, 
                                       String user_name, String clientIP) {
        String token = autenticacionService.generarTokenParaUsuario(usuario);
        
        // 🔥 GUARDAR EN SESIÓN CON INFORMACIÓN DE SEGURIDAD
        session.setAttribute("userName", usuario.getUser_name());
        session.setAttribute("userToken", token);
        session.setAttribute("userId", usuario.getIdUsuario());
        session.setAttribute("clientIP", clientIP); // 🔒 Guardar IP para validación posterior
        session.setAttribute("loginTime", System.currentTimeMillis()); // 🔒 Timestamp del login
        
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
            System.out.println("👑 Login exitoso para ADMINISTRADOR: " + user_name + " desde IP: " + clientIP);
            return "redirect:/dashboard";
        } else {
            System.out.println("✅ Login exitoso para USUARIO: " + user_name + " desde IP: " + clientIP);
            return "redirect:/control";
        }
    }

    private Collection<? extends GrantedAuthority> mapearRoles(Usuario usuario) {
        if (usuario.getRol() != null) {
            return List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getRol().getNombre()));
        } else {
            return List.of(); // Sin rol asignado
        }
    }

    @PostMapping("/registro")
    public String procesarRegistro(@RequestParam String user_name,
                                   @RequestParam String clave,
                                   @RequestParam String confirmarClave,
                                   HttpServletRequest request,
                                   Model model,
                                   HttpSession session) {
        
        String clientIP = getClientIP(request);
        System.out.println("📝 Procesando registro para: " + user_name + " desde IP: " + clientIP);
        
        // 🔒 VERIFICAR SI LA IP ESTÁ BLOQUEADA (opcional para registro)
        if (securityService.isBlocked(clientIP)) {
            long tiempoRestante = securityService.getRemainingLockoutTime(clientIP);
            model.addAttribute("error", 
                String.format("IP temporalmente bloqueada. Intente en %d minutos", tiempoRestante));
            return "registro";
        }
        
        // Validaciones básicas existentes...
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
            session.setAttribute("clientIP", clientIP); // 🔒 Guardar IP
            session.setAttribute("loginTime", System.currentTimeMillis()); // 🔒 Timestamp
            
            model.addAttribute("usuario", nuevoUsuario);
            model.addAttribute("token", nuevoUsuario.getToken());
            model.addAttribute("mensaje", "¡Registro exitoso! Bienvenido " + user_name);
            
            System.out.println("✅ Registro exitoso para: " + user_name + " con ID: " + nuevoUsuario.getIdUsuario() + 
                             " desde IP: " + clientIP);
            
            // 🚀 Los usuarios recién registrados van al control ESP32
            return "redirect:/control";
            
        } catch (Exception e) {
            System.out.println("❌ Error en registro: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al registrar usuario: " + e.getMessage());
            return "registro";
        }
    }

    @GetMapping("/validar")
    public String validarToken(@RequestParam String token, 
                               HttpServletRequest request,
                               Model model, 
                               HttpSession session) {
        
        String clientIP = getClientIP(request);
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(token);
        if (usuarioOpt.isPresent()) {
            Usuario usuario = usuarioOpt.get();
            
            // Crear sesión si no existe
            session.setAttribute("userName", usuario.getUser_name());
            session.setAttribute("userToken", token);
            session.setAttribute("userId", usuario.getIdUsuario());
            session.setAttribute("clientIP", clientIP); // 🔒 Guardar IP
            session.setAttribute("loginTime", System.currentTimeMillis()); // 🔒 Timestamp
            
            model.addAttribute("mensaje", "Token válido. Bienvenido " + usuario.getUser_name());
            model.addAttribute("usuario", usuario);
            
            // Verificar rol y redirigir apropiadamente
            boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
            
            if (isAdmin) {
                System.out.println("👑 Token válido para administrador: " + usuario.getUser_name() + " desde IP: " + clientIP);
                return "redirect:/dashboard";
            } else {
                System.out.println("✅ Token válido para usuario: " + usuario.getUser_name() + " desde IP: " + clientIP);
                return "redirect:/control";
            }
        } else {
            model.addAttribute("error", "Token inválido o expirado");
            return "login";
        }
    }

    @PostMapping("/logout")
    public String cerrarSesion(@RequestParam String user_name, 
                               HttpServletRequest request,
                               Model model, 
                               HttpSession session) {
        
        String clientIP = getClientIP(request);
        System.out.println("🚪 Cerrando sesión para: " + user_name + " desde IP: " + clientIP);
        
        autenticacionService.cerrarSesion(user_name);
        session.invalidate(); // Limpiar sesión completa
        model.addAttribute("mensaje", "Sesión cerrada correctamente");
        return "login";
    }

    /**
     * 🔒 MÉTODO PARA OBTENER LA IP REAL DEL CLIENTE
     */
    private String getClientIP(HttpServletRequest request) {
        // Headers que pueden contener la IP real del cliente
        String[] headerNames = {
            "X-Forwarded-For",
            "X-Real-IP", 
            "Proxy-Client-IP",
            "WL-Proxy-Client-IP",
            "HTTP_X_FORWARDED_FOR",
            "HTTP_X_FORWARDED",
            "HTTP_X_CLUSTER_CLIENT_IP",
            "HTTP_CLIENT_IP",
            "HTTP_FORWARDED_FOR",
            "HTTP_FORWARDED"
        };
        
        for (String header : headerNames) {
            String ip = request.getHeader(header);
            if (ip != null && !ip.isEmpty() && !"unknown".equalsIgnoreCase(ip)) {
                // Si hay múltiples IPs, tomar la primera
                return ip.split(",")[0].trim();
            }
        }
        
        // Si no se encuentra en los headers, usar la IP del request
        return request.getRemoteAddr();
    }

    // ========== ENDPOINTS DE ADMINISTRACIÓN PARA SEGURIDAD ==========
    
    /**
     * 🔒 Endpoint para que los administradores vean el estado de seguridad
     */
    @GetMapping("/admin/security-status")
    @ResponseBody
    public Object getSecurityStatus(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return "❌ No autorizado - Sin sesión activa";
        }
        
        // Verificar si es admin
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido";
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            return "❌ Acceso denegado - Se requieren permisos de administrador";
        }
        
        return securityService.getSecurityStats();
    }
    
    /**
     * 🔒 Endpoint para desbloquear una IP manualmente (solo administradores)
     */
    @PostMapping("/admin/unblock-ip")
    @ResponseBody
    public String unblockIP(@RequestParam String ip, HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return "❌ No autorizado - Sin sesión activa";
        }
        
        // Verificar si es admin
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido";
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            return "❌ Acceso denegado - Se requieren permisos de administrador";
        }
        
        boolean success = securityService.unblockIP(ip);
        if (success) {
            System.out.println("🔓 Admin " + userName + " desbloqueó IP: " + ip);
            return "✅ IP " + ip + " desbloqueada exitosamente";
        } else {
            return "⚠️ IP " + ip + " no estaba bloqueada";
        }
    }

    /**
     * 🔒 Endpoint para obtener información de una IP específica
     */
    @GetMapping("/admin/ip-status")
    @ResponseBody
    public String getIPStatus(@RequestParam String ip, HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return "❌ No autorizado";
        }
        
        // Verificar si es admin
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido";
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            return "❌ Acceso denegado";
        }
        
        return securityService.getIPStatus(ip);
    }

    // ========== MÉTODOS EXISTENTES ACTUALIZADOS ==========

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
                if (isAdmin) {
                    return "redirect:/dashboard";
                } else {
                    return "redirect:/control";
                }
        }
    }

    @GetMapping("/session-info")
    @ResponseBody
    public String sessionInfo(HttpSession session, HttpServletRequest request) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        Long userId = (Long) session.getAttribute("userId");
        String sessionIP = (String) session.getAttribute("clientIP");
        Long loginTime = (Long) session.getAttribute("loginTime");
        String currentIP = getClientIP(request);
        
        if (userName == null) {
            return "❌ No hay sesión activa";
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return "❌ Token inválido para usuario: " + userName;
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        // 🔒 INFORMACIÓN DE SEGURIDAD
        String securityInfo = securityService.getIPStatus(currentIP);
        String ipMatch = sessionIP != null && sessionIP.equals(currentIP) ? "✅ COINCIDE" : "⚠️ DIFERENTE";
        String sessionDuration = loginTime != null ? 
            String.valueOf((System.currentTimeMillis() - loginTime) / (60 * 1000)) + " minutos" : "Desconocido";
        
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
            📊 INFORMACIÓN DE SESIÓN CON SEGURIDAD:
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            👤 Usuario: %s
            🆔 ID: %d
            🎭 Rol: %s
            👑 Es Admin: %s
            🔑 Token: %s...
            🔐 Contraseña: %s
            📅 Fecha Registro: %s
            🔄 Última Modificación: %s
            📊 Estado: %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
            🔒 INFORMACIÓN DE SEGURIDAD:
            🌐 IP Sesión: %s
            🌐 IP Actual: %s
            🔍 Verificación IP: %s
            ⏱️ Duración Sesión: %s
            🛡️ Estado Seguridad: %s
            ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
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
            sessionIP != null ? sessionIP : "No registrada",
            currentIP,
            ipMatch,
            sessionDuration,
            securityInfo,
            session.getId()
        );
    }
    
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