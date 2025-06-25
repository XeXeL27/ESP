package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.servicio.AutenticacionService;
import com.example.demo.models.servicio.ESP32Service;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/control")
public class ESP32ControlController {

    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private ESP32Service esp32Service;

    /**
     * Mostrar la vista de control del ESP32
     */
    @GetMapping("")
    public String mostrarControl(Model model, HttpSession session) {
        // Verificar si hay un usuario en sesión
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            System.out.println("❌ No hay sesión activa, redirigiendo a login");
            return "redirect:/login";
        }
        
        // Validar token
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Token inválido, redirigiendo a login");
            session.invalidate();
            return "redirect:/login";
        }
        
        Usuario usuario = usuarioOpt.get();
        System.out.println("✅ Usuario válido accediendo al control: " + usuario.getUser_name());
        
        // Agregar datos al modelo
        model.addAttribute("usuario", usuario);
        model.addAttribute("token", userToken);
        
        return "esp32-control";
    }

    /**
     * API para obtener datos del usuario actual (AJAX)
     */
    @GetMapping("/api/user/current")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerUsuarioActual(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return ResponseEntity.status(401).build();
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            return ResponseEntity.status(401).build();
        }
        
        Usuario usuario = usuarioOpt.get();
        Map<String, Object> userData = new HashMap<>();
        userData.put("username", usuario.getUser_name());
        userData.put("estado", usuario.getEstado());
        userData.put("fechaRegistro", usuario.getFechaRegistro());
        userData.put("rol", usuario.getRol() != null ? usuario.getRol().getNombre() : "USUARIO");
        
        // Agregar información de persona si existe
        if (usuario.getPersona() != null) {
            userData.put("nombreCompleto", 
                usuario.getPersona().getNombre() + " " + 
                usuario.getPersona().getPaterno());
            userData.put("ci", usuario.getPersona().getCi());
        }
        
        return ResponseEntity.ok(userData);
    }

    /**
     * API para obtener el estado completo del sistema
     */
    @GetMapping("/api/system/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> obtenerEstadoSistema(HttpSession session) {
        // Verificar autenticación
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }
        
        Map<String, Object> systemStatus = new HashMap<>();
        
        try {
            // Estado del ESP32
            String esp32Status = esp32Service.obtenerEstadoConexion();
            systemStatus.put("esp32Connected", esp32Status.contains("conectado"));
            systemStatus.put("esp32Status", esp32Status);
            
            // Estado de la red
            systemStatus.put("networkStatus", "WiFi Activo");
            systemStatus.put("serverTime", java.time.LocalDateTime.now().toString());
            
            // Estado simulado de la puerta (podrías expandir esto)
            systemStatus.put("doorStatus", "CERRADA");
            systemStatus.put("lastAction", "--:--");
            
            return ResponseEntity.ok(systemStatus);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo estado del sistema: " + e.getMessage());
            systemStatus.put("esp32Connected", false);
            systemStatus.put("esp32Status", "Error de conexión");
            systemStatus.put("networkStatus", "Error");
            return ResponseEntity.ok(systemStatus);
        }
    }

    /**
     * Cerrar sesión desde el panel de control
     */
    @PostMapping("/logout")
    public String cerrarSesionControl(HttpSession session, Model model) {
        String userName = (String) session.getAttribute("userName");
        
        if (userName != null) {
            System.out.println("🚪 Cerrando sesión desde panel de control: " + userName);
            autenticacionService.cerrarSesion(userName);
        }
        
        session.invalidate();
        model.addAttribute("mensaje", "Sesión cerrada correctamente desde el panel de control");
        return "redirect:/login";
    }

    /**
     * Método auxiliar para verificar autenticación
     */
    private boolean isAuthenticated(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            return false;
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        return usuarioOpt.isPresent();
    }
}