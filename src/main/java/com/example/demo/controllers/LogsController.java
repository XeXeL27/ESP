package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.entity.LogAcceso;
import com.example.demo.models.servicio.AutenticacionService;
import com.example.demo.models.dao.LogAccesoDao;
import com.example.demo.models.dao.UsuarioDao;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/logs")
public class LogsController {

    @Autowired
    private AutenticacionService autenticacionService;
    
    @Autowired
    private LogAccesoDao logAccesoDao;
    
    @Autowired
    private UsuarioDao usuarioDao;

    /**
     * Verificar si el usuario actual es administrador autenticado
     */
    private boolean isAdminAuthenticated(HttpSession session) {
        String userName = (String) session.getAttribute("userName");
        String userToken = (String) session.getAttribute("userToken");
        
        if (userName == null || userToken == null) {
            System.out.println("❌ Acceso a logs denegado - Sin sesión válida");
            return false;
        }
        
        Optional<Usuario> usuarioOpt = autenticacionService.validarToken(userToken);
        if (usuarioOpt.isEmpty()) {
            System.out.println("❌ Acceso a logs denegado - Token inválido para: " + userName);
            return false;
        }
        
        Usuario usuario = usuarioOpt.get();
        boolean isAdmin = usuario.getRol() != null && "ADMIN".equals(usuario.getRol().getNombre());
        
        if (!isAdmin) {
            System.out.println("❌ Acceso a logs denegado - Usuario " + userName + " no es administrador");
        }
        
        return isAdmin;
    }

