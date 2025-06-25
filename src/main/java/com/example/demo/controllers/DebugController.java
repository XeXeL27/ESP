package com.example.demo.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import com.example.demo.models.dao.UsuarioDao;
import com.example.demo.models.dao.RolDao;
import com.example.demo.models.entity.Usuario;
import com.example.demo.models.entity.Rol;
import java.util.List;

@RestController
@RequestMapping("/api/debug")
public class DebugController {
    
    @Autowired
    private UsuarioDao usuarioDao;
    
    @Autowired
    private RolDao rolDao;
    
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
}