    /**
     * Mostrar vista principal de logs cyberpunk
     */
    @GetMapping("")
    public String mostrarLogs(Model model, HttpSession session,
                             @RequestParam(required = false) String filtro,
                             @RequestParam(required = false) String usuario,
                             @RequestParam(required = false) String fecha) {
        
        // Verificar autenticación y permisos de administrador
        if (!isAdminAuthenticated(session)) {
            return "redirect:/login";
        }
        
        String adminUser = (String) session.getAttribute("userName");
        Optional<Usuario> adminUsuarioOpt = autenticacionService.validarToken((String) session.getAttribute("userToken"));
        
        if (adminUsuarioOpt.isEmpty()) {
            return "redirect:/login";
        }
        
        Usuario adminUsuario = adminUsuarioOpt.get();
        model.addAttribute("usuario", adminUsuario);
        
        try {
            System.out.println("🔍 Admin " + adminUser + " accediendo a logs del sistema");
            
            // Obtener logs según filtros
            List<LogAcceso> logs;
            String tituloFiltro = "Todos los Logs";
            
            if (filtro != null && !filtro.trim().isEmpty()) {
                switch (filtro.toLowerCase()) {
                    case "aperturas":
                        logs = logAccesoDao.findLogsAperturaPuerta();
                        tituloFiltro = "🚪 Logs de Apertura de Puerta";
                        break;
                    case "exitosos":
                        logs = logAccesoDao.findByResultadoOrderByFechaHoraDesc("EXITOSO");
                        tituloFiltro = "✅ Logs Exitosos";
                        break;
                    case "errores":
                        logs = logAccesoDao.findByResultadoOrderByFechaHoraDesc("ERROR");
                        tituloFiltro = "❌ Logs de Errores";
                        break;
                    case "hoy":
                        LocalDateTime inicioHoy = LocalDateTime.now().toLocalDate().atStartOfDay();
                        LocalDateTime finHoy = inicioHoy.plusDays(1);
                        logs = logAccesoDao.findByFechaHoraBetweenOrderByFechaHoraDesc(inicioHoy, finHoy);
                        tituloFiltro = "📅 Logs de Hoy";
                        break;
                    case "semana":
                        LocalDateTime unaSemanaAtras = LocalDateTime.now().minusDays(7);
                        logs = logAccesoDao.findLogsUltimaSemana(unaSemanaAtras);
                        tituloFiltro = "📊 Logs de la Última Semana";
                        break;
                    default:
                        logs = logAccesoDao.findAllOrderByFechaHoraDesc();
                        break;
                }
            } else if (usuario != null && !usuario.trim().isEmpty()) {
                logs = logAccesoDao.findByUsuarioUserName(usuario);
                tituloFiltro = "👤 Logs de Usuario: " + usuario;
            } else {
                // Por defecto, mostrar los últimos 100 logs
                logs = logAccesoDao.findTopNByOrderByFechaHoraDesc(PageRequest.of(0, 100));
            }
            
            // Obtener estadísticas generales
            Object[] estadisticasGenerales = logAccesoDao.getEstadisticasGenerales();
            Object[] estadisticasHoy = logAccesoDao.getEstadisticasHoy();
            
            if (estadisticasGenerales != null && estadisticasGenerales.length >= 6) {
                model.addAttribute("totalLogs", ((Number) estadisticasGenerales[0]).longValue());
                model.addAttribute("logsExitosos", ((Number) estadisticasGenerales[1]).longValue());
                model.addAttribute("logsErrores", ((Number) estadisticasGenerales[2]).longValue());
                model.addAttribute("logsFallidos", ((Number) estadisticasGenerales[3]).longValue());
                model.addAttribute("totalAperturas", ((Number) estadisticasGenerales[4]).longValue());
                model.addAttribute("totalLogins", ((Number) estadisticasGenerales[5]).longValue());
            } else {
                // Valores por defecto si no hay estadísticas
                model.addAttribute("totalLogs", 0L);
                model.addAttribute("logsExitosos", 0L);
                model.addAttribute("logsErrores", 0L);
                model.addAttribute("logsFallidos", 0L);
                model.addAttribute("totalAperturas", 0L);
                model.addAttribute("totalLogins", 0L);
            }
            
            // Procesar estadísticas de hoy
            if (estadisticasHoy != null && estadisticasHoy.length >= 4) {
                model.addAttribute("logsHoy", ((Number) estadisticasHoy[0]).longValue());
                model.addAttribute("aperturasHoy", ((Number) estadisticasHoy[2]).longValue());
                model.addAttribute("usuariosActivosHoy", ((Number) estadisticasHoy[3]).longValue());
                model.addAttribute("erroresHoy", 0L); // Calcularemos esto después si es necesario
            } else {
                model.addAttribute("logsHoy", 0L);
                model.addAttribute("aperturasHoy", 0L);
                model.addAttribute("usuariosActivosHoy", 0L);
                model.addAttribute("erroresHoy", 0L);
            }
            
            // Obtener usuarios más activos
            List<Object[]> usuariosMasActivos = logAccesoDao.getUsuariosMasActivos();
            model.addAttribute("usuariosMasActivos", usuariosMasActivos);
            
            // Obtener lista de todos los usuarios para el filtro
            List<Usuario> todosUsuarios = usuarioDao.findAll();
            model.addAttribute("todosUsuarios", todosUsuarios);
            
            // Agregar datos al modelo
            model.addAttribute("logs", logs);
            model.addAttribute("tituloFiltro", tituloFiltro);
            model.addAttribute("filtroActual", filtro);
            model.addAttribute("usuarioFiltro", usuario);
            model.addAttribute("totalLogsEncontrados", logs.size());
            model.addAttribute("fechaActual", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss")));
            
            System.out.println("📊 Logs cargados: " + logs.size() + " registros encontrados");
            System.out.println("🎯 Filtro aplicado: " + tituloFiltro);
            
            return "logs-cyberpunk";
            
        } catch (Exception e) {
            System.out.println("❌ Error cargando logs: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Error al cargar los logs del sistema: " + e.getMessage());
            return "logs-cyberpunk";
        }
    }

    /**
     * API para obtener logs en tiempo real (AJAX)
     */
    @GetMapping("/api/realtime")
    @ResponseBody
    public ResponseEntity<?> obtenerLogsRealTime(HttpSession session,
                                                @RequestParam(defaultValue = "20") int limit) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            List<LogAcceso> logs = logAccesoDao.findTopNByOrderByFechaHoraDesc(PageRequest.of(0, limit));
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("timestamp", LocalDateTime.now());
            response.put("count", logs.size());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo logs en tiempo real: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener logs en tiempo real");
        }
    }

    /**
     * API para obtener estadísticas actualizadas
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public ResponseEntity<?> obtenerEstadisticas(HttpSession session) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            Object[] estadisticasGenerales = logAccesoDao.getEstadisticasGenerales();
            Object[] estadisticasHoy = logAccesoDao.getEstadisticasHoy();
            
            Map<String, Object> stats = new HashMap<>();
            
            if (estadisticasGenerales != null && estadisticasGenerales.length >= 6) {
                stats.put("totalLogs", ((Number) estadisticasGenerales[0]).longValue());
                stats.put("logsExitosos", ((Number) estadisticasGenerales[1]).longValue());
                stats.put("logsErrores", ((Number) estadisticasGenerales[2]).longValue());
                stats.put("totalAperturas", ((Number) estadisticasGenerales[4]).longValue());
            }
            
            if (estadisticasHoy != null && estadisticasHoy.length >= 4) {
                stats.put("logsHoy", ((Number) estadisticasHoy[0]).longValue());
                stats.put("aperturasHoy", ((Number) estadisticasHoy[1]).longValue());
                stats.put("usuariosActivosHoy", ((Number) estadisticasHoy[2]).longValue());
                stats.put("erroresHoy", ((Number) estadisticasHoy[3]).longValue());
            }
            
            stats.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo estadísticas: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener estadísticas");
        }
    }

    /**
     * API para búsqueda de logs
     */
    @GetMapping("/api/search")
    @ResponseBody
    public ResponseEntity<?> buscarLogs(@RequestParam String query, HttpSession session) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            List<LogAcceso> logs = logAccesoDao.findBySearchTerm(query);
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("query", query);
            response.put("count", logs.size());
            response.put("timestamp", LocalDateTime.now());
            
            System.out.println("🔍 Búsqueda de logs: '" + query + "' - " + logs.size() + " resultados");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error en búsqueda de logs: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error en la búsqueda");
        }
    }

    /**
     * API para obtener actividad por horas (para gráficos)
     */
    @GetMapping("/api/activity-chart")
    @ResponseBody
    public ResponseEntity<?> obtenerActividadPorHoras(HttpSession session) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            List<Object[]> actividadPorHoras = logAccesoDao.getActividadPorHoras();
            
            Map<String, Object> chartData = new HashMap<>();
            chartData.put("actividadPorHoras", actividadPorHoras);
            chartData.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(chartData);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo datos de actividad: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener datos de actividad");
        }
    }

    /**
     * API para obtener logs de errores recientes
        */
    @GetMapping("/api/recent-errors")
    @ResponseBody
    public ResponseEntity<?> obtenerErroresRecientes(HttpSession session) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            LocalDateTime hace24Horas = LocalDateTime.now().minusDays(1);
            List<LogAcceso> erroresRecientes = logAccesoDao.getErroresRecientes(hace24Horas);
            
            Map<String, Object> response = new HashMap<>();
            response.put("errores", erroresRecientes);
            response.put("count", erroresRecientes.size());
            response.put("timestamp", LocalDateTime.now());
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error obteniendo errores recientes: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al obtener errores recientes");
        }
    }

    /**
     * API para limpiar logs antiguos (mantenimiento)
     */
    @PostMapping("/api/cleanup")
    @ResponseBody
    public ResponseEntity<?> limpiarLogsAntiguos(@RequestParam(defaultValue = "30") int diasAtras,
                                               HttpSession session) {
        
        if (!isAdminAuthenticated(session)) {
            return ResponseEntity.status(403).body("❌ Acceso denegado");
        }
        
        try {
            String adminUser = (String) session.getAttribute("userName");
            LocalDateTime fechaLimite = LocalDateTime.now().minusDays(diasAtras);
            
            // Contar logs a eliminar
            List<LogAcceso> logsAEliminar = logAccesoDao.findByFechaHoraBetweenOrderByFechaHoraDesc(
                LocalDateTime.now().minusYears(10), fechaLimite
            );
            
            long cantidadEliminados = logsAEliminar.size();
            
            // Eliminar logs antiguos
            logAccesoDao.deleteLogsAnterioresA(fechaLimite);
            
            Map<String, Object> response = new HashMap<>();
            response.put("mensaje", "Logs antiguos eliminados exitosamente");
            response.put("cantidadEliminados", cantidadEliminados);
            response.put("fechaLimite", fechaLimite);
            response.put("ejecutadoPor", adminUser);
            response.put("timestamp", LocalDateTime.now());
            
            System.out.println("🧹 Admin " + adminUser + " eliminó " + cantidadEliminados + " logs anteriores a " + fechaLimite);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            System.out.println("❌ Error limpiando logs: " + e.getMessage());
            return ResponseEntity.internalServerError()
                .body("Error al limpiar logs antiguos");
        }
    }

    /**
     * Endpoint para redirigir desde el dashboard admin
     */
    @GetMapping("/dashboard")
    public String redirectFromDashboard() {
        return "redirect:/logs";
    }
}